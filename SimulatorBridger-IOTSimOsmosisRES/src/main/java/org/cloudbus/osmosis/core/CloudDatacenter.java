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
import java.util.stream.Stream;

import org.cloudbus.cloudsim.DatacenterCharacteristics;
import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.Storage;
import org.cloudbus.cloudsim.Vm;
import uk.ncl.giacomobergami.components.allocation_policy.VmAllocationPolicy;
import org.cloudbus.cloudsim.core.MainEventManager;
import org.cloudbus.cloudsim.core.SimEvent;
import org.cloudbus.cloudsim.edge.core.edge.LegacyConfiguration;
import org.cloudbus.cloudsim.edge.core.edge.EdgeDevice;
import org.cloudbus.cloudsim.edge.core.edge.LegacyConfiguration.HostEntity;
import org.cloudbus.cloudsim.edge.core.edge.LegacyConfiguration.LinkEntity;
import org.cloudbus.cloudsim.edge.core.edge.LegacyConfiguration.SwitchEntity;
import org.cloudbus.cloudsim.sdn.SDNHost;

/**
 * 
 * @author Khaled Alwasel
 * @contact kalwasel@gmail.com
 * @since IoTSim-Osmosis 1.0
 * 
**/

public class CloudDatacenter extends OsmoticDatacenter {

	public CloudDatacenter(LegacyConfiguration.CloudDataCenterEntity dataCenterEntity,
						   DatacenterCharacteristics characteristics,
						   VmAllocationPolicy vmAllocationPolicy,
						   List<Storage> storageList,
						   double schedulingInterval,
						   SDNController sdnController,
						   AtomicInteger hostId) {
			super(dataCenterEntity.getName(), characteristics, vmAllocationPolicy, storageList, schedulingInterval);
			this.sdnController = sdnController;
			this.sdnController.setDatacenter(this);
			initCloudTopology(dataCenterEntity.getHosts(),
					dataCenterEntity.getSwitches(),
					dataCenterEntity.getLinks(),
					hostId);
			feedSDNWithTopology();
			setGateway(getSdnController().getGateway());
			setDcType(dataCenterEntity.getType());
		}

	public CloudDatacenter(String name,
						   DatacenterCharacteristics characteristics,
						   VmAllocationPolicy generateFacade,
						   LinkedList<Storage> storageList, double scheduling_interval, SDNController sdnController) {
		super(name, characteristics, generateFacade, storageList, scheduling_interval);
		this.sdnController = sdnController;
		this.sdnController.setDatacenter(this);
	}

	public void addVm(Vm vm){
			getVmList().add(vm);
			if (vm.isBeingInstantiated()) vm.setBeingInstantiated(false);
			vm.updateVmProcessing(MainEventManager.clock(), getVmAllocationPolicy().getHost(vm).getVmScheduler().getAllocatedMipsForVm(vm));
		}
		
		@Override
		public void processOtherEvent(SimEvent ev){
			switch(ev.getTag()){
				default: System.out.println("Unknown event recevied by SDNDatacenter. Tag:"+ev.getTag());
			}
		}
		
		public Map<String, Integer> getVmNameIdTable() {
			return this.sdnController.getVmNameIdTable();
		}
		public Map<String, Integer> getFlowNameIdTable() {
			return this.sdnController.getFlowNameIdTable();
		}

		@Override
		public String toString() {
			return this.getClass().getSimpleName() + "{" +
					"id=" + this.getId() +
					", name=" + this.getName() +
					'}';
		}


	public void initCloudTopology(Stream<HostEntity> hostEntites,
								  Stream<SwitchEntity> switchEntites,
								  Collection<LinkEntity> linkEntites,
								  AtomicInteger hostId) {
		topology  = new Topology();
		sdnhosts = new ArrayList<>();
		switches= new ArrayList<>();
		Hashtable<String,Integer> nameIdTable = new Hashtable<>();
		hostEntites.forEach(hostEntity -> {
			long pes =  hostEntity.getPes();
			long mips = hostEntity.getMips();
			int ram = hostEntity.getRam();
			long storage = hostEntity.getStorage();
			double bw = hostEntity.getBw();
			String hostName = hostEntity.getName();
			Host host = createHost(hostId.getAndIncrement(), ram, bw, storage, pes, mips);
			host.setDatacenter(this);
			SDNHost sdnHost = new SDNHost(host, hostName);
			nameIdTable.put(hostName, sdnHost.getAddress());
			this.topology.addNode(sdnHost);
			this.hosts.add(host);
			this.sdnhosts.add(sdnHost);
		});
		switchEntites.forEach(x -> x.initializeSwitch(nameIdTable, topology, switches));
		linkEntites.forEach(x -> x.initializeLink(nameIdTable, topology));
	}
	public void initCloudTopology(List<HostEntity> hostEntites,
								  List<SwitchEntity> switchEntites,
								  List<LinkEntity> linkEntites,
								  AtomicInteger hostId) {
		 topology  = new Topology();		 
		 sdnhosts = new ArrayList<>();
		 switches= new ArrayList<>();
		Hashtable<String,Integer> nameIdTable = new Hashtable<>();
					    		    		    
		for(HostEntity hostEntity : hostEntites){															
			long pes =  hostEntity.getPes();
			long mips = hostEntity.getMips();
			int ram = hostEntity.getRam();
			long storage = hostEntity.getStorage();					
			double bw = hostEntity.getBw();
			String hostName = hostEntity.getName();					
			Host host = createHost(hostId.getAndIncrement(), ram, bw, storage, pes, mips);
			host.setDatacenter(this);
			SDNHost sdnHost = new SDNHost(host, hostName);
			nameIdTable.put(hostName, sdnHost.getAddress());
			this.topology.addNode(sdnHost);
			this.hosts.add(host);
			this.sdnhosts.add(sdnHost);			
		}

		switchEntites.forEach(x -> x.initializeSwitch(nameIdTable, topology, switches));
		linkEntites.forEach(x -> x.initializeLink(nameIdTable, topology));
	}

	@Override
	public void initCloudTopology(List<HostEntity> hostEntites, List<SwitchEntity> switchEntites, List<LinkEntity> linkEntites) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void initEdgeTopology(List<EdgeDevice> devices, List<SwitchEntity> switchEntites,
			List<LinkEntity> linkEntites) {
	}


	public void setDCAndAddVMsToSDNHosts() {
		sdnController.setDatacenter(this);
		sdnController.addVmsToSDNhosts(getVmList());
	}
}
