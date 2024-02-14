/*
 * Title:        BigDataSDNSim 1.0
 * Description:  BigDataSDNSim enables the simulating of MapReduce, big data management systems (YARN), 
 * 				 and software-defined networking (SDN) within cloud environments.
 * 
 * Licence:      GPL - http://www.gnu.org/copyleft/gpl.html
 *
 * Copyright (c) 2020, Newcastle University (UK) and Saudi Electronic University (Saudi Arabia) 
 * 
 */

package uk.ncl.giacomobergami.components.sdn_routing;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;

import java.util.LinkedList;
import java.util.List;

import org.cloudbus.cloudsim.sdn.Link;
import org.cloudbus.cloudsim.sdn.NetworkNIC;
import org.cloudbus.cloudsim.sdn.SDNHost;
import org.cloudbus.osmosis.core.Flow;
import org.cloudbus.osmosis.core.SDNRoutingTable;
import org.cloudbus.osmosis.core.Topology;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

/**
 * 
 * @author Khaled Alwasel
 * @contact kalwasel@gmail.com
 * @since BigDataSDNSim 1.0
 */

public abstract class SDNRoutingPolicy implements Serializable {
	private List<NetworkNIC> nodeList = new ArrayList<>();
	protected Topology topology;
	private String policyName;

	public Topology getTopology() {
		return topology;
	}

	public NetworkNIC inefficientNodeByName(String name) {
		for (var node : nodeList) {
			if (node.getName().equals(name))
				return node;
		}
		return null;
	}
//	public abstract NetworkNIC getNode(SDNHost srcHost, NetworkNIC node, SDNHost desthost, String destApp);
	public abstract void updateSDNNetworkGraph();	
	public abstract List<NetworkNIC> buildRoute(NetworkNIC srcHost, NetworkNIC destHost, Flow pkt);
	public abstract List<NetworkNIC> getRoute(int source, int dest);
	public abstract List<Link> getLinks(int source, int dest);
//	public abstract List<SDNRoutingTable> constructRoutes(NetworkNIC node, NetworkNIC desthost, NetworkNIC srcHost);

	public String getPolicyName() {
		return policyName;
	}

	public void setPolicyName(String policyName) {
		this.policyName = policyName;
	}

	public void buildNodeRelations(Topology topology) {	
		for (NetworkNIC nd : getNodeList()) {
			List<NetworkNIC> adjuNodes = new ArrayList<>();
			List<Link> nodeAdjacentLinks = new ArrayList<>();
			// get all adjacent links of nd
			nodeAdjacentLinks.addAll(topology.getAdjacentLinks(nd));
			for (Link l : nodeAdjacentLinks) {
				NetworkNIC node = l.getOtherNode(nd);
				if (!adjuNodes.contains(node)) {
					adjuNodes.add(node);
				}
			}
		}
	}
	public List<NetworkNIC> getNodeList() {
		return nodeList;
	}
	
	public void setNodeList(Collection<NetworkNIC> nodeList, Topology topology) {
		this.nodeList.addAll(nodeList);
		this.topology = topology;
	}

}
