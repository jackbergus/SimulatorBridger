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

import org.cloudbus.agent.DCAgent;

import java.util.ArrayList;
import java.util.List;

public class RES_example6_DCAgent extends DCAgent {

    List<String> melNames = new ArrayList<>();
    double resPower;
    double lowEmission;
    double lat;
    double lon;

    public RES_example6_DCAgent() {
        //This is necessary for dynamic agent instance creation.
    }

    @Override
    public void monitor() {
        super.monitor();
        melNames.clear();

        //Collect information about DC
        for (String name:getOsmesisDatacenter().getVmNameToIdList().keySet()){
            melNames.add(name);
        }

        //Get current Green Energy Ratio
        //it is assumed that there is at least one RES source
        if (energyController != null) {
            resPower = energyController.getRESCurrentPower();
            lowEmission = energyController.getPowerGrids().get(0).getLowEmission();
            lat = energyController.getEnergySources().get(0).getLocation().getLatitude();
            lon = energyController.getEnergySources().get(0).getLocation().getLongitude();
        }
    }

    @Override
    public void analyze() {
        super.analyze();

        //Create new message.
        RES_example6_AgentMessage message = (RES_example6_AgentMessage) newAgentMessage();

        //Prepapre message content - list of available MELs.
        for(String name:melNames){
            message.addMEL(name);
        }

        message.setGreenEnergyRatio(resPower);
        message.setLowEmissionPercentage(lowEmission);

        message.setLat(lat);
        message.setLon(lon);

        //Send to all neighbours (null destination means all - follows the agent topology defined in the example file).
        message.setDESTINATION(null);

        publishMessage(message);
    }

    @Override
    public void plan(String PowerModel) {
        super.plan(PowerModel);

        //There should not be any message in input queue.

        //Do nothing in DC.
    }

    @Override
    public void execute() {
        super.execute();

        //Do nothing in DC.
    }
}
