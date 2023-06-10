/*
 * Title:        IoTSim-Osmosis 1.0
 * Description:  IoTSim-Osmosis enables the testing and validation of osmotic computing applications 
 * 			     over heterogeneous edge-cloud SDN-aware environments.
 * 
 * Licence:      GPL - http://www.gnu.org/copyleft/gpl.html
 *
 * Copyright (c) 2020, Newcastle University (UK) and Saudi Electronic University (Saudi Arabia) 
 * 
 */


package org.cloudbus.cloudsim.edge.core.edge;

/**
 * 
 * @author Khaled Alwasel
 * @contact kalwasel@gmail.com
 * @since IoTSim-Osmosis 1.0
 * 
**/

import java.util.*;
import java.util.stream.Stream;

import org.cloudbus.agent.AgentBroker;
import org.cloudbus.cloudsim.DatacenterCharacteristics;
import org.cloudbus.cloudsim.Storage;
import org.cloudbus.cloudsim.Vm;
import uk.ncl.giacomobergami.components.allocation_policy.VmAllocationPolicy;
import org.cloudbus.cloudsim.core.MainEventManager;
import org.cloudbus.cloudsim.core.SimEvent;
import org.cloudbus.cloudsim.core.predicates.PredicateType;
import org.cloudbus.cloudsim.edge.core.edge.LegacyConfiguration.HostEntity;
import org.cloudbus.cloudsim.edge.core.edge.LegacyConfiguration.LinkEntity;
import org.cloudbus.cloudsim.edge.core.edge.LegacyConfiguration.SwitchEntity;
import org.cloudbus.osmosis.core.*;
import org.cloudbus.cloudsim.sdn.SDNHost;
import uk.ncl.giacomobergami.components.allocation_policy.VmAllocationPolicyGeneratorFactory;


public class EdgeDataCenter extends OsmoticDatacenter {
	
	private List<Flow> flowList = new ArrayList<>(); 
	private List<Flow> flowListHis = new ArrayList<>();

	public EdgeDataCenter(String name,
						  DatacenterCharacteristics characteristics,
						  VmAllocationPolicy vmAllocationPolicy,
						  List<Storage> storageList,
						  double schedulingInterval)  {
		super(name, characteristics, vmAllocationPolicy, storageList, schedulingInterval);

		//Osmosis Agents
		AgentBroker.getInstance().createDCAgent(name, this);
	}

	public EdgeDataCenter(LegacyConfiguration.EdgeDataCenterEntity edgeDCEntity,
						  DatacenterCharacteristics characteristics,
						  LinkedList<Storage> storageList,
						  double schedulingInterval) {
		this(edgeDCEntity.getName(),
				characteristics,
				VmAllocationPolicyGeneratorFactory.generateFacade(edgeDCEntity.getVmAllocationPolicy().getClassName()),
				storageList,
				schedulingInterval
				);
		setDcType(edgeDCEntity.getType());
	}

	public EdgeDataCenter(LegacyConfiguration.EdgeDataCenterEntity edgeDCEntity,
						  List<EdgeDevice> hostList,
						  LinkedList<Storage> storageList,
						  double schedulingInterval) {
		this(edgeDCEntity, new DatacenterCharacteristics(hostList, edgeDCEntity.getCharacteristics()), storageList, schedulingInterval);
		setSdnController(new EdgeSDNController(edgeDCEntity.getControllers().get(0), this));
		initEdgeTopology(hostList, edgeDCEntity.getSwitches(),edgeDCEntity.getLinks());
		getSdnController().setTopology(topology, hosts, sdnhosts, switches);
		setGateway(getSdnController().getGateway());
	}

	@Override
	public void processEvent(SimEvent ev) {
		// TODO Auto-generated method stub

		super.processEvent(ev);

	}

	@Override
	public void processOtherEvent(SimEvent ev) {
		int tag = ev.getTag();
		switch (tag) {
			
		case OsmoticTags.TRANSMIT_IOT_DATA:
			this.transferIoTData(ev);
			break;
			
		case OsmoticTags.INTERNAL_EVENT:
			updateFlowTransmission();
			break;			
			
		case OsmoticTags.BUILD_ROUTE:
			sendMelDataToClouds(ev);
			break;
			
		default:			
			System.out.println("Unknown event recevied by SDNDatacenter. Tag:"+ev.getTag());		
			break;
		}
	}	

	private void sendMelDataToClouds(SimEvent ev) {
		Flow flow  = (Flow) ev.getData();
		sendNow(this.getSdnController().getId(), OsmoticTags.BUILD_ROUTE, flow);
	}

	public void updateFlowTransmission() {		
		LinkedList<Flow> finshedFlows = new LinkedList<>();
		for(Flow flow : this.flowList){
			boolean isCompleted = flow.updateTransmission();						
			if(isCompleted){
				finshedFlows.add(flow);
			}			
		}
		
		if(finshedFlows.size() != 0){
			this.flowList.removeAll(finshedFlows);
			for(Vm vm : this.getVmList()){
				MEL mel = (MEL) vm;				
				this.removeFlows(mel, finshedFlows);				
				mel.updateAssociatedIoTDevices(); // update MEL Bw					
			}
			for(Flow flow : finshedFlows){
				// update IoT device Bw
				sendNow(flow.getOrigin(), OsmoticTags.updateIoTBW, flow); // tell IoT device to update its bandwidth by removing this finished flow
				sendNow(OsmoticBroker.brokerID, OsmoticTags.Transmission_ACK, flow);
			}
			
			updateAllFlowsBw();
		}		
		
		determineEarliestFinishingFlow();
	}
	
	private void updateAllFlowsBw(){
		// update the destination Bw of every flow 
		
		for(Flow flow : this.flowList){
			for(Vm vm : this.getVmList()){
			MEL mel = (MEL) vm;															
				if(flow.getDestination()  == mel.getId() ){
					mel.updateAssociatedIoTDevices();
					double melCurrentBw = mel.getCurrentBw();
					flow.updateDestBw(melCurrentBw);
				}
			}
		}
		
		// update the main bw of every flow
		for(Flow getFlow : this.flowList){
			getFlow.updateBandwidth();
		}
	}
	
	private void removeFlows(MEL mel, LinkedList<Flow> finshedFlows) {
		LinkedList<Flow> removedList = new LinkedList<>();
		for (Flow flow : mel.getFlowList()){
			for (Flow removedFlow : finshedFlows){
				if(flow.getFlowId() == removedFlow.getFlowId()){
					removedList.add(flow);
				}
			}
		}
		mel.removeFlows(removedList);
		removedList.clear();
	}

	private void transferIoTData(SimEvent ev) {		
		updateFlowTransmission();

		Flow flow = (Flow) ev.getData();
		flow.setDatacenterName(this.getName());
		if(flow.getStartTime() == -1){
			flow.setStartTime(MainEventManager.clock());
		}

		this.flowList.add(flow);
		flowListHis.add(flow);

		Vm vm = null;
		for(Vm getVm : this.getVmList()){
			if(getVm.getId() == flow.getDestination()){
				vm = getVm;
				break;
			}
		}

		if(vm != null){
			MEL mel = (MEL) vm;
			mel.addFlow(flow);
		}

		updateAllFlowsBw();

		flow.setPreviousTime(MainEventManager.clock()); // This makes the computation to progress
		determineEarliestFinishingFlow();
	}

	private void determineEarliestFinishingFlow() {
		MainEventManager.cancelAll(getId(), new PredicateType(OsmoticTags.INTERNAL_EVENT));
		double eft = Double.MAX_VALUE;
		double finishingTime;
		
		if(flowList.size() != 0) {
			for(Flow flow : this.flowList){			
				finishingTime = flow.FinishingTime();
				if(finishingTime < eft){
					eft = finishingTime;
				}
			}			
			send(this.getId(), eft,  OsmoticTags.INTERNAL_EVENT);
		}
	}

	@Override
	public void initCloudTopology(List<HostEntity> hostEntites, List<SwitchEntity> switchEntites,
			List<LinkEntity> linkEntites) {
		// TODO Auto-generated method stub		
	}
	
	@Override
	public void initEdgeTopology(List<EdgeDevice> devices,
								 List<SwitchEntity> switchEntites,
								 List<LinkEntity> linkEntites){
		this.hosts.addAll(devices); 
		topology  = new Topology();		 
		sdnhosts = new ArrayList<>();
		switches= new ArrayList<>();
		Hashtable<String,Integer> nameIdTable = new Hashtable<>();
					    		    		    
		for(EdgeDevice device : devices){
			String hostName = device.getDeviceName();					
			SDNHost sdnHost = new SDNHost(device, hostName);
			nameIdTable.put(hostName, sdnHost.getAddress());											
			this.topology.addNode(sdnHost);		
			this.sdnhosts.add(sdnHost);			
		}

		switchEntites.forEach(x -> x.initializeSwitch(nameIdTable, topology, switches));
		linkEntites.forEach(x -> x.initializeLink(nameIdTable, topology));
	}

	public void initEdgeTopology(List<EdgeDevice> devices,
								 Stream<SwitchEntity> switchEntites,
								 Collection<LinkEntity> linkEntites){
		this.hosts.addAll(devices);
		topology  = new Topology();
		sdnhosts = new ArrayList<>();
		switches= new ArrayList<>();
		Hashtable<String,Integer> nameIdTable = new Hashtable<>();

		for(EdgeDevice device : devices){
			String hostName = device.getDeviceName();
			SDNHost sdnHost = new SDNHost(device, hostName);
			nameIdTable.put(hostName, sdnHost.getAddress());
			this.topology.addNode(sdnHost);
			this.sdnhosts.add(sdnHost);
		}

		switchEntites.forEach(x -> x.initializeSwitch(nameIdTable, topology, switches));
		linkEntites.forEach(x -> x.initializeLink(nameIdTable, topology));
		getSdnController().setTopology(topology, hosts, sdnhosts, switches);
		setGateway(getSdnController().getGateway());
	}

}