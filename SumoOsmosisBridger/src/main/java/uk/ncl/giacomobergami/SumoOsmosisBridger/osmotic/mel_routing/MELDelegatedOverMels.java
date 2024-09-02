package uk.ncl.giacomobergami.SumoOsmosisBridger.osmotic.mel_routing;

import org.cloudbus.osmosis.core.OsmoticBroker;
import uk.ncl.giacomobergami.components.iot.IoTDevice;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.cloudbus.cloudsim.edge.utils.LogUtil.logger;

public class MELDelegatedOverMels extends MELNearestDistanceSwitch {

    private Map<String, Integer> roundRobinMelMap;
    public MELDelegatedOverMels() {
        roundRobinMelMap = new HashMap<>();
    }

    @Override
    public boolean test(String s) {
        return (s.startsWith("@")) || super.test(s);
    }

    @Override
    public List<String> getCandidateMELsFromPattern(String pattern, OsmoticBroker self) {
        if (pattern.equals("*")) {
            return new ArrayList<>(self.iotVmIdByName.keySet());
        } else {
            return super.getCandidateMELsFromPattern(pattern, self);
        }
    }

    @Override
    public String apply(IoTDevice ioTDevice,
                        String melName,
                        OsmoticBroker self) {
        if (melName.startsWith("@")) {
            var host = melName.substring(1);
            var minimumHost = self.resolveEdgeDeviceFromId(host);
            if (minimumHost == null) {
                logger.warn("Warning: the MELs are likely not to be associated to a host: stopping the communication");
                return null;
            }
            var instances = minimumHost.getVmList();
            if (!roundRobinMelMap.containsKey(host)){
                roundRobinMelMap.put(host,0);
            }
            int pos = roundRobinMelMap.get(host);
            if (pos>= instances.size()){
                pos=0;
            }
            var result = instances.get(pos);
            pos++;
            if (pos>= instances.size()){
                pos=0;
            }
            roundRobinMelMap.put(host,pos);
            return result.getVmName();
        } else {
            return super.apply(ioTDevice, melName, self);
        }
    }
}
