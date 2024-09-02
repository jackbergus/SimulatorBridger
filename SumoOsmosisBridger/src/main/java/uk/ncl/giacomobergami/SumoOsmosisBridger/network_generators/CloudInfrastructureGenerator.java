package uk.ncl.giacomobergami.SumoOsmosisBridger.network_generators;

import com.google.common.collect.HashMultimap;
import uk.ncl.giacomobergami.components.loader.SubNetworkConfiguration;
import uk.ncl.giacomobergami.components.networking.*;
import uk.ncl.giacomobergami.utils.annotations.Input;
import uk.ncl.giacomobergami.utils.annotations.Output;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class CloudInfrastructureGenerator {
    private static AtomicInteger core = new AtomicInteger(1);
    private static AtomicInteger aggregate = new AtomicInteger(1);
    private static AtomicInteger edge = new AtomicInteger(1);
    private static AtomicInteger vm = new AtomicInteger(1);
    private static AtomicInteger host = new AtomicInteger(1);

    public static String coreId(int id) {
        return "core"+id;
    }
    public static String aggregateId(int id) {
        return "aggregate"+id;
    }
    public static String edgeId(int id) {
        return "edge"+id;
    }
    public static String vmId(int id) {
        return "VM_"+id;
    }
    public static String hostId(int id) {
        return "host"+id;
    }

    public static int numberOfGroups(int targetRow, int groupSize) {
        if (targetRow < groupSize) return  1;
        else if ((0 < groupSize) && (groupSize <= targetRow)) return 1 + (targetRow-groupSize);
        else return targetRow;
    }

    public static Switch generateCoreSwitch(int id, long mips) {
        return new Switch("core", coreId(id), mips);
    }
    public static List<Switch> generateDistinctCoreSwitches(int n, long mips) {
        return IntStream.range(1, n+1).mapToObj(x-> generateCoreSwitch(core.getAndIncrement(), mips)).collect(Collectors.toList());
    }
    public static Switch generateAggregateSwitch(int id, long mips) {
        return new Switch("aggregate", aggregateId(id), mips);
    }
    public static List<Switch> generateDistinctAggregateSwitches(int n, long mips) {
        return IntStream.range(1, n+1).mapToObj(x-> generateAggregateSwitch(aggregate.getAndIncrement(), mips)).collect(Collectors.toList());
    }
    public static Switch generateEdgeSwitch(int id, long mips) {
        return new Switch("edge", edgeId(id), mips);
    }
    public static List<Switch> generateDistinctEdgeSwitches(int n, long mips) {
        return IntStream.range(1, n+1).mapToObj(x-> generateEdgeSwitch(edge.getAndIncrement(), mips)).collect(Collectors.toList());
    }
    public static VM generateVirtualMachine(int id, int bandwidth, String policy, double mips, int pes, int ram, long storage) {
        return new VM(vmId(id), bandwidth, mips, ram, pes, policy, storage);
    }
    public static List<VM> generateDistinctVMs(int n, int bandwidth, String policy, double mips, int pes, int ram, long storage) {
        return IntStream.range(1, n+1).mapToObj(x-> generateVirtualMachine(vm.getAndIncrement(), bandwidth, policy, mips, pes, ram, storage)).collect(Collectors.toList());
    }
    public static Host generateHost(int id, int bw, int mips, int pes, int ram, long storage, double max_vehicle_communication) {
        return new Host(hostId(id),  pes, ram,  bw, storage, mips, 0, 0, 0, 0, max_vehicle_communication);
    }
    public static List<Host> generateDistinctHosts(int n, int bandwidth, int mips, int pes, int ram, long storage, double max_vehicle_communication) {
        return IntStream.range(1, n+1).mapToObj(x-> generateHost(host.getAndIncrement(), bandwidth, mips, pes, ram, storage, max_vehicle_communication)).collect(Collectors.toList());
    }

    public static class Configuration {
        public String cloud_network_name;
        public String gateway_name;
        public long gateway_iops;

        public int n_cores;
        public long cores_iops;
        public int gateway_to_core_bandwidth;

        public int n_aggregates;
        public long aggregates_iops;
        public int core_to_aggregate_bandwidth;

        public int n_edges;
        public int edges_iops;
        public int n_edges_group_size;
        public int aggregate_to_edge_bandwidth;
        public int IoTNumber;

        public HostsAndVMs              hosts_and_vms;
        public DataCenterWithController network_configuration;

        public Configuration copy() {
            Configuration result = new Configuration();
            result.cloud_network_name = cloud_network_name;
            result.gateway_name = gateway_name;
            result.gateway_iops = gateway_iops;
            result.n_cores = n_cores;
            result.cores_iops = cores_iops;
            result.gateway_to_core_bandwidth = gateway_to_core_bandwidth;
            result.n_aggregates = n_aggregates;
            result.aggregates_iops = aggregates_iops;
            result.core_to_aggregate_bandwidth = core_to_aggregate_bandwidth;
            result.n_edges = n_edges;
            result.edges_iops = edges_iops;
            result.n_edges_group_size = n_edges_group_size;
            result.aggregate_to_edge_bandwidth = aggregate_to_edge_bandwidth;
            result.IoTNumber = IoTNumber;
            result.hosts_and_vms = hosts_and_vms.copy();
            result.network_configuration = network_configuration.copy();
            return result;
        }
    }

    public static SubNetworkConfiguration generateFromConfiguration(@Input final Configuration conf,
                                                                    @Output List<TopologyLink> result) {
        List<Switch> switches = new ArrayList<>();
        conf.hosts_and_vms.validate();
        switches.add(new Switch("gateway", conf.gateway_name, conf.gateway_iops));

        // Gateway to cores
        var cores = generateDistinctCoreSwitches(conf.n_cores, conf.cores_iops);
        for (var core : cores) result.add(new TopologyLink(conf.cloud_network_name, conf.gateway_name, core.name, conf.gateway_to_core_bandwidth));
        switches.addAll(cores);
//        cores.clear();

        // Cores to aggregates
        var aggregates = generateDistinctAggregateSwitches(conf.n_aggregates, conf.aggregates_iops);
        int half_cores = conf.n_cores/2;
        int half_aggregates = conf.n_aggregates/2;
        for (int i = 1; i<=half_cores; i++) {
            for (int j = 0; (j<half_aggregates); j++) {
                int jId = j*2+1;
                if (jId <= conf.n_aggregates) {
                    result.add(new TopologyLink(conf.cloud_network_name,
                                                cores.get(i-1).name,
                                                aggregates.get(jId-1).name,
                                                conf.core_to_aggregate_bandwidth));
                }
            }
        }
        for (int i = half_cores; i<=conf.n_cores; i++) {
            for (int j = 1; (j<half_aggregates); j++) {
                int jId = j*2;
                if (jId <= conf.n_aggregates) {
                    result.add(new TopologyLink(conf.cloud_network_name,
                                                cores.get(i-1).name,
                                                aggregates.get(jId-1).name,
                                                conf.core_to_aggregate_bandwidth));
                }
            }
        }
        switches.addAll(aggregates);
//        aggregates.clear();

        // Aggregates to edges
        var edges = generateDistinctEdgeSwitches(conf.n_edges, conf.edges_iops);
        var nGroups = numberOfGroups(conf.n_edges, conf.n_edges_group_size);
        int splits;
        if (nGroups >= conf.n_aggregates) {
            splits = numberOfGroups(nGroups, conf.n_aggregates);
            for (int prev_i = 1; prev_i<=conf.n_aggregates; prev_i++) {
                int finalAssignJ = prev_i+splits-1+conf.n_edges_group_size-1;
                for (int assign_j = prev_i; assign_j<=finalAssignJ; assign_j++)
                    result.add(new TopologyLink(conf.cloud_network_name,
                                                aggregates.get(prev_i-1).name,
                                                edges.get(assign_j-1).name,
                                                conf.aggregate_to_edge_bandwidth));
            }
        } else {
            splits = numberOfGroups(conf.n_aggregates, nGroups);
            HashMultimap<Integer, Integer> m = HashMultimap.create();
            for (int i = 1; i<=nGroups; i++) {
                for (int prev_i = i; prev_i<i+splits; prev_i++) {
                    for (int next_j = i; next_j<i+conf.n_edges_group_size; next_j++) {
                        if (prev_i > aggregates.size())
                            throw new RuntimeException("ERROR!");
                        if (next_j > edges.size())
                            throw new RuntimeException("ERROR!");
                        m.put(prev_i, next_j);
                    }
                }
            }
            for (var x : m.asMap().entrySet()) {
                var prev_i = x.getKey();
                for (var assign_j : x.getValue()) {
                    result.add(new TopologyLink(conf.cloud_network_name,
                                                aggregates.get(prev_i-1).name,
                                                edges.get(assign_j-1).name,
                                                conf.aggregate_to_edge_bandwidth));
                }
            }
        }
        switches.addAll(edges);
//        edges.clear();

        var hosts = generateDistinctHosts(
                conf.hosts_and_vms.n_hosts_per_edges * conf.n_edges,
                conf.hosts_and_vms.hosts_bandwidth,
                conf.hosts_and_vms.hosts_mips,
                conf.hosts_and_vms.hosts_pes,
                conf.hosts_and_vms.hosts_ram,
                conf.hosts_and_vms.hosts_storage,
                0
        );
        for (int i = 1; i<conf.n_edges; i++) {
            for (int j = 1; j<=conf.hosts_and_vms.n_hosts_per_edges; j++) {
                result.add(new TopologyLink(conf.cloud_network_name,
                                            edges.get(i-1).name,
                                            hosts.get(conf.hosts_and_vms.n_hosts_per_edges*(i-1)+j).name,
                                            conf.aggregate_to_edge_bandwidth));
            }
        }

        var vm = generateDistinctVMs(conf.hosts_and_vms.n_vm, conf.hosts_and_vms.vm_bw, conf.hosts_and_vms.vm_cloudletPolicy, conf.hosts_and_vms.vm_mips, conf.hosts_and_vms.vm_pes, conf.hosts_and_vms.vm_ram, conf.hosts_and_vms.vm_storage);
        return new SubNetworkConfiguration(hosts, vm, switches, conf.network_configuration, conf.cloud_network_name);

    }
}
