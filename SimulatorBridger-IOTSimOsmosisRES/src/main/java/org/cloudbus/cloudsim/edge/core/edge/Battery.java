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

import java.io.Serializable;

/**
 * 
 * @author Khaled Alwasel, Tomasz Szydlo
 * @contact kalwasel@gmail.com
 * @since IoTSim-Osmosis 1.0
 * 
**/

public class Battery implements Serializable {
	private double maxCapacity;
	private double currentCapacity;
	private double batterySensingRate;
	private double batterySendingRate;

	private boolean resPowered;
	private double peakSolarPower;
	private double batteryVoltage;
	private double maxChargingCurrent;

	boolean charging;
	private double chargingCurrent;

	public double getChargingCurrent() {
		return chargingCurrent;
	}

	public boolean isCharging() {
		return charging;
	}

	public void setCharging(boolean charging) {
		this.charging = charging;
	}

	public double getBatteryVoltage() {
		return batteryVoltage;
	}

	public void setBatteryVoltage(double batteryVoltage) {
		this.batteryVoltage = batteryVoltage;
	}

	public double getMaxChargingCurrent() {
		return maxChargingCurrent;
	}

	public void setMaxChargingCurrent(double maxChargingCurrent) {
		this.maxChargingCurrent = maxChargingCurrent;
	}

	public boolean isResPowered() {
		return resPowered;
	}

	public void setResPowered(boolean resPowered) {
		this.resPowered = resPowered;
	}

	public double getPeakSolarPower() {
		return peakSolarPower;
	}

	public void setPeakSolarPower(double peakSolarPower) {
		this.peakSolarPower = peakSolarPower;
	}

	public double getMaxCapacity() {
		return maxCapacity;
	}
	
	public void setMaxCapacity(double maxCapacity) {
		this.maxCapacity = maxCapacity;
	}

	public double getBatterySensingRate() {
		return batterySensingRate;
	}

	public void setBatterySensingRate(double batterySensingRate) {
		this.batterySensingRate = batterySensingRate;
	}
	
	public void setBatterySendingRate(double batterySendingRate) {
		this.batterySendingRate = batterySendingRate;
	}
	
	public double getBatterySendingRate() {
		return batterySendingRate;
	}
	
	public double getCurrentCapacity() {
		return currentCapacity;
	}
	public void initCapacity(double currentCapacity) {
		this.currentCapacity = currentCapacity;
	}
	public void setCurrentCapacity(double currentCapacity) {
//		if (currentCapacity > this.currentCapacity) {
//			System.err.println("ERROR");
//		}
		this.currentCapacity = currentCapacity;
	}
	public void decrementCapacity(double delta) {
		this.currentCapacity -= delta;
	}

	public void chargeBattery(double energyTransfer, double current){
		currentCapacity += energyTransfer;
		if (currentCapacity > maxCapacity){
			currentCapacity = maxCapacity;
			chargingCurrent = 0;
			//chargingCurrent = current;
		} else {
			chargingCurrent = current;
		}
	}

	public double getBatteryTotalConsumption(){
		if(this.currentCapacity < 0){
			this.currentCapacity = 0;
		}
		double consum = this.maxCapacity - this.currentCapacity;
		return consum;
	}
}
