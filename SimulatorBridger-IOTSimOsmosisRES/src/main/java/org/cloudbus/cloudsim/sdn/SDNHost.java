/*
 * Title:        CloudSimSDN
 * Description:  SDN extension for CloudSim
 * Licence:      GPL - http://www.gnu.org/copyleft/gpl.html
 *
 * Copyright (c) 2015, The University of Melbourne, Australia
 */

package org.cloudbus.cloudsim.sdn;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.core.SimEntity;
import org.cloudbus.cloudsim.core.SimEvent;
import org.jooq.DSLContext;


/**
 * Extended class of Host to support SDN.
 * Added function includes data transmission after completion of Cloudlet compute processing.
 * 
 * @author Jungmin Son
 * @author Rodrigo N. Calheiros
 * @since CloudSimSDN 1.0
 */
public class SDNHost extends SimEntity implements NetworkNIC {
	Host host;	
	ForwardingTable forwardingTable;
	List<NetworkNIC> adjuNodes = new ArrayList<>(); 
	String hostName; 
	
	public SDNHost(Host host, String name){
		super(name);		
		this.host=host;	
		this.hostName = name;
		this.forwardingTable = new ForwardingTable();
	}
	
	public Host getHost(){
		return host;
	}

	@Override
	public void startEntity(){}
	
	@Override
	public void shutdownEntity(){}

	@Override
	public void processEvent(SimEvent ev) {
		int tag = ev.getTag();
		switch(tag){

			default: System.out.println("Unknown event received by "+super.getName()+". Tag:"+ev.getTag());
		}
	}

	@Override
	public void processEvent(SimEvent ev, Connection conn, DSLContext context) {
		int tag = ev.getTag();
		switch(tag){

			default: System.out.println("Unknown event received by "+super.getName()+". Tag:"+ev.getTag());
		}
	}
	
	/******* Routeable interface implementation methods ******/

	@Override
	public int getAddress() {
		return super.getId();
	}
	
	public String toString() {
		return "SDNHost: "+this.getName();
	}
}
