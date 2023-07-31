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

import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.edge.core.edge.LegacyConfiguration;
import org.cloudbus.cloudsim.edge.core.edge.LegacyConfiguration.LinkEntity;
import org.cloudbus.cloudsim.edge.core.edge.LegacyConfiguration.SwitchEntity;
import org.cloudbus.cloudsim.sdn.Link;
import org.cloudbus.cloudsim.sdn.NetworkNIC;
import org.cloudbus.cloudsim.sdn.Switch;
import uk.ncl.giacomobergami.components.sdn_routing.SDNRoutingPolicyGeneratorFacade;
import uk.ncl.giacomobergami.components.sdn_traffic.SDNTrafficPolicyGeneratorFacade;
import uk.ncl.giacomobergami.components.sdn_traffic.SDNTrafficSchedulingPolicy;
import uk.ncl.giacomobergami.components.sdn_routing.SDNRoutingPolicy;

/**
 * 
 * @author Khaled Alwasel
 * @contact kalwasel@gmail.com
 * @since IoTSim-Osmosis 1.0
 * 
**/

public class SDWANController extends SDNController {
	
	private List<OsmoticDatacenter> osmesisDatacentres;
	private Map<OsmoticDatacenter, List<Integer>> datacenterVmList;
	protected Topology topology;

	public SDWANController(LegacyConfiguration.WanEntity controllerEntity,
						   List<Switch> datacenterGateways) {
		this(controllerEntity.getControllers().getName(),
				SDNTrafficPolicyGeneratorFacade.generateFacade(controllerEntity.getControllers().getTrafficPolicy()),
				SDNRoutingPolicyGeneratorFacade.generateFacade(controllerEntity.getControllers().getRoutingPolicy()));
		setName(controllerEntity.getControllers().getName());
		initSdWANTopology(controllerEntity.getSwitches(),
				(Collection<LinkEntity>)controllerEntity.getLinks(),
				          datacenterGateways);
	}

	public SDWANController(String name,
						   String traffic,
						   String routing) {
		this(name,
				SDNTrafficPolicyGeneratorFacade.generateFacade(traffic),
				SDNRoutingPolicyGeneratorFacade.generateFacade(routing));
		setName(name);
	}
	
	public SDWANController(String name, SDNTrafficSchedulingPolicy sdnPolicy, SDNRoutingPolicy sdnRouting){
		super(name, sdnPolicy,sdnRouting);
		this.datacenterName = "WAN_Layer";
	}

	public void addAllDatacenters(List<OsmoticDatacenter> osmesisDatacentres) {
		this.osmesisDatacentres = osmesisDatacentres;
		this.datacenterVmList = new HashMap<>();
		for(OsmoticDatacenter dc : this.osmesisDatacentres){
			List<Integer> list = new ArrayList<>();
			for(Vm vm : dc.getVmList()){
				list.add(vm.getId());
			}
			datacenterVmList.put(dc, list);
		}		
	}
		
	public List<OsmoticDatacenter> getOsmesisDatacentres() {
		return osmesisDatacentres;
	}
	
	private OsmoticDatacenter findDatacenter(int vmId){
		OsmoticDatacenter datacenter = null;
		
		for(OsmoticDatacenter dc : this.osmesisDatacentres){
			if(datacenterVmList.get(dc).contains(vmId)){
				datacenter = dc;	
			}
		}
		return datacenter;
	}
	
	public void startTransmitting(Flow flow) {				

		int srcVm = flow.getOrigin();
		int dstVm = flow.getDestination();
		
		OsmoticDatacenter srcDC = findDatacenter(srcVm);
		OsmoticDatacenter destDC = findDatacenter(dstVm);
		
		NetworkNIC srchost = srcDC.getGateway();
		NetworkNIC dsthost = destDC.getGateway();
		
		int flowId = flow.getFlowId();
		
		if (srchost != null)			
		{		
			List<NetworkNIC> route = sdnRoutingPolicy.getRoute(flow.getOrigin(), flow.getDestination());
			if(route == null){
				sdnRoutingPolicy.buildRoute(srchost, dsthost, flow);
			}
				 												
			List<NetworkNIC> endToEndRoute = sdnRoutingPolicy.getRoute(flow.getOrigin(), flow.getDestination());
			if (endToEndRoute == null)
				System.err.println("UNEXPECTED!");
			flow.setNodeOnRouteList(endToEndRoute);
			
			List<Link> links = sdnRoutingPolicy.getLinks(flow.getOrigin(), flow.getDestination());
			flow.setLinkList(links);
			
			sendNow(destDC.getSdnController().getId(), OsmoticTags.BUILD_ROUTE, flow);
												
		} 		 	
	}

//	protected boolean buildSDNForwardingTableVmBased(NetworkNIC srcHost, NetworkNIC desthost, Flow flow) {
//		sdnRoutingPolicy.buildRoute(srcHost, desthost, flow);
////		NetworkNIC currentNode = null;
////		NetworkNIC nextNode = null;
////
////		int iterate = route.size()-1;
////		for(int i = iterate; i >= 0; i--){
////			currentNode = route.get(i);
////			if(currentNode.equals(desthost)){
////				break;
////			}else{
////				nextNode = route.get(i-1);
////			}
//////			currentNode.addRoute(srcHost, desthost, flowId, nextNode);
////		}
////		return true;
//	}

	public void initSdWANTopology(List<SwitchEntity> switchEntites,
								  Collection<LinkEntity> linkEntites,
								  List<Switch> datacenterGateway) {
		topology  = new Topology();		 		 
		switches= new ArrayList<>();
		 
		Hashtable<String,Integer> nameIdTable = new Hashtable<>();
					    		    		   	
		for(SwitchEntity switchEntity : switchEntites){							
			long iops = switchEntity.getIops();
			String switchName = switchEntity.getName();
			String switchType = switchEntity.getType();
			Switch sw = null;
			sw = new Switch(switchName, switchType, iops);					
			if(sw != null) {
				nameIdTable.put(switchName, sw.getAddress());
				this.topology.addNode(sw);
				this.switches.add(sw);
			}
		}
		
		if(datacenterGateway != null){
					
			for(Switch datacenterGW : datacenterGateway){										
				if(datacenterGateway != null) {
					nameIdTable.put(datacenterGW.getName(), datacenterGW.getAddress());
					this.topology.addNode(datacenterGW);
					this.switches.add(datacenterGW);
				}
			}	
		}
			
		for(LinkEntity linkEntity : linkEntites){									
				String src = linkEntity.getSource();  
				String dst = linkEntity.getDestination();				
				double bw = linkEntity.getBw();
				int srcAddress = nameIdTable.get(src);
				if(dst.equals("")){
					System.out.println("Null!");			
				}
				int dstAddress = nameIdTable.get(dst);
				topology.addLink(srcAddress, dstAddress, bw);
		}
		this.sdnRoutingPolicy.setNodeList(topology.getAllNodes(), topology);
	}
}
