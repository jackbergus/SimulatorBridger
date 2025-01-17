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

import uk.ncl.giacomobergami.utils.data.YAML;
import uk.ncl.giacomobergami.utils.pipeline_confs.BatteryConfiguration;

import java.io.File;
import java.io.Serializable;
import java.util.Optional;

/**
 * 
 * @author Khaled Alwasel, Tomasz Szydlo
 * @contact kalwasel@gmail.com
 * @since IoTSim-Osmosis 1.0
 * 
**/

public class Battery implements Serializable {
	private final transient File battery_file = new File("clean_example/3_extIOTSim_configuration/battery_parameters.yaml");
	private final transient Optional<BatteryConfiguration> battery_conf = YAML.parse(BatteryConfiguration.class, battery_file);

	private final boolean Active = battery_conf.get().isActive();

	private final float a0 = battery_conf.get().getA0();
	private final float a1 = battery_conf.get().getA1();
	private final float a2 = battery_conf.get().getA2();
	private final float a3 = battery_conf.get().getA3();
	private final float a4 = battery_conf.get().getA4();
	private final float a5 = battery_conf.get().getA5();

	private final float b0 = battery_conf.get().getB0();
	private final float b1 = battery_conf.get().getB1();
	private final float b2 = battery_conf.get().getB2();
	private final float b3 = battery_conf.get().getB3();
	private final float b4 = battery_conf.get().getB4();
	private final float b5 = battery_conf.get().getB5();

	private final float c0 = battery_conf.get().getC0();
	private final float c1 = battery_conf.get().getC1();
	private final float c2 = battery_conf.get().getC2();

	private final float d0 = battery_conf.get().getD0();
	private final float d1 = battery_conf.get().getD1();
	private final float d2 = battery_conf.get().getD2();

	private final double PB = battery_conf.get().getCharging_DischargingPower();
	private final double PSB = battery_conf.get().getStandby_Loss();
	private final double EC = battery_conf.get().getMaximum_Battery_Capacity();

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
	public void decrementCapacity(double delta, double deltaTime, boolean transmit ,boolean isInjected) {
		if(!isInjected) {
			this.currentCapacity -= delta;
			if (Active && transmit) {
				double reduction = dischargeBattery(deltaTime);
				this.currentCapacity -= reduction;
			}
		}
		if(this.currentCapacity < 0 || Double.isInfinite(this.currentCapacity) || Double.isNaN(this.currentCapacity)) this.currentCapacity = 0;
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

	public double calculateOpenCircuitVoltage() {
		double VOC = a0*Math.exp(-a1*currentCapacity) + a2 + a3*currentCapacity - a4*Math.pow(currentCapacity, 2) + a5*Math.pow(currentCapacity, 3);
		return VOC;
	}

	private double calculateResistanceOhmicLosses() {
		double RS = b0*Math.exp(-b1*currentCapacity) + b2 + b3*currentCapacity - b4*Math.pow(currentCapacity, 2) + b5*Math.pow(currentCapacity, 3);
		return RS;
	}

	private double calculateResistanceChargeTransfer() {
		double RTS = c0*Math.exp(-c1*currentCapacity) + c2;
		return RTS;
	}

	private double calculateResistanceMembrabeDiffusion() {
		double RTL = d0*Math.exp(-d1*currentCapacity) + d2;
		return RTL;
	}

	public double calculateTotalResistance() {
		double RS = calculateResistanceOhmicLosses();
		double RTS = calculateResistanceChargeTransfer();
		double RTL = calculateResistanceMembrabeDiffusion();

		double RTOT = RS;// + RTS + RTL;
		return RTOT;
	}

	public double calculateCircuitCurrent(){
		double VOC = calculateOpenCircuitVoltage();
		double RTOT = calculateTotalResistance();

		double negCheck = Math.pow(VOC, 2) - 4 * RTOT * PB;
		double IT = negCheck > 0 ? (VOC - Math.sqrt(negCheck)) / (2 * RTOT) : 0;
		return IT;
	}

	public double chargeEfficiency(){
		double VOC = calculateOpenCircuitVoltage();
		double RTOT = calculateTotalResistance();
		double IT = calculateCircuitCurrent();

		double NC = VOC/(VOC - RTOT*IT);
		return NC;
	}

	public double dischargeEfficiency(){
		double VOC = calculateOpenCircuitVoltage();
		double RTOT = calculateTotalResistance();
		double IT = calculateCircuitCurrent();

		double NDC = (VOC - RTOT*IT)/ VOC;
		return NDC;
	}

	public double dischargeBattery(double deltaTime) {
		double NDC = dischargeEfficiency();
		double NC = chargeEfficiency();
		double discharge = 0;

		if(PB > 0) {
			discharge = (PB*deltaTime)/(EC*NDC);
		} else if (PB == 0) {
			discharge = (PSB*deltaTime)/(EC*NDC);
		}else if(PB < 0) {
			discharge = (PB*deltaTime*NC)/EC;
		}

		return discharge;
	}
}
