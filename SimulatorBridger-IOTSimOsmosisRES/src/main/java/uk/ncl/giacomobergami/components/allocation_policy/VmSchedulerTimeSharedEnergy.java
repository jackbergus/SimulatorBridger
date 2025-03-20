/*
 * Title:        CloudSimSDN
 * Description:  SDN extension for CloudSim
 * Licence:      GPL - http://www.gnu.org/copyleft/gpl.html
 *
 * Copyright (c) 2015, The University of Melbourne, Australia
 */

package uk.ncl.giacomobergami.components.allocation_policy;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.cloudbus.cloudsim.Pe;
import org.cloudbus.cloudsim.VmSchedulerTimeShared;
import org.cloudbus.cloudsim.core.MainEventManager;
import org.cloudbus.cloudsim.sdn.power.PowerUtilizationHistoryEntry;
import org.cloudbus.cloudsim.sdn.power.PowerUtilizationInterface;
import org.jooq.meta.duckdb.system.main.Main;

/**
 * VmSchedulerTimeSharedEnergy is a VMM allocation policy that allocates one or more Pe to a VM, and
 * allows sharing of PEs by time. If there is no free PEs to the VM, allocation fails. Free PEs are
 * not allocated to VMs
 * 
 * @author Rodrigo N. Calheiros
 * @author Anton Beloglazov
 * @author Jungmin Son
 *  * @since CloudSim Toolkit 1.0
 */
public class VmSchedulerTimeSharedEnergy extends VmSchedulerTimeShared implements PowerUtilizationInterface{

	private double lastTime = 0.0;
	
	public VmSchedulerTimeSharedEnergy(List<? extends Pe> pelist) {
		super(pelist);
	}

	@Override
	protected void setAvailableMips(double availableMips) {
		super.setAvailableMips(availableMips);
		addUtilizationEntry();
	}
	
	private HashMap<Double, PowerUtilizationHistoryEntry> utilizationHistories = null;
	private static double powerOffDuration = 3600; //if host is idle for 1 hours, it's turned off.
	
	public void addUtilizationEntryTermination(double terminatedTime) {
		DecimalFormat df = new DecimalFormat("#.###");
		df.setRoundingMode(RoundingMode.HALF_UP);
		terminatedTime = Double.parseDouble(df.format(terminatedTime));
		if(this.utilizationHistories != null)
			this.utilizationHistories.put(terminatedTime, new PowerUtilizationHistoryEntry(terminatedTime, 0));
	}
	
	public List<PowerUtilizationHistoryEntry> getUtilizationHistory() {
		List<PowerUtilizationHistoryEntry> LUHL = this.utilizationHistories.values().stream().toList();
		return LUHL;
	}

	public double getUtilizationEnergyConsumption() {
		
		double total=0;
		double lastTime=0;
		double lastMips=0;
		if(this.utilizationHistories == null)
			return 0;
		
		for(PowerUtilizationHistoryEntry h : this.utilizationHistories.values()) {
			double duration = h.startTime - lastTime;
			double utilPercentage = lastMips/ getTotalMips();
			double power = calculatePower(utilPercentage);
			double energyConsumption = power * duration;
			
			// Assume that the host is turned off when duration is long enough
			if(duration > powerOffDuration && lastMips == 0)
				energyConsumption = 0;
			
			total += energyConsumption;
			lastTime = h.startTime;
			lastMips = h.usedMips;
		}
		return total/3600;	// transform to Whatt*hour from What*seconds
	}
	
	private double calculatePower(double u) {
		double power = 120 + 154 * u;
		return power;
	}


	public void addUtilizationEntry() {
		double totalMips = getTotalMips();
		double usingMips = totalMips - this.getAvailableMips();
		if(usingMips < 0) {
			logger.error("addUtilizationEntry : using mips is negative, No way!");
		}
		if(utilizationHistories == null)
			utilizationHistories = new HashMap<>();//new ArrayList<>();
		double time = (double) (Math.round(MainEventManager.clock()) * 1000) /1000;
		this.utilizationHistories.put(time, new PowerUtilizationHistoryEntry(time, usingMips));
	}
	
	private double getTotalMips() {
		return this.getPeList().size() * this.getPeCapacity();
	}
}
