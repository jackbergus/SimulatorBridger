/*
 * Title:        CloudSim Toolkit
 * Description:  CloudSim (Cloud Simulation) Toolkit for Modeling and Simulation of Clouds
 * Licence:      GPL - http://www.gnu.org/copyleft/gpl.html
 *
 * Copyright (c) 2009-2012, The University of Melbourne, Australia
 */

package org.cloudbus.cloudsim.power;

import java.util.List;
import java.util.Map;

import org.cloudbus.cloudsim.DatacenterCharacteristics;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Storage;
import org.cloudbus.cloudsim.Vm;
import uk.ncl.giacomobergami.components.allocation_policy.VmAllocationPolicy;
import org.cloudbus.cloudsim.core.MainEventManager;
import org.cloudbus.cloudsim.core.CloudSimTags;
import org.cloudbus.cloudsim.core.predicates.PredicateType;

/**
 * PowerDatacenterNonPowerAware is a class that represents a non-power aware data center in the
 * context of power-aware simulations.
 * 
 * If you are using any algorithms, policies or workload included in the power package please cite
 * the following paper:
 * 
 * Anton Beloglazov, and Rajkumar Buyya, "Optimal Online Deterministic Algorithms and Adaptive
 * Heuristics for Energy and Performance Efficient Dynamic Consolidation of Virtual Machines in
 * Cloud Data Centers", Concurrency and Computation: Practice and Experience (CCPE), Volume 24,
 * Issue 13, Pages: 1397-1420, John Wiley & Sons, Ltd, New York, USA, 2012
 * 
 * @author Anton Beloglazov
 * @since CloudSim Toolkit 2.0
 */
public class PowerDatacenterNonPowerAware extends PowerDatacenter {

	/**
	 * Instantiates a new datacenter.
	 * 
	 * @param name the name
	 * @param characteristics the res config
	 * @param schedulingInterval the scheduling interval
	 * @param utilizationBound the utilization bound
	 * @param vmAllocationPolicy the vm provisioner
	 * @param storageList the storage list
	 * 
	 * @throws Exception the exception
	 */
	public PowerDatacenterNonPowerAware(
			String name,
			DatacenterCharacteristics characteristics,
			VmAllocationPolicy vmAllocationPolicy,
			List<Storage> storageList,
			double schedulingInterval) throws Exception {
		super(name, characteristics, vmAllocationPolicy, storageList, schedulingInterval);
	}

	/**
	 * Updates processing of each cloudlet running in this PowerDatacenter. It is necessary because
	 * Hosts and VirtualMachines are simple objects, not entities. So, they don't receive events and
	 * updating cloudlets inside them must be called from the outside.
	 * 
	 * @pre $none
	 * @post $none
	 */
	@Override
	protected void updateCloudletProcessing(double deltaTime) {
		if (getCloudletSubmitted() == -1 || getCloudletSubmitted() == MainEventManager.clock()) {
			MainEventManager.cancelAll(getId(), new PredicateType(CloudSimTags.VM_DATACENTER_EVENT));
			schedule(getId(), getSchedulingInterval(), CloudSimTags.VM_DATACENTER_EVENT);
			return;
		}
		double currentTime = MainEventManager.clock();
		double timeframePower = 0.0;

		if (currentTime > getLastProcessTime()) {
			double timeDiff = currentTime - getLastProcessTime();
			double minTime = Double.MAX_VALUE;


			for (PowerHost host : this.<PowerHost> getHostList()) {
				logger.debug(
						String.format("%.2f: Host #%d", MainEventManager.clock(), host.getId()));

				double hostPower = 0.0;

				try {
					hostPower = host.getMaxPower() * timeDiff;
					timeframePower += hostPower;
				} catch (Exception e) {
					e.printStackTrace();
				}

				logger.debug(String.format(
								"%.2f: Host #%d utilization is %.2f%%",
						MainEventManager.clock(),
						host.getId(),
						host.getUtilizationOfCpu() * 100));
				;
				logger.debug(String.format(
						"%.2f: Host #%d energy is %.2f W*sec",
						MainEventManager.clock(),
						host.getId(),
						hostPower));
			}

			logger.debug(
					String.format("\n%.2f: Consumed energy is %.2f W*sec\n", MainEventManager.clock(), timeframePower));
			logger.debug("\n\n--------------------------------------------------------------\n\n");

			for (PowerHost host : this.<PowerHost> getHostList()) {
				logger.debug(String.format("\n%.2f: Host #%d", MainEventManager.clock(), host.getId()));

				double time = host.updateVmsProcessing(currentTime); // inform VMs to update
																		// processing
				if (time < minTime) {
					minTime = time;
				}
			}

			setPower(getPower() + timeframePower);

			checkCloudletCompletion();

			/** Remove completed VMs **/
			for (PowerHost host : this.<PowerHost> getHostList()) {
				for (Vm vm : host.getCompletedVms()) {
					getVmAllocationPolicy().deallocateHostForVm(vm);
					getVmList().remove(vm);
					logger.info("VM #" + vm.getId() + " has been deallocated from host #" + host.getId());
				}
			}

			if (!isDisableMigrations()) {
				List<Map<String, Object>> migrationMap = getVmAllocationPolicy().optimizeAllocation(
						getVmList());

				if (migrationMap != null) {
					for (Map<String, Object> migrate : migrationMap) {
						Vm vm = (Vm) migrate.get("vm");
						PowerHost targetHost = (PowerHost) migrate.get("host");
						PowerHost oldHost = (PowerHost) vm.getHost();

						if (oldHost == null) {
							logger.debug(String.format(
									"%.2f: Migration of VM #%d to Host #%d is started",
									MainEventManager.clock(),
									vm.getId(),
									targetHost.getId()));
						} else {
							logger.debug(String.format(
									"%.2f: Migration of VM #%d from Host #%d to Host #%d is started",
									MainEventManager.clock(),
									vm.getId(),
									oldHost.getId(),
									targetHost.getId()));
						}

						targetHost.addMigratingInVm(vm);
						incrementMigrationCount();

						/** VM migration delay = RAM / bandwidth + C (C = 10 sec) **/
						send(
								getId(),
								vm.getRam() / ((double) vm.getBw() / 8000) + 10,
								CloudSimTags.VM_MIGRATE,
								migrate);
					}
				}
			}

			// schedules an event to the next time
			if (minTime != Double.MAX_VALUE) {
				MainEventManager.cancelAll(getId(), new PredicateType(CloudSimTags.VM_DATACENTER_EVENT));
				// CloudSim.cancelAll(getId(), CloudSim.SIM_ANY);
				send(getId(), getSchedulingInterval(), CloudSimTags.VM_DATACENTER_EVENT);
			}

			setLastProcessTime(currentTime);
		}
	}

}
