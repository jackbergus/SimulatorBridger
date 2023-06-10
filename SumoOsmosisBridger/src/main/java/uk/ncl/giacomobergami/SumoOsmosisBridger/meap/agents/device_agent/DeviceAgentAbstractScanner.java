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

package uk.ncl.giacomobergami.SumoOsmosisBridger.meap.agents.device_agent;

import org.cloudbus.agent.AgentBroker;
import org.cloudbus.agent.DeviceAgent;
import org.cloudbus.cloudsim.core.MainEventManager;
import org.cloudbus.cloudsim.edge.core.edge.EdgeDataCenter;
import org.cloudbus.cloudsim.edge.core.edge.EdgeDevice;
import org.cloudbus.osmosis.core.OsmoticAppDescription;
import org.cloudbus.osmosis.core.OsmoticBroker;
import org.cloudbus.osmosis.core.OsmoticTags;
import uk.ncl.giacomobergami.SumoOsmosisBridger.meap.messages.PayloadForIoTAgent;
import uk.ncl.giacomobergami.SumoOsmosisBridger.meap.messages.MessageWithPayload;
import uk.ncl.giacomobergami.utils.gir.SquaredCartesianDistanceFunction;
import uk.ncl.giacomobergami.utils.structures.ImmutablePair;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.cloudbus.cloudsim.core.CloudSimTags.MAPE_WAKEUP_FOR_COMMUNICATION;
import static org.cloudbus.osmosis.core.OsmoticTags.MOVING;

public class DeviceAgentAbstractScanner extends DeviceAgent {
    private static SquaredCartesianDistanceFunction f;
    AtomicInteger ai;

    public DeviceAgentAbstractScanner() {
        ai = new AtomicInteger(1);
        f = SquaredCartesianDistanceFunction.getInstance();
    }

    protected List<ImmutablePair<EdgeDataCenter, EdgeDevice>> ls = Collections.emptyList();

    @Override
    public void monitor() {
        super.monitor();
        if (ls != null) ls.clear();

        // TODO: doing this if and only if the device is going to communicate with the device now

        // Monitoring the neighbouring nodes
        var iot = getIoTDevice();
        // Returning if the agent, at this current time, is not scheduled for transmission
//        if (!iot.transmit) return;

        ls = AgentBroker
                .getInstance()
                .getOsmoticDataCentersStream()
                .filter(x -> (x instanceof EdgeDataCenter))
                .flatMap(x->x.getSdnhosts().stream().map(y -> new ImmutablePair<>(x, y.getHost())))
                .filter(x -> {
                    if (!(x.getRight() instanceof EdgeDevice)) return false;
                    var obj = (EdgeDevice)x.getRight();
                    var distance = Math.sqrt(f.getDistance(iot, obj.location));
                    return ((distance <= iot.mobility.signalRange) && (distance <= obj.signalRange));
                })
                .map(x -> new ImmutablePair<>(((EdgeDataCenter)x.getLeft()), ((EdgeDevice) x.getRight())))
                .collect(Collectors.toList());
    }

    @Override
    public void execute() {
        // Executing = setting the broker to send the message
        // In practice, the IoT node starts communicating with the
        // designated MEL

        super.execute();

        // If I did not set this up, there is no meaning on resolving the nearest Edge
        // device over which perform a distributed communication
        if ((ls != null) && (!ls.isEmpty())) {
            // Still, against multiple possible candidates, the node is always picking the nearest!
            var nearest = getReceivedMessages()
                    .stream()
                    .min(Comparator.comparing(o -> f.getDistance(getIoTDevice().mobility.location, ((MessageWithPayload<PayloadForIoTAgent>) o).getPayload())));

            // Starting communicating only if there is a nearest candidate
            if (nearest.isPresent()) {
                getIoTDevice().transmit = true;
                var message = nearest.get();
                double StartDataGenerationTime = MainEventManager.clock();
                int appID = MainEventManager.getNewAppId();
                String appName = "App_"+appID;
                double DataRate = 1.0; //1.0;
                double StopDataGenerationTime = StartDataGenerationTime+DataRate;
                String ioTDeviceName = getIoTDevice().getName();
                long ioTDeviceOutput = 1L;
                var payload = ((MessageWithPayload<PayloadForIoTAgent>)message).getPayload();
                String MELName = payload.MELName;
                long osmesisEdgeletSize = 250; //250;
                long MELOutput = 70; //70;
                String vmName = "VM_"+((appID % 10)+1);
                long osmesisCloudletSize = 200; //200;
                OsmoticAppDescription app = new OsmoticAppDescription(appName, appID, DataRate, StopDataGenerationTime, ioTDeviceName, ioTDeviceOutput, MELName, osmesisEdgeletSize, MELOutput, vmName, osmesisCloudletSize, StartDataGenerationTime);
                int iotDeviceID = getIoTDevice().getId();
                app.setIoTDeviceId(iotDeviceID);
                getIoTDevice().schedule(OsmoticBroker.brokerID, 0.0, OsmoticTags.GENERATE_OSMESIS_WITH_RESOLUTION, app);
            } else {
                // The device is moving but not communicating
                getIoTDevice().schedule(getIoTDevice().getId(), MainEventManager.clock(), MOVING, null);
                getIoTDevice().schedule(OsmoticBroker.brokerID, MainEventManager.clock()+OsmoticBroker.getDeltaVehUpdate(), MAPE_WAKEUP_FOR_COMMUNICATION, null);
            }
        }


    }
}
