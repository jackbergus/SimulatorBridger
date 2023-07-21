/*
 * Title:        CloudSimSDN
 * Description:  SDN extension for CloudSim
 * Licence:      GPL - http://www.gnu.org/copyleft/gpl.html
 *
 * Copyright (c) 2015, The University of Melbourne, Australia
 */

package org.cloudbus.cloudsim.sdn;


import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;

import java.util.List;
import java.util.Map;


import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Vm;

import org.cloudbus.cloudsim.core.MainEventManager;

import org.cloudbus.cloudsim.core.SimEntity;
import org.cloudbus.cloudsim.core.SimEvent;

import org.cloudbus.osmosis.core.Topology;

/**
 * NOS calculates and estimates network behaviour. It also mimics SDN Controller functions.  
 * It manages channels between switches, and assigns packages to channels and control their completion
 * Once the transmission is completed, forward the packet to the destination.
 * 
 * @author Jungmin Son
 * @author Rodrigo N. Calheiros
 * @since CloudSimSDN 1.0
 */
public abstract class NetworkOperatingSystem extends SimEntity {

	protected String physicalTopologyFileName; 
	protected Topology topology;

	protected List<Host> hosts;
	protected List<SDNHost> sdnhosts;
	protected List<Switch> switches= new ArrayList<Switch>();

	
	protected List<? extends Vm> vmList;

	protected Map<String, Integer> vmNameIdTable = new HashMap<String, Integer>();;
	Map<String, Integer> flowNameIdTable;
	public static Map<Integer, String> debugVmIdName = new HashMap<Integer, String>();
	public static Map<Integer, String> debugFlowIdName = new HashMap<Integer, String>();	
	
	
	// Resolution of the result.
	public static double minTimeBetweenEvents = 0.001;	// in sec
	public static int resolutionPlaces = 5;
	public static int timeUnit = 1;	// 1: sec, 1000: msec

	public NetworkOperatingSystem(String name) {
		super(name);
	}
	
	public static double getMinTimeBetweenNetworkEvents() {
	    return minTimeBetweenEvents* timeUnit;
	}
	
	public static double round(double value) {
		if(value == 0) return value;
		int places = resolutionPlaces;
	    if (places < 0) throw new IllegalArgumentException();

		if(timeUnit >= 1000) value = Math.floor(value*timeUnit);
		
	    BigDecimal bd = new BigDecimal(value);
	    bd = bd.setScale(places, RoundingMode.CEILING);
	    return bd.doubleValue();
	}
		
	@Override
	public void startEntity() {}

	@Override
	public void shutdownEntity() {}
	
	@Override
	public void processEvent(SimEvent ev) {
		int tag = ev.getTag();
		
		switch(tag){
			default:
				System.out.println("NOS --> Unknown event received by "+super.getName()+". Tag:"+ev.getTag());
		}
	}
	
	protected void processCompleteFlows(List<Channel> channels){}
		
	public Map<String, Integer> getVmNameIdTable() {
		return this.vmNameIdTable;
	}
	public Map<String, Integer> getFlowNameIdTable() {
		return this.flowNameIdTable;
	}


	public List<Host> getHostList() {
		return this.hosts;				
	}
	
	public List<Switch> getSwitchList() {
		return this.switches;
	}

	protected Vm findVm(int vmId) {
		for(Vm vm:vmList) {
			if(vm.getId() == vmId)
				return vm;
		}
		return null;
	}
	
	protected SDNHost findSDNHost(Host host) {
		for(SDNHost sdnhost:sdnhosts) {
			if(sdnhost.getHost().equals(host)) {
				return sdnhost;
			}
		}
		return null;
	}
	
	protected SDNHost findSDNHost(int vmId) {
		Vm vm = findVm(vmId);
		if(vm == null)
			return null;
		
		for(SDNHost sdnhost:sdnhosts) {
			if(sdnhost.getHost().equals(vm.getHost())) {
				return sdnhost;
			}
		}
		return null;
	}
	
	public int getHostAddressByVmId(int vmId) {
		Vm vm = findVm(vmId);
		if(vm == null) {
			logger.error(MainEventManager.clock() + ": " + getName() + ": Cannot find VM with vmId = "+ vmId);
			return -1;
		}
		
		Host host = vm.getHost();
		SDNHost sdnhost = findSDNHost(host);
		if(sdnhost == null) {
			logger.error(MainEventManager.clock() + ": " + getName() + ": Cannot find SDN Host with vmId = "+ vmId);
			return -1;
		}
		
		return sdnhost.getAddress();
	}
}
