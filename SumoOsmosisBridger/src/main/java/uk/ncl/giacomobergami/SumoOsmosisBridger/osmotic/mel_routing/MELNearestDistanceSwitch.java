package uk.ncl.giacomobergami.SumoOsmosisBridger.osmotic.mel_routing;

import com.eatthepath.jvptree.DistanceFunction;
import org.cloudbus.cloudsim.VmSchedulerTimeShared;
import org.cloudbus.cloudsim.edge.core.edge.EdgeDevice;
import org.cloudbus.osmosis.core.OsmoticBroker;
import uk.ncl.giacomobergami.components.allocation_policy.VmSchedulerTimeSharedEnergy;
import uk.ncl.giacomobergami.components.iot.IoTDevice;
import uk.ncl.giacomobergami.components.mel_routing.RoundRobinMELSwitchPolicy;
import uk.ncl.giacomobergami.utils.gir.CartesianPoint;
import uk.ncl.giacomobergami.utils.gir.SquaredCartesianDistanceFunction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MELNearestDistanceSwitch extends RoundRobinMELSwitchPolicy {
    private final DistanceFunction<CartesianPoint> f;
    private Map<String, Integer> roundRobinMelMap;

    public MELNearestDistanceSwitch() {
        f = SquaredCartesianDistanceFunction.getInstance();
        roundRobinMelMap = new HashMap<>();
    }

    @Override
    public boolean test(String s) {
        return (s.equals("*")) || super.test(s);
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
        double minimumDistance = Double.MAX_VALUE;
        EdgeDevice minimumHost = null;
        var hosts = self.selectVMFromHostPredicate();
        for (String host : hosts) {
            var edgeHost = self.resolveEdgeDeviceFromId(host);
            double edgeSqRange = edgeHost.signalRange * edgeHost.signalRange;
            ((VmSchedulerTimeSharedEnergy) edgeHost.getVmScheduler()).addUtilizationEntry();
            var squaredDistance = f.getDistance(ioTDevice, edgeHost);
            if (squaredDistance <= edgeSqRange) {
                if (squaredDistance <= minimumDistance) {
                    minimumDistance = squaredDistance;
                    minimumHost = edgeHost;
                }
            }
        }
        if (minimumHost != null) {
            var instances = minimumHost.getVmList();
            if (!roundRobinMelMap.containsKey(ioTDevice.getName())){
                roundRobinMelMap.put(ioTDevice.getName(),0);
            }
            int pos = roundRobinMelMap.get(ioTDevice.getName());
            if (pos>= instances.size()){
                pos=0;
            }
            var result = instances.get(pos);
            pos++;
            if (pos>= instances.size()){
                pos=0;
            }
            roundRobinMelMap.put(ioTDevice.getName(),pos);
            return result.getVmName();
        }
        return null;
    }
}
