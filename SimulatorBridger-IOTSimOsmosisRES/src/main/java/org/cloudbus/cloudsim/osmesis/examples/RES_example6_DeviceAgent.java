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

import org.cloudbus.agent.AgentMessage;
import org.cloudbus.agent.DeviceAgent;
import org.jooq.DSLContext;

import java.sql.Connection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RES_example6_DeviceAgent extends DeviceAgent {

    List<AgentMessage> messages;

    Map<String, String> tempRoutingTable = new HashMap<>();
    Map<String, Double> tempBestRatio = new HashMap<>();

    //devices are located in Berlin


    public RES_example6_DeviceAgent() {
        //This is necessary for dynamic agent instance creation.
    }

    @Override
    public void monitor() {
        super.monitor();

        //Do nothing.

    }

    @Override
    public void analyze() {
        super.analyze();

        //Do nothing.
    }

    private boolean isInstance(String name){
        boolean result = name.matches("^\\S*.[0-9]+$");
        return result;
    }

    private String getAbstract(String name){
        return name.replaceAll(".[0-9]+$", ".*");
    }

    //selects with the highest value
    private void updateTempRoutingTable(String abs_mel, String inst_mel, double val){
        if (!tempRoutingTable.containsKey(abs_mel)){
            tempRoutingTable.put(abs_mel,inst_mel);
            tempBestRatio.put(abs_mel,val);
        }

        if (tempBestRatio.get(abs_mel)<val){
            tempBestRatio.put(abs_mel,val);
            tempRoutingTable.put(abs_mel,inst_mel);
        }
    }

    private double haversineDistance(double lat_1, double lon_1, double lat_2, double lon_2){
        double d2r = (Math.PI / 180.0);
        double d_long = (lon_2 - lon_1) * d2r;
        double d_lat = (lat_2 - lat_1) * d2r;
        double a = Math.pow(Math.sin(d_lat / 2.0), 2.0) + Math.cos(lat_1 * d2r) * Math.cos(lat_2 * d2r)
                * Math.pow(Math.sin(d_long / 2.0), 2.0);
        double c = 2.0 * Math.atan2(Math.sqrt(a), Math.sqrt(1.0 - a));
        return 6378.1370 * c;
    }

    @Override
    public void plan(String PowerModel, Connection conn, DSLContext context) {
        super.plan(PowerModel, conn, context);

        //Get already received messages and analyze the data.
        //It clears the incoming buffer.
        messages = getReceivedMessages();

        tempRoutingTable.clear();
        tempBestRatio.clear();

        //check if there is no energy from RES
        boolean noRES=false;
        for(AgentMessage message:messages) {
            RES_example6_AgentMessage ex6_message = (RES_example6_AgentMessage) message;
            double res = ex6_message.getGreenEnergyRatio();
            if (res>0.1) noRES = true;
        }

        if (!noRES){
            for (AgentMessage message : messages) {
                RES_example6_AgentMessage ex6_message = (RES_example6_AgentMessage) message;

                //choose the MEL instance that is closest to the device
                double val = - haversineDistance(ex6_message.getLat(), ex6_message.getLon(), 52.52, 13.40 );

                //choose the MEL instance that is located in the datacenter with highest percentage of low-carbon power
                // sources in the power grid
                //double val = ex6_message.getLowEmissionPercentage();

                for (String mel_name : ex6_message.getAvailableMELs()) {
                    if (isInstance(mel_name)) {
                        updateTempRoutingTable(getAbstract(mel_name), mel_name, val);
                    }
                }
            }
        } else {
            //choose the MEL instance from Edge with highest RES value
            for (AgentMessage message : messages) {
                RES_example6_AgentMessage ex6_message = (RES_example6_AgentMessage) message;

                double res = ex6_message.getGreenEnergyRatio();
                for (String mel_name : ex6_message.getAvailableMELs()) {
                    if (isInstance(mel_name)) {
                        updateTempRoutingTable(getAbstract(mel_name), mel_name, res);
                    }
                }
            }
        }
    }

    @Override
    public void execute() {
        super.execute();

        //Modify the routing to use the selected MEL instances
        for (String mel:tempRoutingTable.keySet()){
            getIoTDevice().getRoutingTable().addRule(mel,tempRoutingTable.get(mel));
        }
    }
}
