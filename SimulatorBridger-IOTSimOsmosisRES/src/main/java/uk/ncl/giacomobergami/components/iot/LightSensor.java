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

package uk.ncl.giacomobergami.components.iot;

import org.cloudbus.cloudsim.core.SimEvent;
import org.cloudbus.cloudsim.edge.core.edge.LegacyConfiguration;
import org.jooq.DSLContext;

import java.sql.Connection;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 
 * @author Khaled Alwasel
 * @contact kalwasel@gmail.com
 * @since IoTSim-Osmosis 1.0
 * 
**/

public class LightSensor extends IoTDevice {


	
	public LightSensor(LegacyConfiguration.IotDeviceEntity onta, AtomicInteger flowid) {
		super( onta, flowid);
	}

	@Override
	public boolean updateBatteryBySensing() {
		battery.setCurrentCapacity(battery.getCurrentCapacity() - battery.getBatterySensingRate());
		if(battery.getCurrentCapacity()<0)
			return  true;
		return false;
	}

	@Override
	public boolean updateBatteryByTransmission() {
		battery.setCurrentCapacity(battery.getCurrentCapacity() - battery.getBatterySendingRate());
		if(battery.getCurrentCapacity()<0)
			return  true;
		return false;
	}

	@Override
	public void startEntity() {
		super.startEntity();
	}

	@Override
	public void processEvent(SimEvent ev) {
		super.processEvent(ev);
	}

	@Override
	public void processEvent(SimEvent ev, Connection conn, DSLContext context) {
		super.processEvent(ev);
	}

	@Override
	public void shutdownEntity() {
	}
}