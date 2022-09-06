package uk.ncl.giacomobergami.SumoOsmosisBridger.meap.agents;

import com.google.common.collect.HashMultimap;
import org.cloudbus.agent.AbstractAgent;
import org.cloudbus.cloudsim.edge.core.edge.EdgeDataCenter;
import org.cloudbus.cloudsim.edge.core.edge.EdgeDevice;
import org.cloudbus.osmosis.core.NetworkNodeType;
import uk.ncl.giacomobergami.SumoOsmosisBridger.meap.messages.MessageWithPayload;
import uk.ncl.giacomobergami.SumoOsmosisBridger.meap.messages.PayloadForIoTAgent;
import uk.ncl.giacomobergami.SumoOsmosisBridger.meap.messages.PayloadFromIoTAgent;
import uk.ncl.giacomobergami.components.iot.IoTDevice;
import uk.ncl.giacomobergami.traffic_orchestrator.solver.MinCostMaxFlow;
import uk.ncl.giacomobergami.utils.gir.SquaredCartesianDistanceFunction;
import uk.ncl.giacomobergami.utils.structures.ImmutablePair;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class AbstractNetworkAgent extends AbstractAgent {
    private final AbstractAgent actualAgent;

    /**
     *
     * @param actualAgent   Setting the actual agent which is going to receive the messages,
     *                      so the messages can be "drained" from him.
     */
    public AbstractNetworkAgent(AbstractAgent actualAgent) {
        this.actualAgent = actualAgent;
    }
    private static final SquaredCartesianDistanceFunction f = SquaredCartesianDistanceFunction.getInstance();

    private AbstractNetworkAgentPolicy policy;
    public AbstractNetworkAgentPolicy getPolicy() { return policy; }
    public void setPolicy(AbstractNetworkAgentPolicy policy) { this.policy = policy; }

    @Override
    public void monitor() {}
    @Override
    public void analyze() {}
    @Override
    public void execute() {}

    @Override
    public void plan() {
        var messagesFromIoTDevices = actualAgent.getReceivedMessages(x -> ((MessageWithPayload<PayloadFromIoTAgent>)x).getPayload());
        if (messagesFromIoTDevices.isEmpty()) return;
        HashMap<String, IoTDevice> devices = new HashMap<>();
        HashMultimap<String, PayloadFromIoTAgent> payloadMap = HashMultimap.create();
        for (var x : messagesFromIoTDevices) {
            payloadMap.put(x.sender.getName(), x);
            devices.put(x.sender.getName(), x.sender);
        }
        switch (policy) {

            case GreedyNearest -> {
                // For each node that we might have, selecting
                for (var x : payloadMap.asMap().entrySet()) {
                    var dst = x.getKey();
                    var dev = devices.get(dst);
                    x.getValue()
                            .stream()
                            .flatMap(y->y.candidates.stream().map(ImmutablePair::getRight))
                            .min(Comparator.comparingDouble(o -> f.getDistance(dev, o.location)))
                            .ifPresent(y-> {
                                var payload = new PayloadForIoTAgent(y.getDeviceName()+".*", y.getX(), y.getY());
                                var message = new MessageWithPayload<PayloadForIoTAgent>();
                                message.setSOURCE(getName());
                                message.setDESTINATION(Collections.singletonList(dst));
                                message.setPayload(payload);
                                publishMessage(message);
                            });
                }
                // break;
            }

            case GreedyProposal -> {
                // Greedy algorithm for determinign the node to communicate with
                // Potentially, also changing the pathing algorithm
            }

            case OptimalMinCostFlux -> {
                AtomicInteger id_generator = new AtomicInteger(0);
                List<String> id_to_name = new ArrayList<>();
                Map<String, Integer> name_to_id = new HashMap<>();
                Map<String, List<String>> paths = new HashMap<>();

                int bogusSrc = id_generator.getAndIncrement();
                int bogusDst = id_generator.getAndIncrement();
                id_to_name.add(null);
                id_to_name.add(null);

                int niot = 0;
                HashMap<String, EdgeDataCenter> networks = new HashMap<>();
                HashMap<String, NetworkNodeType> nodeType = new HashMap<>();

                for (var cp : payloadMap.asMap().entrySet()) {
                    name_to_id.put("iot_"+cp.getKey(), id_generator.getAndIncrement());
                    id_to_name.add("iot_"+cp.getKey());
                    niot++;
                    for (var msgPayload : cp.getValue()) {
                        if (!msgPayload.sender.getName().equals(cp.getKey()))
                            throw new RuntimeException("ERROR: IoT senders do not match!");
                        for (var edgeCandidate : msgPayload.candidates) {
                            var edgeDataCenter = edgeCandidate.getLeft();
                            var edgeNode = edgeCandidate.getRight();
                            networks.putIfAbsent(edgeDataCenter.getNet().name, edgeDataCenter);
                        }
                    }
                }

                for (var net : networks.entrySet()) {
                    var actualNetwork = net.getValue();
                    for (var edge : actualNetwork.getTopology().getAllLinks()) {
                        var src = actualNetwork.resolveNode(edge.src());
                        if (src == null)
                            throw new RuntimeException("Unresolved node: "+edge.src());
                        var dst = actualNetwork.resolveNode(edge.dst());
                        if (dst == null)
                            throw new RuntimeException("Unresolved node: "+edge.dst());
                        // A disambiguated name contains the nome name as well as its network's name
                        var actualSrcDisambiguatedName = edge.src().getName()+"@"+net.getKey();
                        var actualDstDisambiguatedName = edge.dst().getName()+"@"+net.getKey();
                        name_to_id.computeIfAbsent(actualSrcDisambiguatedName, k -> {
                            id_to_name.add(k);
                            nodeType.put(k, src);
                            return id_generator.getAndIncrement();
                        });
                        name_to_id.computeIfAbsent(actualDstDisambiguatedName, k -> {
                            id_to_name.add(k);
                            nodeType.put(k, dst);
                            return id_generator.getAndIncrement();
                        });
                    }
                }

                int N = id_generator.get();
                double cost[][] = new double[N][N];
                for (var array: cost) Arrays.fill(array, 0);
                int flow[][] = new int[N][N];
                for (var array: flow) Arrays.fill(array, 0);

                // After counting how many nodes are there, now we can actually create the network!
                for (var cp : payloadMap.asMap().entrySet()) {
                    var iot = "iot_"+cp.getKey();
                    var iot_id = name_to_id.get(iot);
                    cost[bogusSrc][iot_id] = 0;
                    flow[bogusSrc][iot_id] = 1;
                    for (var msgPayload : cp.getValue()) {
                        for (var edgeCandidate : msgPayload.candidates) {
                            var edgeDataCenter = edgeCandidate.getLeft();
                            var edgeNode = edgeCandidate.getRight();
                            var edgeName = edgeNode.getDeviceName()+"@"+edgeDataCenter.getNet().name;
                            var edgeId = name_to_id.get(edgeName);
                            if (edgeId == null)
                                throw new RuntimeException("ERROR:" +edgeName+" is not associated to an id!");

                            cost[iot_id][edgeId] = Math.sqrt(f.getDistance(devices.get(cp.getKey()),  edgeNode.location));
                            flow[iot_id][edgeId] = 1;
                        }
                    }

                }

                for (var net : networks.entrySet()) {
                    var actualNetwork = net.getValue();
                    for (var edge : actualNetwork.getTopology().getAllLinks()) {
                        var src = actualNetwork.resolveNode(edge.src());
                        if (src == null)
                            throw new RuntimeException("Unresolved node: "+edge.src());
                        var dst = actualNetwork.resolveNode(edge.dst());
                        if (dst == null)
                            throw new RuntimeException("Unresolved node: "+edge.dst());
                        // A disambiguated name contains the nome name as well as its network's name
                        var actualSrcDisambiguatedName = edge.src().getName()+"@"+net.getKey();
                        var actualDstDisambiguatedName = edge.dst().getName()+"@"+net.getKey();
                        var srcId = name_to_id.get(actualSrcDisambiguatedName);
                        var dstId = name_to_id.get(actualDstDisambiguatedName);
                        if ((src.index() == dst.index()) && (src.getT() == NetworkNodeType.type.Host)) {
                            var srcHost = src.getVal2().getHost();
                            if (!(srcHost instanceof EdgeDevice))
                                throw new RuntimeException("ERROR on src host: this supports only edge hosts! " + srcHost);
                            var dstHost = dst.getVal2().getHost();
                            if (!(dstHost instanceof EdgeDevice))
                                throw new RuntimeException("ERROR on dst host: this supports only edge hotsts! "+ dstHost);
                            flow[srcId][dstId] = Math.min((int)((EdgeDevice)srcHost).max_vehicle_communication,
                                                          (int)((EdgeDevice)dstHost).max_vehicle_communication);
                            cost[srcId][dstId] = Math.sqrt(f.getDistance(((EdgeDevice)srcHost),((EdgeDevice)dstHost)));
                        } else {
                            flow[srcId][dstId] = niot;
                            cost[srcId][dstId] = 1;
                        }
                    }
                    var gateway = actualNetwork.getGateway().getName()+"@"+net.getKey();
                    var gatewayId = name_to_id.get(gateway);
                    if (gatewayId == null)
                        throw new RuntimeException("ERROR: unresolved gateway " + gateway);
                    flow[gatewayId][bogusDst] = niot;
                    cost[gatewayId][bogusDst] = 1;
                }

                // Now, we can run the pathing algorithm
                MinCostMaxFlow algorithm = new MinCostMaxFlow();
                var result = algorithm.getMaxFlow(flow, cost, bogusSrc, bogusDst);
                Set<String> computedIoTPaths = new HashSet<>();

                for (var p : result.minedPaths) {
                    var calculated_path = p.stream().map(id_to_name::get).collect(Collectors.toList());
                    computedIoTPaths.add(calculated_path.get(1));
                    paths.put(calculated_path.get(1), calculated_path);

                    // TODO: update the path within the network, the the SDNController is the suitable one!

//                    var payload = new PayloadForIoTAgent(y.getDeviceName()+".*", y.getX(), y.getY());
//                    var message = new MessageWithPayload<PayloadForIoTAgent>();
//                    message.setSOURCE(getName());
//                    message.setDESTINATION(Collections.singletonList(dst));
//                    message.setPayload(payload);
//                    publishMessage(message);
                }
                if (result.minedPaths.size() != niot) {
                    if (result.minedPaths.size() > niot) {
                        throw new RuntimeException("We are expecting the opposite, that the mined paths are less than the expected ones");
                    }
                    for (var v : devices.entrySet()) {
                        if (computedIoTPaths.contains(v.getKey())) continue; // I am not re-computing the paths that were computed before
                        var iotDeviceId = name_to_id.get(v.getKey());
                        algorithm.bellman_ford_moore(iotDeviceId);
                        var p = algorithm.map.get(new ImmutablePair<>(iotDeviceId, bogusDst));
                        if (p == null) {
                            throw new RuntimeException("There should always be a path for the device towards the bogus destination! " + v.getKey()+ " with id  "+ iotDeviceId);
                        }
                        var pp = p.stream().map(id_to_name::get).collect(Collectors.toList());
                        paths.put(v.getKey(), pp);
                    }
                    if ((paths.size() != niot)) {
                        throw new RuntimeException("That should have fixed the problem! " + paths.size()+ " vs "+ niot);
                    }
                }

                System.out.println(paths);
            }
        }
    }


}