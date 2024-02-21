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
import uk.ncl.giacomobergami.components.sdn_routing.MaximumFlowRoutingPolicy;
import uk.ncl.giacomobergami.traffic_orchestrator.solver.MinCostMaxFlow;
import uk.ncl.giacomobergami.utils.gir.CartesianPoint;
import uk.ncl.giacomobergami.utils.gir.SquaredCartesianDistanceFunction;
import uk.ncl.giacomobergami.utils.structures.ImmutablePair;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class AbstractNetworkAgent extends AbstractAgent {
    private final AbstractAgent actualAgent;
    private final MaximumFlowRoutingPolicy routing;

    /**
     *
     * @param actualAgent   Setting the actual agent which is going to receive the messages,
     *                      so the messages can be "drained" from him.
     */
    public AbstractNetworkAgent(AbstractAgent actualAgent) {
        this.actualAgent = actualAgent;
        routing = new MaximumFlowRoutingPolicy();
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

    public static String replaceLast(String string, String toReplace, String replacement) {
        int pos = string.lastIndexOf(toReplace);
        if (pos > -1) {
            return string.substring(0, pos)
                    + replacement
                    + string.substring(pos + toReplace.length());
        } else {
            return string;
        }
    }

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
        messagesFromIoTDevices.clear();
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
                                var payload = new PayloadForIoTAgent("@"+y.getDeviceName(), y.getX(), y.getY());
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
//                System.out.println("This run:");
                String iot_prefix = "iot_";
                String element_with_separator = "@";
                AtomicInteger id_generator = new AtomicInteger(0);
                List<String> id_to_name = new ArrayList<>();
                Map<String, Integer> name_to_id = new HashMap<>();
                Map<String, List<String>> paths = new HashMap<>();
                HashMultimap<String, List<String>> paths_for_network = HashMultimap.create();

                int bogusSrc = id_generator.getAndIncrement();
                int bogusDst = id_generator.getAndIncrement();
                id_to_name.add(null);
                id_to_name.add(null);
                Set<String> iotNames = new HashSet<>();

                int niot = 0;
                HashMap<String, EdgeDataCenter> networks = new HashMap<>();
                HashMap<String, NetworkNodeType> nodeType = new HashMap<>();

                for (var cp : payloadMap.asMap().entrySet()) {
                    var name = iot_prefix+cp.getKey();
//                    System.out.println(iot_prefix+cp.getKey());
                    name_to_id.put(name, id_generator.getAndIncrement());
                    id_to_name.add(name);
                    iotNames.add(name);
                    niot++;
                    for (var msgPayload : cp.getValue()) {
                        if (!msgPayload.sender.getName().equals(cp.getKey()))
                            throw new RuntimeException("ERROR: IoT senders do not match!");
                        for (var edgeCandidate : msgPayload.candidates) {
                            var edgeDataCenter = edgeCandidate.getLeft();
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
                        var actualSrcDisambiguatedName = edge.src().getName()+element_with_separator+net.getKey();
                        var actualDstDisambiguatedName = edge.dst().getName()+element_with_separator+net.getKey();
                        name_to_id.computeIfAbsent(actualSrcDisambiguatedName, k -> {
                            id_to_name.add(null); // Doppelgänger has no name
                            id_to_name.add(k);
                            nodeType.put(k, src);
                            id_generator.incrementAndGet();
                            return id_generator.getAndIncrement(); // the previous is the doppelgänger giving acces to the current node
                        });
                        name_to_id.computeIfAbsent(actualDstDisambiguatedName, k -> {
                            id_to_name.add(null); // Doppelgänger has no name
                            id_to_name.add(k);
                            nodeType.put(k, dst);
                            id_generator.incrementAndGet();
                            return id_generator.getAndIncrement(); // the previous is the doppelgänger giving acces to the current node
                        });
                    }
                }

                int N = id_generator.get();
                var graph = MinCostMaxFlow.createGraph(N);
                int cost[][] = new int[N][N];
                for (var array: cost) Arrays.fill(array, 0);
                int cap[][] = new int[N][N];
                for (var array: cap) Arrays.fill(array, 0);

                // After counting how many nodes are there, now we can actually create the network!
                for (var cp : payloadMap.asMap().entrySet()) {
                    var iot = iot_prefix+cp.getKey();
                    var iot_id = name_to_id.get(iot);
                    cost[bogusSrc][iot_id] = 1;
                    cap[bogusSrc][iot_id] = 1;
                    for (var msgPayload : cp.getValue()) {
                        for (var edgeCandidate : msgPayload.candidates) {
                            var edgeDataCenter = edgeCandidate.getLeft();
                            var edgeNode = edgeCandidate.getRight();
                            var edgeName = edgeNode.getDeviceName()+element_with_separator+edgeDataCenter.getNet().name;
                            var edgeId = name_to_id.get(edgeName);
                            if (edgeId == null)
                                throw new RuntimeException("ERROR:" +edgeName+" is not associated to an id!");
                            var doppelGangerName = id_to_name.get(edgeId-1);
                            if (doppelGangerName != null)
                                throw new RuntimeException(doppelGangerName);
                            if (!Objects.equals(id_to_name.get(edgeId),edgeName))
                                throw new RuntimeException(name_to_id.get(edgeId)+" for "+edgeId+" != "+edgeName);
                            cost[iot_id][edgeId-1] = (int)Math.round(Math.sqrt(f.getDistance(devices.get(cp.getKey()),  edgeNode.location)) * 100.0);
                            cap[iot_id][edgeId-1] = 1;
                            cost[edgeId-1][edgeId] = 1;
                            cap[edgeId-1][edgeId] = (int)edgeNode.max_vehicle_communication;
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
                        var actualSrcDisambiguatedName = edge.src().getName()+element_with_separator+net.getKey();
                        var actualDstDisambiguatedName = edge.dst().getName()+element_with_separator+net.getKey();
                        var srcId = name_to_id.get(actualSrcDisambiguatedName);
                        var dstId = name_to_id.get(actualDstDisambiguatedName);
                        if ((src.index() == dst.index()) && (src.getT() == NetworkNodeType.type.Host)) {
                            var srcHost = src.getVal2().getHost();
                            if (!(srcHost instanceof EdgeDevice))
                                throw new RuntimeException("ERROR on src host: this supports only edge hosts! " + srcHost);
                            var dstHost = dst.getVal2().getHost();
                            if (!(dstHost instanceof EdgeDevice))
                                throw new RuntimeException("ERROR on dst host: this supports only edge hotsts! "+ dstHost);
                            cap[srcId][dstId] = Math.min((int)((EdgeDevice)srcHost).max_vehicle_communication,
                                                          (int)((EdgeDevice)dstHost).max_vehicle_communication);
                            cost[srcId][dstId] = (int)Math.round(Math.sqrt(f.getDistance(((EdgeDevice)srcHost),((EdgeDevice)dstHost))) * 100.0);
                        } else {
                            cap[srcId][dstId] = niot;
                            cost[srcId][dstId] = 1;
                        }
                    }
                    var gateway = actualNetwork.getGateway().getName()+element_with_separator+net.getKey();
                    var gatewayId = name_to_id.get(gateway);
                    if (gatewayId == null)
                        throw new RuntimeException("ERROR: unresolved gateway " + gateway);
                    cap[gatewayId][bogusDst] = niot;
                    cost[gatewayId][bogusDst] = 1;
                }

                // Now, we can run the pathing algorithm
                MinCostMaxFlow algorithm = new MinCostMaxFlow();
                var result = algorithm.getMaxFlow(cap, cost, bogusSrc, bogusDst);
                Set<String> computedIoTPaths = new HashSet<>();
                HashMultimap<String, List<String>> obtainedPaths = HashMultimap.create();

                for (var p : result.minedPaths) {
                    var calculated_path = p.stream().map(id_to_name::get).filter(Objects::nonNull).collect(Collectors.toList());
//                    System.out.println(calculated_path);
                    computedIoTPaths.add(calculated_path.get(0));
                    paths.put(calculated_path.get(0), calculated_path);
                    obtainedPaths.put(calculated_path.get(1), calculated_path);
                }
                if (result.minedPaths.size() != niot) {
                    if (result.minedPaths.size() > niot) {
                        throw new RuntimeException("We are expecting the opposite, that the mined paths are less than the expected ones");
                    }
                    for (var v : devices.entrySet()) {
                        if (computedIoTPaths.contains(iot_prefix + v.getKey())) continue; // I am not re-computing the paths that were computed before
                        var iotDeviceId = name_to_id.get(iot_prefix + v.getKey());
                        algorithm.bellman_ford_moore(iotDeviceId);
                        var p = algorithm.map.get(new ImmutablePair<>(iotDeviceId, bogusDst));
                        if (p == null) {
                            throw new RuntimeException("There should always be a path for the device towards the bogus destination! " + iot_prefix + v.getKey()+ " with id  "+ iotDeviceId);
                        }
                        var pp = p.stream().map(id_to_name::get).collect(Collectors.toList());
                        paths.put(iot_prefix + v.getKey(), pp);
                    }
                    if ((paths.size() != niot)) {
                        throw new RuntimeException("That should have fixed the problem! " + paths.size()+ " vs "+ niot);
                    }
                }

                HashMultimap<String, String> hm = HashMultimap.create();

                for (var solutions : paths.entrySet()) {
                    var iotName = solutions.getKey().substring(iot_prefix.length());
                    var iotPath = solutions.getValue();
                    var candidate = iotPath.remove(0).substring(iot_prefix.length());
                    iotPath.removeIf(iotNames::contains);
                    if (!candidate.equals(iotName))
                        throw new RuntimeException("ERROR: IoT does not match");
                    String network;
                    int i = 0, substring_starts_at = 0;

                    {
                        int j = 0;
                        int min=Integer.MAX_VALUE;
                        String[] array = new String[iotPath.size()];
                        iotPath = iotPath.stream().filter(Objects::nonNull).collect(Collectors.toList());

                        //reversing the strings and finding the length of smallest string
                        for(i=0;i<(iotPath.size());i++)  {
                            if(iotPath.get(i) != null) {
                                if (iotPath.get(i).length() < min)
                                    min = iotPath.get(i).length();
                                StringBuilder input1 = new StringBuilder();
                                input1.append(iotPath.get(i));
                                array[i] = input1.reverse().toString();
                            }
                        }

                        //finding the length of longest suffix
                        array = Arrays.stream(array).filter(Objects::nonNull).toArray(String[]::new);
                        for(i=0;i<min;i++) {
                            for(j=1;j<(array.length);j++)
                                if(array[j].charAt(i)!=array[j-1].charAt(i))
                                    break;
                            if(j!=array.length) break;
                        }
                    }

                    var tmp = nodeType.get(iotPath.get(0));
                    if (tmp == null)
                        throw new RuntimeException("Expected that the network contained the node " + iotPath.get(0));
                    var y = (CartesianPoint) nodeType.get(iotPath.get(0)).getVal2().getHost();
                    substring_starts_at = i;
                    network = iotPath.get(0).substring(iotPath.get(0).length()-substring_starts_at);
                    if (!network.startsWith(element_with_separator)) {
                        if (!network.contains(element_with_separator))
                            throw new RuntimeException("Error: we expect that the common suffix starts with @");
                        int atSign = network.lastIndexOf('@');
                        network = network.substring(atSign);
                        substring_starts_at = iotPath.get(0).length()-atSign;
                    }
                    network = network.substring(1);;
                    for (i = 0, N = iotPath.size(); i<N; i++) {
                        iotPath.set(i,  replaceLast(iotPath.get(i), "@"+network, ""));
                    }
                    var iotConnectToMel = "@"+iotPath.get(0); // Correct specification, determining the precise host
                    //iotPath.get(0)+".*"; Old, incorrect specification

                    // Setting the path to the IoT Device
                    paths_for_network.put(network, iotPath);

                    // Sending the IoT device who should they contact!
                    var payload = new PayloadForIoTAgent(iotConnectToMel, y.getX(), y.getY());
                    var message = new MessageWithPayload<PayloadForIoTAgent>();
                    hm.put(iotConnectToMel+"@"+network, iotName);
                    message.setSOURCE(getName());
                    message.setDESTINATION(Collections.singletonList(iotName));
                    message.setPayload(payload);
                    publishMessage(message);
                }

                for (var distinctPaths : paths_for_network.asMap().entrySet()) {
                    var network = distinctPaths.getKey();
                    var network_routing = networks.get(network).getSdnController().getSdnRoutingPoloicy();
                    if (network_routing instanceof MaximumFlowRoutingPolicy) {
                        // Updating the connections and the paths given the attempt to connections
                        ((MaximumFlowRoutingPolicy)network_routing).setNewPaths(distinctPaths.getValue());
                    }
                }

                hm.asMap().forEach((k,v)-> {
//                    System.out.println(k+"-->"+v.size());
                });
//                System.out.println("DEBUG");
            }
        }
    }
}
