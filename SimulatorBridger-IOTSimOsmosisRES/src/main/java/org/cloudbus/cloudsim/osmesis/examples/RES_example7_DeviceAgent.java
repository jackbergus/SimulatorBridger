/*
 * Title:        IoTSim-Osmosis-RES 1.0
 * Description:  IoTSim-Osmosis-RES enables the testing and validation of osmotic computing applications
 * 			     over heterogeneous edge-cloud SDN-aware environments powered by the Renewable Energy Sources.
 *
 * Licence:      GPL - http://www.gnu.org/copyleft/gpl.html
 *
 * Copyright (c) 2021, Newcastle University (UK) and Saudi Electronic University (Saudi Arabia) and
 *                     AGH University of Science and Technology (Poland)
 *
 */

package org.cloudbus.cloudsim.osmesis.examples;

import org.cloudbus.agent.DeviceAgent;
import org.jooq.DSLContext;

import java.sql.Connection;

public class RES_example7_DeviceAgent extends DeviceAgent {

    public RES_example7_DeviceAgent() {
        //This is necessary for dynamic agent instance creation.
    }

    @Override
    public void monitor() {
        super.monitor();

        double capacity = getIoTDevice().getBattery().getCurrentCapacity();

        if (getIoTDevice().getBattery().isResPowered()){
            System.out.println("["+getIoTDevice().getName() + "]Battery capacity:"+(int) capacity + "mAh\t Charging:" + getIoTDevice().getBattery().isCharging() +"\t Charging current:"+(int)getIoTDevice().getBattery().getChargingCurrent()+"mA");
            //System.out.println((int) capacity);
            //System.out.println((int) getIoTDevice().getBattery().get);
        }

    }

    @Override
    public void analyze() {
        super.analyze();

        //Do nothing.
    }

    @Override
    public void plan(String PowerModel, Connection conn, DSLContext context) {
        super.plan(PowerModel, conn, context);

        //Do nothing.
    }

    @Override
    public void execute() {
        super.execute();

        //Do nothing.
    }
}
