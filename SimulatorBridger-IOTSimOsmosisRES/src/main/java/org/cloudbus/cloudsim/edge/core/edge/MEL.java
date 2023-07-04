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

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.cloudbus.cloudsim.core.MainEventManager;
import uk.ncl.giacomobergami.components.cloudlet_scheduler.CloudletScheduler;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.osmosis.core.Flow;
import org.cloudbus.osmosis.core.OsmoticBroker;
import uk.ncl.giacomobergami.components.cloudlet_scheduler.CloudletSchedulerGeneratorFactory;

/**
 * 
 * @author Khaled Alwasel
 * @contact kalwasel@gmail.com
 * @since IoTSim-Osmosis 1.0
 * 
**/

public class MEL extends Vm {
	
	private int edgeDatacenterId;	
	private double currentBw;
	private static final int SIZE = 2048;

	public MEL(int edgeDatacenterId, int id, int userId, double mips, int numberOfPes, int ram, long bw, String vmm,
			   CloudletScheduler cloudletScheduler) {
		super(id, userId, mips, numberOfPes, ram, bw, SIZE, vmm, cloudletScheduler);
		this.edgeDatacenterId = edgeDatacenterId;		
	}

//	public static CloudletScheduler initCloudletSchedulerFromClassName(String name) {
//		try {
//			return (CloudletScheduler) Class.forName(name).newInstance();
//		} catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
//			e.printStackTrace();
//			return null;
//		}
//	}

	public MEL(int edgeDatacenterId,
			   AtomicInteger vmId,
			   LegacyConfiguration.MELEntities melEntity,
			  OsmoticBroker broker) {
		this(edgeDatacenterId,
				vmId.getAndIncrement(),
				broker.getId(),
				melEntity.getMips(),
				melEntity.getPesNumber(),
				melEntity.getRam(),
				melEntity.getBw(),
				melEntity.getVmm(),
				CloudletSchedulerGeneratorFactory.generateFacade(melEntity.getCloudletSchedulerClassName()));
		setVmName(melEntity.getName());
	}
	
	public int getEdgeDatacenterId() {
		return edgeDatacenterId;
	}

	public void updateAssociatedIoTDevices() {		
		int numOfFlows = flowList.size();
		if(numOfFlows == 0){
			numOfFlows = 1;
		}
		this.currentBw  = this.getBw() / numOfFlows; // the updated bw 				
	}

	private List<Flow> flowList = new ArrayList<>(); 

	List<Flow> flowListHis =  new ArrayList<>();

	public List<Flow> getFlowList() {
		return flowList;
	}
	
	public void addFlow(Flow flow) {
		flowList.add(flow);	
		flowListHis.add(flow);	
	}

	public void removeFlows(LinkedList<Flow> removedList) {
		this.flowList.removeAll(removedList);
		
	}


	public double getCurrentBw() {
		return currentBw;
	}

}
