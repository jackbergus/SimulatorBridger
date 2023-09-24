/*
 * Title:        CloudSimSDN
 * Description:  SDN extension for CloudSim
 * Licence:      GPL - http://www.gnu.org/copyleft/gpl.html
 *
 * Copyright (c) 2015, The University of Melbourne, Australia
 */

package org.cloudbus.cloudsim.sdn;

import java.util.*;

import org.cloudbus.osmosis.core.Flow;
import org.cloudbus.cloudsim.core.MainEventManager;
import org.cloudbus.osmosis.core.OsmoticBroker;

/** 
 *   
 * @author Jungmin Son
 * @author Rodrigo N. Calheiros
 * @since CloudSimSDN 1.0
 * 
 * 
 * @author Khaled Alwasel
 * @contact kalwasel@gmail.com
 * @since BigDataSDNSim 1.0
 */
 

public class Channel {
	private List<NetworkNIC> nodes;
	private List<Link> links;
	private double allocatedBandwidth = 0; // Actual bandwidth allocated to the channel
	private double previousTime;

	private LinkedList<Flow> inTransmission;
	private LinkedList<Flow> completed;	
	private final int srcId;
	private final int dstId;
	private final int chId;	
	public static double transmissionTime = 0;	
	private Map<Double, Double> bwChangesLogMap = new TreeMap<>();

	public Channel(int chId, int srcId, int dstId, List<NetworkNIC> nodes, List<Link> links) {
		this.chId = chId;
		this.srcId = srcId;
		this.dstId = dstId;
		this.nodes = nodes;
		this.links = links;		

		this.inTransmission = new LinkedList<>();
		this.completed = new LinkedList<>();
	}		
	
	public void initialize() {
		links.forEach(link -> {
			if (link != null) link.addChannel(this);
		});
//		for(int i=0; i<nodes.size(); i++) {
//// 			NetworkNIC from = nodes.get(i);
//			Link link = links.get(i);
//			if(link != null){
//				link.addChannel(this);
//			}
//		}
	}
	
	public void terminate() {
		// Assign BW to all links
		links.forEach(link -> {
			if (link != null) link.removeChannel(this);
		});
	}
	
	public double getLowestSharedBandwidth() {
		// Get the lowest bandwidth along links in the channel
//		double lowestSharedBw = Double.POSITIVE_INFINITY;
//		double linkBw = 0;
		return links.stream()
				.filter(Objects::nonNull)
				.mapToDouble(Link::getFreeBandwidth)
				.min()
				.orElse(Double.POSITIVE_INFINITY);
//		for(int i=0; i<nodes.size()-1; i++) {
//
//			Link link = links.get(i);
//			if(link != null){
//				linkBw = link.getFreeBandwidth();
//				if (linkBw < lowestSharedBw){
//					lowestSharedBw = linkBw;
//				}
//			}
//		}
//		return lowestSharedBw;
	}	

	public boolean adjustSharedBandwidthAlongLink() {
		// Get the lowest bandwidth along links in the channel
		double lowestLinkBw = getLowestSharedBandwidth();
		if(this.allocatedBandwidth != lowestLinkBw) {
			changeBandwidth(lowestLinkBw);
			return true;
		}
		return false;
	}
	
	public boolean changeBandwidth(double newBandwidth){
		if (newBandwidth == allocatedBandwidth)
			return false; //nothing changed
		
		boolean isChanged = this.updateFlowProcessing();
		//boolean toChange = !isChanged && !this.completed.isEmpty();
		//this.allocatedBandwidth = !toChange ? newBandwidth : this.allocatedBandwidth;
		this.allocatedBandwidth = newBandwidth;

		this.bwChangesLogMap.put(MainEventManager.clock(), newBandwidth);

		if(!this.inTransmission.isEmpty()) {
			var choice = "MEL";
			var change = choice.equals("MEL") ? this.inTransmission.getFirst().getWorkflowTag().getIotDeviceFlow().getAppNameDest() : this.inTransmission.getFirst().getAppNameSrc();

			OsmoticBroker.updateEdgeTOCloudBandwidth(change, (float) this.allocatedBandwidth);
		}
		return isChanged;
	}

	public Map<Double, Double> getBwChangesLogMap() {
		return bwChangesLogMap;
	}

	public double getAllocatedBandwidth() {
		return allocatedBandwidth;
	}

	private double getAllocatedBandwidthPerFlow() {
		if(inTransmission.size() == 0) {
			return getAllocatedBandwidth();
		}
		return getAllocatedBandwidth()/inTransmission.size();
	}
	
	public int getActiveTransmissionNum() {
		return inTransmission.size();
	}	

	public boolean updateFlowProcessing(){
		double currentTime = MainEventManager.clock();
		double timeSpent = NetworkOperatingSystem.round(currentTime - this.previousTime);
		
		if(timeSpent <= 0 || inTransmission.size() == 0)
			return false;	// Nothing changed

		double processedThisRound = NetworkOperatingSystem.round((timeSpent*getAllocatedBandwidthPerFlow()));
		
		LinkedList<Flow> completedTransmissions = new LinkedList<>();
		for(Flow transmission: inTransmission){
			transmission.addCompletedLength(processedThisRound);
			
			if (transmission.isCompleted()){ 
				completedTransmissions.add(transmission);
				this.completed.add(transmission);
				transmissionTime = timeSpent;
			}	
		}
		
		this.inTransmission.removeAll(completedTransmissions);
		previousTime=currentTime;
		
		if(completedTransmissions.isEmpty())
			return false;	// Nothing changed
		return true;
	}
	 
	private double estimateFinishTime(Flow flow) {
		double bw = getAllocatedBandwidthPerFlow();		
		if(bw == 0) {
			return Double.POSITIVE_INFINITY;
		}		
		double eft = (double)flow.getAmountToBeProcessed()/bw;			
		return eft;
	}
	
	public double nextFinishTime() {
		double delay = Double.POSITIVE_INFINITY;

		for (Flow transmission:this.inTransmission){
			double eft = estimateFinishTime(transmission);
			if (eft<delay)
				delay = eft;
		}
		
		if(delay == Double.POSITIVE_INFINITY) {
			return delay;
		}
		else if(delay < 0) {
			throw new IllegalArgumentException("Channel.nextFinishTime: delay"+delay);
		}
		delay=NetworkOperatingSystem.round(delay);
		if (delay < NetworkOperatingSystem.getMinTimeBetweenNetworkEvents()) { 
			delay = NetworkOperatingSystem.getMinTimeBetweenNetworkEvents();
		}
		return delay;
	}

	public double addTransmission(Flow flow){
		if (this.inTransmission.isEmpty()) 
			previousTime= MainEventManager.clock();
		
		this.inTransmission.add(flow);
		double eft = estimateFinishTime(flow);
		return eft;
	}


	public void removeTransmission(Flow flow){
		inTransmission.remove(flow);
	}


	public LinkedList<Flow> getFinishedFlows(){
		LinkedList<Flow> returnList = new LinkedList<Flow>();
		if (!completed.isEmpty()){
			returnList.addAll(completed);
		}
		completed.removeAll(returnList);
		return returnList;
	}
	
	public int getChId() {
		return chId;
	}

	public double getLastUpdateTime(){
		return previousTime;
	}
	
	public String toString() {
		return "Channel("+this.srcId+"->"+this.dstId+"|"+this.chId
				+"): BW:"+allocatedBandwidth+", Transmissions:"+inTransmission.size();
	}

	public NetworkNIC getLastNode() {
		NetworkNIC node = this.nodes.get(this.nodes.size()-1);
		return node;
	}

	public int getSrcId() {
		return srcId;
	}

	public int getDstId() {
		return dstId;
	}

	private List<Flow> flowsList = new ArrayList<>();
	
	public void addFlowToList(Flow pkt){
		this.flowsList.add(pkt);
	}
	
	public List<Flow> getFlowList(){
		return this.flowsList;
	}
	
}
