/*
 * RoundRobinMELSwitchPolicy.java
 * This file is part of SimulatorBridger-IOTSimOsmosisRES
 *
 * Copyright (C) 2022 - Giacomo Bergami
 *
 * SimulatorBridger-IOTSimOsmosisRES is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * SimulatorBridger-IOTSimOsmosisRES is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with SimulatorBridger-IOTSimOsmosisRES. If not, see <http://www.gnu.org/licenses/>.
 */
package uk.ncl.giacomobergami.components.mel_routing;

import org.cloudbus.osmosis.core.OsmoticBroker;
import uk.ncl.giacomobergami.components.iot.IoTDevice;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.cloudbus.cloudsim.edge.utils.LogUtil.logger;

public class RoundRobinMELSwitchPolicy implements MELSwitchPolicy, Serializable {
    private Map<String, Integer> roundRobinMelMap;
    public RoundRobinMELSwitchPolicy() { roundRobinMelMap = new HashMap<>(); }
    @Override
    public String apply(IoTDevice IoTDevice, String abstractMel, OsmoticBroker self) {
        List<String> instances = getCandidateMELsFromPattern(abstractMel, self);
        if (instances.isEmpty()) {
            logger.warn("Warning: IoTDevice " + IoTDevice.getName()+" was expecting to communicate with someone, but no candidate was detected: the app is going to be discarded soon...");
            return null;
        }
        if (!roundRobinMelMap.containsKey(abstractMel)){
            roundRobinMelMap.put(abstractMel,0);
        }
        int pos = roundRobinMelMap.get(abstractMel);
        if (pos>= instances.size()){
            pos=0;
        }
        String result = instances.get(pos);
        pos++;
        if (pos>= instances.size()){
            pos=0;
        }
        roundRobinMelMap.put(abstractMel,pos);
        return result;
    }
}
