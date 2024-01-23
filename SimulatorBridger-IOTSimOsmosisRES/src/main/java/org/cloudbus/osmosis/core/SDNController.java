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


import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

import org.cloudbus.cloudsim.Datacenter;
import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.core.MainEventManager;
import org.cloudbus.cloudsim.core.SimEvent;
import org.cloudbus.cloudsim.edge.core.edge.LegacyConfiguration.LinkEntity;
import org.cloudbus.cloudsim.edge.core.edge.LegacyConfiguration.SwitchEntity;
import org.cloudbus.cloudsim.sdn.Link;
import org.cloudbus.cloudsim.sdn.NetworkOperatingSystem;
import org.cloudbus.cloudsim.sdn.NetworkNIC;
import org.cloudbus.cloudsim.sdn.SDNHost;
import org.cloudbus.cloudsim.sdn.Switch;

import org.jooq.DSLContext;
import uk.ncl.giacomobergami.components.sdn_traffic.SDNTrafficSchedulingPolicy;
import uk.ncl.giacomobergami.components.sdn_routing.SDNRoutingPolicy;

/**
 * 
 * @author Khaled Alwasel
 * @contact kalwasel@gmail.com
 * @since IoTSim-Osmosis 1.0
 * 
**/

public class SDNController extends NetworkOperatingSystem {
	protected volatile SDNRoutingPolicy sdnRoutingPolicy;
	private volatile SDNTrafficSchedulingPolicy sdnSchedulingPolicy;
	
	private volatile OsmosisOrchestrator orchestrator;

	protected String datacenterName; 
	private volatile OsmoticBroker edgeDatacenterBroker;
	
	private volatile Datacenter datacenter;
	private volatile Switch gateway;
    private volatile SDNController wanController;
	
    protected String name;
    
	public SDNController(String name, SDNTrafficSchedulingPolicy sdnPolicy, SDNRoutingPolicy sdnRouting) {				
		super(name);
		this.sdnSchedulingPolicy = sdnPolicy;
		this.sdnRoutingPolicy = sdnRouting;
	}
	
	public void setWanOorchestrator(OsmosisOrchestrator orchestrator) {
		this.orchestrator = orchestrator;
	}
	
	@Override
	public void processEvent(SimEvent ev) {
		int tag = ev.getTag();
		Flow flow;
		switch(tag){
		
		case OsmoticTags.BUILD_ROUTE:
			 flow = (Flow) ev.getData();			
			scheduleFlow(flow);
			break;
					
		case OsmoticTags.BUILD_ROUTE_GREEN:
			@SuppressWarnings("unchecked")
			List<Object> list = (List<Object>) ev.getData();
			 startTransmittingGreenEnergy(list);
			break;
			
		default: System.out.println(this.getName() + ": Unknown event received by "+super.getName()+". Tag:"+ev.getTag());
		}
	}

	@Override
	public void processEvent(SimEvent ev, Connection conn, DSLContext context) {
		int tag = ev.getTag();
		Flow flow;
		switch(tag){

			case OsmoticTags.BUILD_ROUTE:
				flow = (Flow) ev.getData();
				scheduleFlow(flow);
				break;

			case OsmoticTags.BUILD_ROUTE_GREEN:
				@SuppressWarnings("unchecked")
				List<Object> list = (List<Object>) ev.getData();
				startTransmittingGreenEnergy(list);
				break;

			default: System.out.println(this.getName() + ": Unknown event received by "+super.getName()+". Tag:"+ev.getTag());
		}
	}
	
	protected void startTransmittingGreenEnergy(List<Object> list) {

	}

	private void scheduleFlow(Flow flow){				 					
		startTransmitting(flow);	
	}
	
	public void startTransmitting(Flow flow) {
		int srcVm = flow.getOrigin();
		int dstVm = flow.getDestination();

		// Either src or dst are null if they are coming from outside the local network
		NetworkNIC srchost = findSDNHost(srcVm);
		NetworkNIC dsthost = findSDNHost(dstVm);
		int flowId = flow.getFlowId();
		if (srchost == null)  {
			srchost = this.getGateway(); // packets coming from outside the datacenter			
		}
		logger.trace(srchost+"-->"+dsthost);
									
		if(srchost.equals(dsthost)) {
			logger.debug(MainEventManager.clock() + ": " + getName() + ": Source SDN Host is same as destination. No need for routing!");
			List<NetworkNIC> listNodes = new ArrayList<>();
			listNodes.add(srchost);			
			getSdnSchedulingPolicy().setAppFlowStartTime(flow, flow.getSubmitTime()); // no transmission
			return;
		} 

		List<NetworkNIC> route;
		route = sdnRoutingPolicy.getRoute(flow.getOrigin(), flow.getDestination());
		if(route == null){			
			buildSDNForwardingTableVmBased(srcVm, dstVm, flowId, flow);
			List<NetworkNIC> endToEndRoute = sdnRoutingPolicy.getRoute(flow.getOrigin(), flow.getDestination());
			if (route != null) {
				if (!endToEndRoute.equals(route))
					throw new RuntimeException("Unvalid assumption!");
			}
			route = endToEndRoute;
		}
		flow.setNodeOnRouteList(route);
		List<Link> links = sdnRoutingPolicy.getLinks(flow.getOrigin(), flow.getDestination());
		flow.setLinkList(links);

		if(findSDNHost(dstVm) == null){
			sendNow(this.getWanController().getId(),
					OsmoticTags.BUILD_ROUTE,
					flow);
		} else {
			sendNow(this.getWanOorchestrator().getId(),
					OsmoticTags.START_TRANSMISSION,
					flow);
		}


	}
		 
	protected boolean buildSDNForwardingTableVmBased(int srcVm, int dstVm, int flowId, Flow flow) {
		NetworkNIC desthost = findSDNHost(dstVm);		
		if(desthost == null){
			/*
			 * If desthost is null, it means the destination resides in a different datacenter.
			 * Send the packet to the gateway.
			 */
			desthost = this.getGateway(); 
			flow.setLabelPlace("outside");
		}
		NetworkNIC srcHost = findSDNHost(srcVm);
		if (srcHost == null) {
			srcHost = this.getGateway(); // packets coming from outside the datacenter			
		}
		sdnRoutingPolicy.buildRoute(srcHost, desthost, flow);
		return true;			
	}
	 	
	public SDNRoutingPolicy getSdnRoutingPoloicy() {
		return this.sdnRoutingPolicy;
	}

	
	public SDNTrafficSchedulingPolicy getSdnSchedulingPolicy() {
		return sdnSchedulingPolicy;
	}

	public void addVmsToSDNhosts(List<? extends Vm> vmList){
		this.vmList = vmList;
		
		for (Vm vm : this.vmList){
			NetworkOperatingSystem.debugVmIdName.put(vm.getId(),vm.getVmName());
		}		
	}
	
	public void setTopology(Topology topology, List<Host> hosts, List<SDNHost> sdnhosts, List<Switch> switches){	
		this.topology = topology;
		this.hosts = hosts;
		this.sdnhosts = sdnhosts;
		this.switches = switches;
		this.sdnRoutingPolicy.setNodeList(topology.getAllNodes(), topology);
		this.sdnRoutingPolicy.buildNodeRelations(topology);
		for(Switch sw : switches){
			if(sw.getSwType().equals("gateway")){
				this.gateway = sw;
			}
		}
	}				
	
	public void setEdgeDataCenterBroker(OsmoticBroker edgeDataCenterBroker) {
		edgeDatacenterBroker = edgeDataCenterBroker;
	}
	
	public OsmosisOrchestrator getWanOorchestrator() {
		return this.orchestrator;
	}
	
	public OsmoticBroker getEdgeDataCenterBroker() {
		return edgeDatacenterBroker;
	}

    public void setName(String name){
    	this.name = name;
    }
    
	public void setDatacenter(Datacenter dc) {		
		this.datacenter = dc;
		this.datacenterName = this.getDatacenter().getName();
	}	
	
	public Datacenter getDatacenter() {
		return datacenter;
	}

    public void setWanController(SDNController wanController) {
        this.wanController = wanController;
    }

    public SDNController getWanController() {
        return wanController;
    }

	public Switch getGateway() {
		return this.gateway;
	}

	public void addAllDatacenters(List<OsmoticDatacenter> osmesisDatacentres) {}
	public void initSdWANTopology(List<SwitchEntity> switches, List<LinkEntity> wanLinks, List<Switch> datacenterGateways) {}
}
