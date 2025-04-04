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

package org.cloudbus.osmosis.core;

import java.io.Serializable;
import java.sql.Connection;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import org.cloudbus.cloudsim.sdn.Link;
import org.cloudbus.cloudsim.sdn.NetworkNIC;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Table;
import uk.ncl.giacomobergami.components.OsmoticRunner;
import uk.ncl.giacomobergami.components.simulator.OsmoticWrapper;
import uk.ncl.giacomobergami.utils.database.JavaPostGres;

/**
 * 
 * @author Khaled Alwasel
 * @contact kalwasel@gmail.com
 * @since BigDataSDNSim 1.0
 */

public class Topology implements Serializable {

	Hashtable<Integer,NetworkNIC> nodesTable;	
	Table<Integer, Integer, Link> links; 	
	Multimap<NetworkNIC,Link> nodeLinks;	// Node -> all Links
	Table<Integer, Integer, List<Link>> nTnlinks; // store the links from one vertex/node to vertex/node
	List<Link> nodeLinkLists; 
	AtomicInteger LinkID = new AtomicInteger(0);
	ArrayList<Link> linkList;
	public HashMap<Integer, Link> linkMap;
	public HashBasedTable<Integer, Integer, Integer> numLinks = HashBasedTable.create();
    private  List<CloudDatacenter> datacentres = null;
    private  SDNController wanController  = null;
	int Topology_ID;

	
	public Topology() {
		nodesTable = new Hashtable<>();
		nodeLinks = HashMultimap.create();
		links = HashBasedTable.create();
		linkMap = new HashMap<>();
		this.nTnlinks = HashBasedTable.create();
		this.Topology_ID = OsmoticWrapper.TopologyID.getAndIncrement();
	}
	
	public Link getLink(int from, int to) {
		return links.get(from, to);
	}
	public NetworkNIC getNode(int id) {
		return nodesTable.get(id);
	}
	
	public void addNode(NetworkNIC node){
		nodesTable.put(node.getAddress(), node);
	}

	public void removeNode(NetworkNIC node) {
		nodesTable.remove(node.getAddress());
		Collection<Link> linksToRemove = nodeLinks.removeAll(node);
		var nodeAddr = node.getAddress();
		for (Link edge : linksToRemove) {
			nodeLinkLists.remove(edge);
			NetworkNIC dst = edge.dst();
			var ls = nTnlinks.get(node.getAddress(), dst.getAddress());
			ls.remove(edge);
			if (ls.isEmpty()) nTnlinks.remove(node.getAddress(), dst.getAddress());
			var ls2 = nodeLinks.get(dst);
			ls2.remove(edge);
			if (ls.isEmpty()) nodeLinks.removeAll(dst);
			links.remove(nodeAddr, dst.getAddress());
		}
	}


	public void addLink(int from, int to, double bw, Connection conn, boolean update) {
		NetworkNIC fromNode = nodesTable.get(from);
		NetworkNIC toNode = nodesTable.get(to);

		if(!nodesTable.containsKey(from)||!nodesTable.containsKey(to)){
			throw new IllegalArgumentException("Unknown node on link:"+nodesTable.get(from).getAddress()+"->"+nodesTable.get(to).getAddress());
		}

		Link l = new Link(fromNode, toNode, bw, LinkID.getAndIncrement(), this.Topology_ID);

		// Two way links (From -> to, To -> from)		
		links.put(from, to, l);
		links.put(to, from, l);

		nodeLinks.put(fromNode, l);
		nodeLinks.put(toNode, l);
		linkMap.put(l.getLinkID(), l);

		if(nTnlinks.get(from, to)== null){
			nodeLinkLists = new ArrayList<Link>();
			nTnlinks.put(from, to, nodeLinkLists);
			OsmoticWrapper.linkChannels.put(from, to, 0);
		}
		if(nTnlinks.get(to, from) == null){
			nodeLinkLists = new ArrayList<Link>();
			nTnlinks.put(to, from, nodeLinkLists);
			OsmoticWrapper.linkChannels.put(to, from, 0);
		}

		List<Link> temLink_1 = nTnlinks.get(from, to);
		if(!temLink_1.contains(l)){
			temLink_1.add(l);
			nTnlinks.put(from, to, temLink_1);
			numLinks.put(from, to, temLink_1.size());
			//if(update) {
				String entry = "(" + OsmoticRunner.linkID.getAndIncrement() + ", " + l.getToplogyID() + ", " + l.getLinkID() + ", " + from + ", " + to + ", " + bw + ", " + 0 + ")";
				JavaPostGres.INSERTLinkData(conn, "sourceToDestLinks (unique_entry_id, topology_id, link_id, from_id, to_id, bw, nochannels)", entry);
			//}
			//JavaPostGres.updateLinkData(conn, bw, from, to);

		}
		
		List<Link> temLink_2 = nTnlinks.get(to, from);
		if(!temLink_2.contains(l)){
			temLink_2.add(l);
			nTnlinks.put(to, from, temLink_1);
			numLinks.put(to, from, temLink_1.size());
			//if(update) {
				String entry = "(" + OsmoticRunner.linkID.getAndIncrement() + ", " + l.getToplogyID() + ", " + l.getLinkID() + ", " + to + ", " + from + ", " + bw + ", " + 0 + ")";
				JavaPostGres.INSERTLinkData(conn, "sourceToDestLinks (unique_entry_id, topology_id, link_id, from_id, to_id, bw, nochannels)", entry);
			//}
		}

	}
	
	public Collection<Link> getAdjacentLinks(NetworkNIC node) {
		return nodeLinks.get(node);
	}
	
	public Collection<NetworkNIC> getAllNodes() {
		return nodesTable.values();
	}
	
	public Collection<Link> getAllLinks() {
		return nodeLinks.values();
	}

	public Link getLinkfromMap(Integer linkID) {
		return linkMap.get(linkID);
	}

	public List<Link> getNodeToNodeLinks(NetworkNIC srcNode, NetworkNIC destNode) {
		return nTnlinks.get(srcNode.getAddress(), destNode.getAddress());
	}

    public void setTopology(List<CloudDatacenter> datacentres, SDNController wanController) {    	
        this.datacentres = datacentres;
        this.wanController = wanController;
    }

    public List<CloudDatacenter> getDatacentres() {
        return datacentres;
    }
    public SDNController getWanController() { 
    	return wanController;  
    }

    public void removeLink(int srcAddress, int dstAddress) {
		NetworkNIC fromNode = nodesTable.get(srcAddress);
		NetworkNIC toNode = nodesTable.get(dstAddress);
		var ls = nTnlinks.remove(fromNode.getAddress(), toNode.getAddress());
		nodeLinkLists.removeAll(ls);
		nodeLinks.get(fromNode).removeAll(ls);
		nodeLinks.get(toNode).removeAll(ls);
		links.remove(srcAddress, dstAddress);
		links.remove(dstAddress, srcAddress);
	}
}
