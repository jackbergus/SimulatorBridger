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

package org.cloudbus.osmosis.core;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import com.google.common.collect.HashMultimap;
import org.cloudbus.agent.AgentBroker;
import org.cloudbus.agent.CentralAgent;
import org.cloudbus.cloudsim.*;
import org.cloudbus.cloudsim.core.MainEventManager;
import org.cloudbus.cloudsim.core.CloudSimTags;
import org.cloudbus.cloudsim.core.SimEvent;
import org.cloudbus.cloudsim.edge.core.edge.EdgeDevice;
import org.cloudbus.cloudsim.edge.core.edge.EdgeLet;
import uk.ncl.giacomobergami.components.iot.IoTDevice;
import uk.ncl.giacomobergami.components.iot.IoTEntityGenerator;
import uk.ncl.giacomobergami.components.loader.GlobalConfigurationSettings;
import uk.ncl.giacomobergami.components.mel_routing.MELSwitchPolicy;
import uk.ncl.giacomobergami.utils.asthmatic.WorkloadCSV;

import static org.cloudbus.cloudsim.core.CloudSimTags.MAPE_WAKEUP_FOR_COMMUNICATION;
import static org.cloudbus.osmosis.core.OsmoticTags.GENERATE_OSMESIS_WITH_RESOLUTION;

/**
 * 
 * @author Khaled Alwasel
 * @contact kalwasel@gmail.com
 * @since IoTSim-Osmosis 1.0
 * 
**/

public class OsmoticBroker extends DatacenterBroker {
//	public EdgeSDNController edgeController;
	public List<Cloudlet> edgeletList = new ArrayList<>();
	public List<OsmoticAppDescription> appList;
	public Map<String, Integer> iotDeviceNameToId = new HashMap<>();
	public Map<String, IoTDevice> iotDeviceNameToObject = new HashMap<>();
	public Map<Integer, List<? extends Vm>> mapVmsToDatacenter  = new HashMap<>();
	public static int brokerID;
	public Map<String, Integer> iotVmIdByName = new HashMap<>();
	public static List<WorkflowInfo> workflowTag = new ArrayList<>();
	public List<OsmoticDatacenter> datacenters = new ArrayList<>();
	private final AtomicInteger edgeLetId;
	public boolean isWakeupStartSet;

	//private Map<String, Integer> roundRobinMelMap = new HashMap<>();

	public CentralAgent osmoticCentralAgent;
	private AtomicInteger flowId;
	private IoTEntityGenerator ioTEntityGenerator;
	private double deltaVehUpdate;

	public OsmoticBroker(String name,
						 AtomicInteger edgeLetId,
						 AtomicInteger flowId) {
		super(name);
		this.edgeLetId = edgeLetId;
		this.flowId = flowId;
		this.appList = new ArrayList<>();		
		brokerID = this.getId();
		isWakeupStartSet = false;
	}

	@Override
	public void startEntity() {
		super.startEntity();
	}

	@Override
	public void processEvent(SimEvent ev) {
		double chron = MainEventManager.clock();
		var ab = AgentBroker.getInstance();

		if (!isWakeupStartSet) {
			for (Double forcedWakeUpTime :
					ioTEntityGenerator.collectionOfWakeUpTimes()) {
				double time = forcedWakeUpTime - chron;
				if (time > 0.0) {
					schedule(OsmoticBroker.brokerID, time, MAPE_WAKEUP_FOR_COMMUNICATION, null);
				}
			}
			isWakeupStartSet = true;
		}

		if (ev.getTag() == MAPE_WAKEUP_FOR_COMMUNICATION) {
			System.out.println("WakeUp Call @"+chron);
		}

		// Updates the IoT Device with the geo-location information
		iotDeviceNameToObject.forEach((id, obj) -> {
			ioTEntityGenerator.updateIoTDevice(obj, chron, chron+deltaVehUpdate);
		});

		//Update simulation time in the AgentBroker
		ab.updateTime(chron);

		//Execute MAPE loop at time interval
		ab.executeMAPE(chron);



		switch (ev.getTag()) {
			case CloudSimTags.RESOURCE_CHARACTERISTICS_REQUEST:
				this.processResourceCharacteristicsRequest(ev);
				break;

			case CloudSimTags.RESOURCE_CHARACTERISTICS:
				this.processResourceCharacteristics(ev);
				break;

			case CloudSimTags.VM_CREATE_ACK:
				this.processVmCreate(ev);
				break;

			case GENERATE_OSMESIS_WITH_RESOLUTION: {
				// Registering an app that was determined dynamically
				OsmoticAppDescription app = (OsmoticAppDescription) ev.getData();
				int melId=-1;
				if (!melRouting.test(app.getMELName())){
					melId = getVmIdByName(app.getMELName());
				}
				app.setMelId(melId);
				int vmIdInCloud = this.getVmIdByName(app.getVmName());
				int edgeDatacenterId = this.getDatacenterIdByVmId(melId);
				app.setEdgeDcId(edgeDatacenterId);
				app.setEdgeDatacenterName(this.getDatacenterNameById(edgeDatacenterId));
				int cloudDatacenterId = this.getDatacenterIdByVmId(vmIdInCloud);
				app.setCloudDcId(cloudDatacenterId);
				app.setCloudDatacenterName(this.getDatacenterNameById(cloudDatacenterId));
				this.appList.add(app);
				// After this set up. then we can generate the osmesis!
			}
			case OsmoticTags.GENERATE_OSMESIS:
				generateIoTData(ev);
				break;

			case OsmoticTags.Transmission_ACK:
				askMelToProccessData(ev);
				break;

			case CloudSimTags.CLOUDLET_RETURN:
				processCloudletReturn(ev);
				break;

			case OsmoticTags.Transmission_SDWAN_ACK:
				askCloudVmToProccessData(ev);
				break;

			case CloudSimTags.END_OF_SIMULATION: // just printing
				this.shutdownEntity();
				break;

			case OsmoticTags.ROUTING_MEL_ID_RESOLUTION:
				this.melResolution(ev);

			default:
				break;
		}
	}

	MELSwitchPolicy melRouting;
	public MELSwitchPolicy getMelRouting() {
		return melRouting;
	}
	public void setMelRouting(MELSwitchPolicy melRouting) {
		this.melRouting = melRouting;
	}

	private void melResolution(SimEvent ev) {
		Flow flow = (Flow) ev.getData();
		String melName = flow.getAppNameDest();
		String IoTDevice = flow.getAppNameSrc();
		var actualIoT = iotDeviceNameToObject.get(IoTDevice);
		int mel_id = -1;

		if (melRouting.test(melName)){
			// Using a policy for determining the next MEL
			String melInstanceName = melRouting.apply(actualIoT, melName, this);
			if (melInstanceName == null) return;
			flow.setAppNameDest(melInstanceName);
			mel_id = getVmIdByName(melInstanceName); //name of VM

			//dynamic mapping to datacenter
			int edgeDatacenterId = this.getDatacenterIdByVmId(mel_id);
			flow.setDatacenterId(edgeDatacenterId);
			flow.setDatacenterName(this.getDatacenterNameById(edgeDatacenterId));
			flow.getWorkflowTag().setSourceDCName(this.getDatacenterNameById(edgeDatacenterId));
		} else {
			mel_id = getVmIdByName(melName); //name of VM

			//dynamic mapping to datacenter
			int edgeDatacenterId = this.getDatacenterIdByVmId(mel_id);
			flow.setDatacenterId(edgeDatacenterId);
			flow.setDatacenterName(this.getDatacenterNameById(edgeDatacenterId));
			flow.getWorkflowTag().setSourceDCName(this.getDatacenterNameById(edgeDatacenterId));
		}

		flow.setDestination(mel_id);
		sendNow(flow.getDatacenterId(), OsmoticTags.TRANSMIT_IOT_DATA, flow);
	}

	protected void processCloudletReturn(SimEvent ev)
	{
		Cloudlet cloudlet = (Cloudlet) ev.getData();						
		getCloudletReceivedList().add(cloudlet);
		EdgeLet edgeLet = (EdgeLet) ev.getData();	
		if(!edgeLet.getIsFinal()){	
			askMelToSendDataToCloud(ev);			
			return;
		}	
		edgeLet.getWorkflowTag().setFinishTime(MainEventManager.clock());
	}
	
	private void askMelToProccessData(SimEvent ev) {
		Flow flow = (Flow) ev.getData();		
		EdgeLet edgeLet = generateEdgeLet(flow.getOsmesisEdgeletSize());
		edgeLet.setVmId(flow.getDestination());
		edgeLet.setCloudletLength(flow.getOsmesisEdgeletSize());
		edgeLet.isFinal(false);
		edgeletList.add(edgeLet);
		int appId = flow.getOsmesisAppId();
		edgeLet.setOsmesisAppId(appId);
		edgeLet.setWorkflowTag(flow.getWorkflowTag());
		edgeLet.getWorkflowTag().setEdgeLet(edgeLet);		
		this.setCloudletSubmittedList(edgeletList);		
		sendNow(flow.getDatacenterId(), CloudSimTags.CLOUDLET_SUBMIT, edgeLet);
	}
	
	private EdgeLet generateEdgeLet(long length) {				
		long fileSize = 30;
		long outputSize = 1;		
		EdgeLet edgeLet = new EdgeLet(edgeLetId.getAndIncrement(), length, 1, fileSize, outputSize, new UtilizationModelFull(), new UtilizationModelFull(),
				new UtilizationModelFull());			
		edgeLet.setUserId(this.getId());
//		LegacyTopologyBuilder.edgeLetId++;
		return edgeLet;
	}	

	protected void askCloudVmToProccessData(SimEvent ev) {
		Flow flow = (Flow) ev.getData();		
		int appId = flow.getOsmesisAppId();		
		int dest = flow.getDestination();				
		OsmoticAppDescription app = getAppById(appId);
		long length = app.getOsmesisCloudletSize();		
		EdgeLet cloudLet =	generateEdgeLet(length);							
		cloudLet.setVmId(dest);
		cloudLet.isFinal(true);			
		edgeletList.add(cloudLet);		
		cloudLet.setOsmesisAppId(appId);
		cloudLet.setWorkflowTag(flow.getWorkflowTag());
		cloudLet.getWorkflowTag().setCloudLet(cloudLet);		
		this.setCloudletSubmittedList(edgeletList);		
		cloudLet.setUserId(OsmoticBroker.brokerID);
		this.setCloudletSubmittedList(edgeletList);
		int dcId = getDatacenterIdByVmId(dest);
		sendNow(dcId, CloudSimTags.CLOUDLET_SUBMIT, cloudLet);
	}

	private void askMelToSendDataToCloud(SimEvent ev) {
		EdgeLet edgeLet = (EdgeLet) ev.getData();		
		int osmesisAppId = edgeLet.getOsmesisAppId();		
		OsmoticAppDescription app = getAppById(osmesisAppId);
		int sourceId = edgeLet.getVmId(); // MEL or VM  			
		int destId = this.getVmIdByName(app.getVmName()); // MEL or VM
		int id = flowId.getAndIncrement();
		int melDataceneter = this.getDatacenterIdByVmId(sourceId);		
		Flow flow  = new Flow(app.getMELName(), app.getVmName(), sourceId , destId, id, null);									
		flow.setAppName(app.getAppName());
		flow.addPacketSize(app.getMELOutputSize());
		flow.setSubmitTime(MainEventManager.clock());
		flow.setOsmesisAppId(osmesisAppId);				
		flow.setWorkflowTag(edgeLet.getWorkflowTag());
		flow.getWorkflowTag().setEdgeToCloudFlow(flow);		
//		LegacyTopologyBuilder.flowId++;
		sendNow(melDataceneter, OsmoticTags.BUILD_ROUTE, flow);
	}	

	private OsmoticAppDescription getAppById(int osmesisAppId) {
		OsmoticAppDescription osmesis = null;
		for(OsmoticAppDescription app : this.appList){
			if(app.getAppID() == osmesisAppId){
				osmesis = app;
			}
		}
		return osmesis;
	}

	HashMultimap<String, String> map = null;
	public Set<String> selectVMFromHostPredicate(String melId) {
		if (map == null) {
			map = HashMultimap.create();
		}
		for (var cp : mapVmsToDatacenter.entrySet()) {
			for (var vmOrMel : cp.getValue()) {
				var host = vmOrMel.getHost();
				if (host instanceof EdgeDevice) {
					String toStartRegex = vmOrMel.getVmName();
					toStartRegex = toStartRegex.substring(0, toStartRegex.lastIndexOf('.'))+".*";
					map.put(toStartRegex, ((EdgeDevice) host).getDeviceName());
				}
			}
		}
		return map.get(melId);
	}
	public Collection<String> selectVMFromHostPredicate() {
		if (map == null) {
			map = HashMultimap.create();
		}
		for (var cp : mapVmsToDatacenter.entrySet()) {
			for (var vmOrMel : cp.getValue()) {
				var host = vmOrMel.getHost();
				if (host instanceof EdgeDevice) {
					String toStartRegex = vmOrMel.getVmName();
					toStartRegex = toStartRegex.substring(0, toStartRegex.lastIndexOf('.'))+".*";
					map.put(toStartRegex, ((EdgeDevice) host).getDeviceName());
				}
			}
		}
		return map.values();
	}

	public EdgeDevice resolveEdgeDeviceFromId(String hostId) {
		for (var cp : mapVmsToDatacenter.entrySet()) {
			for (var vmOrMel : cp.getValue()) {
				var host = vmOrMel.getHost();
				if (host instanceof EdgeDevice) {
					if (((EdgeDevice)host).getDeviceName().equals(hostId))
						return (EdgeDevice) host;
				}
			}
		}
		return null;
	}

	public void submitVmList(List<? extends Vm> list, int datacenterId) {		
		mapVmsToDatacenter.put(datacenterId, list);
		getVmList().addAll(list);
	}
	
	protected void createVmsInDatacenter(int datacenterId) {		
		int requestedVms = 0;
		List<? extends Vm> vmList = mapVmsToDatacenter.get(datacenterId);
		if(vmList != null){
			for (int i = 0; i < vmList.size(); i++) {
				Vm vm = vmList.get(i);		
					sendNow(datacenterId, CloudSimTags.VM_CREATE_ACK, vm);				
					requestedVms++;
			}
		}
		getDatacenterRequestedIdsList().add(datacenterId);
		setVmsRequested(requestedVms);
		setVmsAcks(0);
	}

	@Override
	protected void processOtherEvent(SimEvent ev) {

	}
	
	@Override
	public void processVmCreate(SimEvent ev) {
		super.processVmCreate(ev);
		if (allRequestedVmsCreated()) {		
			for(OsmoticAppDescription app : this.appList){				
				int iotDeviceID = getiotDeviceIdByName(app.getIoTDeviceName());

				//This is necessary for osmotic flow abstract routing.
				int melId=-1;
				if (!melRouting.test(app.getMELName())){
					melId = getVmIdByName(app.getMELName());
				}
				app.setMelId(melId);
				int vmIdInCloud = this.getVmIdByName(app.getVmName());
				app.setIoTDeviceId(iotDeviceID);
				int edgeDatacenterId = this.getDatacenterIdByVmId(melId);		
				app.setEdgeDcId(edgeDatacenterId);
				app.setEdgeDatacenterName(this.getDatacenterNameById(edgeDatacenterId));				
				int cloudDatacenterId = this.getDatacenterIdByVmId(vmIdInCloud);				
				app.setCloudDcId(cloudDatacenterId);
				app.setCloudDatacenterName(this.getDatacenterNameById(cloudDatacenterId));				
				if(app.getAppStartTime() == -1){
					app.setAppStartTime(MainEventManager.clock());
				}
				double delay = app.getDataRate()+app.getStartDataGenerationTime();
				send(this.getId(), delay, OsmoticTags.GENERATE_OSMESIS, app);
			}
		}
	}	

	private void generateIoTData(SimEvent ev){
		OsmoticAppDescription app = (OsmoticAppDescription) ev.getData();
		if((MainEventManager.clock() >= app.getStartDataGenerationTime()) &&
				(MainEventManager.clock() < app.getStopDataGenerationTime()) &&
				!app.getIsIoTDeviceDied()){
			sendNow(app.getIoTDeviceId(), OsmoticTags.SENSING, app);
			send(this.getId(), app.getDataRate(), OsmoticTags.GENERATE_OSMESIS, app);
		}
	}
			
	private boolean allRequestedVmsCreated() {
		return this.getVmsCreatedList().size() == getVmList().size() - getVmsDestroyed();
	}

	public void submitOsmesisApps(List<OsmoticAppDescription> appList) {
		this.appList = appList;		
	}

	public List<OsmoticAppDescription> submitWorkloadCSVApps(List<WorkloadCSV> appList) {
		this.appList = appList.stream().map(GlobalConfigurationSettings::asLegacyApp).collect(Collectors.toList());
		return this.appList;
	}

	public int getiotDeviceIdByName(String melName){
		return this.iotDeviceNameToId.get(melName);
	}
	public IoTDevice getiotDeviceByName(String melName){
		return this.iotDeviceNameToObject.get(melName);
	}

	public void mapVmNameToId(Map<String, Integer> melNameToIdList) {
		this.iotVmIdByName.putAll(melNameToIdList);
	}
	
	public int getVmIdByName(String name){
		return this.iotVmIdByName.get(name);
	}

	public void setDatacenters(List<OsmoticDatacenter> osmesisDatacentres) {
		this.datacenters = osmesisDatacentres;		
	}
	
	private int getDatacenterIdByVmId(int vmId){
		int dcId = 0;
		for(OsmoticDatacenter dc :datacenters){
			for(Vm vm : dc.getVmList()){
				if(vm.getId() == vmId){
					dcId = dc.getId();					
				}
			}
		}
		return dcId;
	}
	
	private String getDatacenterNameById(int id){
		String name = "";
		for(OsmoticDatacenter dc :datacenters){
			if(dc.getId() == id){
				name = dc.getName();
			}
		}
		return name;
	}

	public void addIoTDevice(IoTDevice device) {
		iotDeviceNameToId.put(device.getName(), device.getId());
		iotDeviceNameToObject.put(device.getName(), device);
	}

	public void setIoTTraces(IoTEntityGenerator ioTEntityGenerator) {
		this.ioTEntityGenerator = ioTEntityGenerator;
	}

	public void setDeltaVehUpdate(double deltaVehUpdate) {
		this.deltaVehUpdate = deltaVehUpdate;
	}

	public double getDeltaVehUpdate() {
		return deltaVehUpdate;
	}
}
