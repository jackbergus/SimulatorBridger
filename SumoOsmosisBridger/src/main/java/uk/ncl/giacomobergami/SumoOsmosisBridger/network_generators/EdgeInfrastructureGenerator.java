package uk.ncl.giacomobergami.SumoOsmosisBridger.network_generators;

import com.fasterxml.jackson.annotation.JsonIgnore;
import uk.ncl.giacomobergami.components.loader.SubNetworkConfiguration;
import uk.ncl.giacomobergami.components.networking.*;
import uk.ncl.giacomobergami.utils.annotations.Input;
import uk.ncl.giacomobergami.utils.annotations.Output;
import uk.ncl.giacomobergami.utils.shared_data.edge.TimedEdge;
import uk.ncl.giacomobergami.utils.structures.StraightforwardAdjacencyList;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class EdgeInfrastructureGenerator {

    private static AtomicInteger edgeDevice = new AtomicInteger(1);
    private static AtomicInteger vm = new AtomicInteger(1);
    private static AtomicInteger edgeSwitch = new AtomicInteger(1);

    public static String edgeDeviceId(int id) {
        return "edgeDevice_"+id;
    }
    public static String edgeId(int id) {
        return "edge"+id;
    }
    public static String coreId(int id) {
        return "core"+id;
    }
    public static String melId(String network_name, int id) {
        return "MEL_"+network_name+"."+id;
    }

    public static Switch generateEdgeSwitch(int id, long mips) {
        return new Switch("edge", edgeId(id), mips);
    }

    public static Switch generateCoreSwitch(int id, double mips) {
        return new Switch("core", coreId(id), (long) mips);
    }

    public static Host generateEdgeDevice(int id, double bw, int mips, int pes, int ram, long storage, double max_vehicle_communication) {
        return new Host(edgeDeviceId(id),  pes, ram,  bw, storage, mips, 0, 0, 0, 0, max_vehicle_communication);
    }
    public static List<Host> generateDistinctEdgeDevices(int n, double bandwidth, int mips, int pes, int ram, long storage, double max_vehicle_communication) {
        return IntStream.range(1, n+1).mapToObj(x-> generateEdgeDevice(edgeDevice.getAndIncrement(), bandwidth, mips, pes, ram, storage, max_vehicle_communication)).collect(Collectors.toList());
    }
    public static VM generateMEL(String network_name, int id, double bandwidth, String policy, double mips, int pes, int ram, long storage) {
        return new VM(melId(network_name, id), bandwidth, mips, ram, pes, policy, storage);
    }
    public static List<VM> generateVMs(String network_name, int n, double bandwidth, String policy, double mips, int pes, int ram, long storage) {
        return IntStream.range(1, n+1).mapToObj(x-> generateMEL(network_name, vm.getAndIncrement(), bandwidth, policy, mips, pes, ram, storage)).collect(Collectors.toList());
    }

    public static class Configuration {
        public boolean movingEdges;
        public String edge_network_name;
        public String gateway_name;
        public long gateway_iops;
        public double reset_max_vehicle_communication;

        public int n_edgeDevices_and_edges;
        public double edge_device_to_edge_bw;
        public int edge_switch_iops;
        public double between_edge_bw;

        public int n_core;
        public int n_edges_to_one_core;
        public double edge_to_core_bw;

        public double core_to_gateway_bw;

        public HostsAndVMs              hosts_and_vms;
        public DataCenterWithController network_configuration;

        @JsonIgnore
        StraightforwardAdjacencyList<String> edge_switch_network;

        @JsonIgnore
        public ArrayList<String> stringToInteger;

        public boolean getMovingEdges() {
            return movingEdges;
        }

        public Configuration copy() {
            Configuration result = new Configuration();
            result.movingEdges = movingEdges;
            result.edge_network_name = edge_network_name;
            result.gateway_name = gateway_name;
            result.gateway_iops = gateway_iops;
            result.n_edgeDevices_and_edges = n_edgeDevices_and_edges;
            result.edge_device_to_edge_bw = edge_device_to_edge_bw;
            result.edge_switch_iops = edge_switch_iops;
            result.between_edge_bw = between_edge_bw;
            result.n_core = n_core;
            result.n_edges_to_one_core = n_edges_to_one_core;
            result.edge_to_core_bw = edge_to_core_bw;
            result.core_to_gateway_bw = core_to_gateway_bw;
            result.hosts_and_vms = hosts_and_vms.copy();
            result.network_configuration = network_configuration.copy();
            result.edge_switch_network = new StraightforwardAdjacencyList<>();
            if (edge_switch_network != null)
            edge_switch_network.forEach((k, v)-> result.edge_switch_network.put(k, v));
            return result;
        }
    }

    public static SubNetworkConfiguration generate(@Input final Configuration conf,
                                                   @Output List<TopologyLink> result,
                                                   @Input boolean only_one_mel_per_edge_network,
                                                   @Input Function<String, TimedEdge> f) {
        List<Switch> switches = new ArrayList<>();
        conf.hosts_and_vms.validate();

        switches.add(new Switch("gateway", conf.gateway_name, conf.gateway_iops));
        var hosts = generateDistinctEdgeDevices(conf.n_edgeDevices_and_edges,
                conf.hosts_and_vms.hosts_bandwidth,
                conf.hosts_and_vms.hosts_mips,
                conf.hosts_and_vms.hosts_pes,
                conf.hosts_and_vms.hosts_ram,
                conf.hosts_and_vms.hosts_storage,
                conf.reset_max_vehicle_communication
        );
        if (hosts.size() != conf.stringToInteger.size())
            throw  new RuntimeException("ERROR!");
        for (int i = 0; i<hosts.size(); i++) {
            hosts.get(i).name = conf.stringToInteger.get(i);
            var el = f.apply(hosts.get(i).name);
            var dst = hosts.get(i);
            dst.x = el.x;
            dst.y = el.y;
            dst.signalRange = el.communication_radius;
        }

        if (conf.n_edges_to_one_core<= 0)
            throw new RuntimeException("ERROR");
        conf.n_core = conf.n_edgeDevices_and_edges % conf.n_edges_to_one_core;
        if (only_one_mel_per_edge_network)
            conf.hosts_and_vms.n_vm = 1;

        var vm = generateVMs(conf.edge_network_name, conf.hosts_and_vms.n_vm, conf.hosts_and_vms.vm_bw, conf.hosts_and_vms.vm_cloudletPolicy, conf.hosts_and_vms.vm_mips, conf.hosts_and_vms.vm_pes, conf.hosts_and_vms.vm_ram, conf.hosts_and_vms.vm_storage);
        for (int i = 1; i<=conf.n_edgeDevices_and_edges; i++) {
            var edgeDeviceID = hosts.get(i-1).name;
            switches.add(generateEdgeSwitch(edgeSwitch.getAndIncrement(), conf.edge_switch_iops));
            var edgeID = switches.get(i-1).name;
            result.add(new TopologyLink(conf.edge_network_name, edgeDeviceID, edgeID, conf.edge_device_to_edge_bw));
            var nCore = ((i-1) % conf.n_edges_to_one_core)+1;
            var nCoreID = coreId(nCore);
            result.add(new TopologyLink(conf.edge_network_name, edgeID, nCoreID, conf.edge_to_core_bw));
            if (((i-1) / conf.n_edges_to_one_core)<= 0) {
                switches.add(generateCoreSwitch(nCore, conf.edge_to_core_bw));
                result.add(new TopologyLink(conf.edge_network_name, nCoreID, conf.gateway_name, conf.core_to_gateway_bw));
            }
        };

        conf.edge_switch_network.forEach((src, dst) -> {
            result.add(new TopologyLink(conf.edge_network_name, src, dst, conf.between_edge_bw));
            result.add(new TopologyLink(conf.edge_network_name, dst, src, conf.between_edge_bw));
        });

        return new SubNetworkConfiguration(hosts, vm, switches, conf.network_configuration, conf.edge_network_name);
    }
}