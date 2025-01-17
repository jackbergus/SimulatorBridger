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
import uk.ncl.giacomobergami.SumoOsmosisBridger.SimulatorManager;
import uk.ncl.giacomobergami.SumoOsmosisBridger.meap.messages.PayloadForIoTAgent;
import uk.ncl.giacomobergami.SumoOsmosisBridger.meap.messages.MessageWithPayload;
import uk.ncl.giacomobergami.components.iot.IoTEntityGenerator;
import uk.ncl.giacomobergami.utils.data.YAML;
import uk.ncl.giacomobergami.utils.gir.SquaredCartesianDistanceFunction;
import uk.ncl.giacomobergami.utils.pipeline_confs.TrafficConfiguration;
import uk.ncl.giacomobergami.utils.shared_data.edge.Edge;
import uk.ncl.giacomobergami.utils.structures.ImmutablePair;

import java.io.File;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.*;
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
    private static final File converter_file = new File("clean_example/converter.yaml");
    private static final Optional<TrafficConfiguration> time_conf = YAML.parse(TrafficConfiguration.class, converter_file);
    List<String> mels = new ArrayList<>();
    HashMap<String, Integer> timesPerMel = new HashMap<>();
    HashMap<String, Integer> timesPerEdgeNetwork = new HashMap<>();

    double endSUMO = SimulatorManager.getSimulationEnd();
    DecimalFormat df = new DecimalFormat("#.###");

    @Override
    public void monitor() {

        super.monitor();
        if (ls != null) ls.clear();

        // TODO: doing this if and only if the device is going to communicate with the device now

        // Monitoring the neighbouring nodes
        var iot = getIoTDevice();
        HashSet<Double> setWUT = IoTEntityGenerator.getSetWUT();
        double currentTime = (double) Math.round(MainEventManager.clock() * 1000) /1000;
        // Returning if the agent, at this current time, is not scheduled for transmission
        if (!iot.transmit || !setWUT.contains(currentTime) ||currentTime > endSUMO) return;
        HashMap<String, String> edgeToMelList = new HashMap<>();
        HashMap<String, Integer> thisLoop = new HashMap<>();
        ls = AgentBroker
                .getInstance()
                .getOsmoticDataCentersStream()
                .filter(x -> (x instanceof EdgeDataCenter))
                .flatMap(x->x.getSdnhosts().stream().map(y -> new ImmutablePair<>(x, y.getHost())))
                .filter(x -> {
                    if (!(x.getRight() instanceof EdgeDevice obj)) return false;
                    edgeToMelList.put(obj.getDeviceName(), x.getLeft().getName());
                    timesPerEdgeNetwork.putIfAbsent(x.getLeft().getName(), 0);
                    timesPerMel.putIfAbsent(obj.getDeviceName(), 0);
                    thisLoop.putIfAbsent(obj.getDeviceName(), 0);
                    double distance = Math.sqrt(f.getDistance(iot, obj.location));
                    boolean outcome = ((distance <= iot.mobility.signalRange) && (distance <= obj.signalRange));
                    return outcome;
                })
                .map(x -> new ImmutablePair<>(((EdgeDataCenter)x.getLeft()), ((EdgeDevice) x.getRight())))
                .collect(Collectors.toList());

        if (Objects.equals(time_conf.get().getMelProcessing(), "Quietest") && time_conf.get().getRoutingAlgorithm().contains("MCFR")) {

            List<String> initialMels = new ArrayList<>();
            for (ImmutablePair<EdgeDataCenter, EdgeDevice> l : ls) {
                initialMels.add(l.getRight().getDeviceName());
            }

            List<String> actNetwork = new ArrayList<>();
            for (String network : timesPerEdgeNetwork.keySet()) {
                if (timesPerEdgeNetwork.get(network) == (int) Collections.min(timesPerEdgeNetwork.values())) {
                    actNetwork.add(network);
                }
            }

            HashMap<String, Integer> quietestMels = new HashMap<>();
            for (String chosenMels : edgeToMelList.keySet()) {
                if (actNetwork.contains(edgeToMelList.get(chosenMels))) {
                    quietestMels.put(chosenMels, timesPerMel.get(chosenMels));
                }
            }

            for (String mel : quietestMels.keySet()) {
                if (quietestMels.get(mel) == (int) Collections.min(quietestMels.values()) && !mels.contains("@" + mel)) {
                    mels.add("@" + mel);
                    timesPerMel.put(mel, timesPerMel.get(mel) + 1);
                    thisLoop.put(mel, thisLoop.get(mel) + 1);
                    if (mels.size() == ls.size()) break;
                }
            }

            ls = AgentBroker
                    .getInstance()
                    .getOsmoticDataCentersStream()
                    .filter(x -> (x instanceof EdgeDataCenter))
                    .flatMap(x -> x.getSdnhosts().stream().map(y -> new ImmutablePair<>(x, y.getHost())))
                    .filter(x -> {
                        if (!(x.getRight() instanceof EdgeDevice)) return false;
                        return mels.contains("@" + ((EdgeDevice) x.getValue()).getDeviceName());
                    })
                    .map(x -> new ImmutablePair<>(((EdgeDataCenter) x.getLeft()), ((EdgeDevice) x.getRight())))
                    .collect(Collectors.toList());

            for (String mel : timesPerMel.keySet()) {
                timesPerEdgeNetwork.put(edgeToMelList.get(mel), timesPerEdgeNetwork.get(edgeToMelList.get(mel)) + thisLoop.get(mel));
            }
            mels.clear();
        }
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
                df.setRoundingMode(RoundingMode.HALF_UP);
                getIoTDevice().transmit = true;
                var message = nearest.get();
                double StartDataGenerationTime = Double.parseDouble(df.format(MainEventManager.clock()));
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
                if(MainEventManager.clock() + OsmoticBroker.getDeltaVehUpdate() > endSUMO) {
                    getIoTDevice().schedule(OsmoticBroker.brokerID, MainEventManager.clock() + OsmoticBroker.getDeltaVehUpdate(), MAPE_WAKEUP_FOR_COMMUNICATION, null);
                }
            }
        }


    }
}
