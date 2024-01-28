package uk.ncl.giacomobergami.SumoOsmosisBridger.network_generators;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jooq.DSLContext;
import uk.ncl.giacomobergami.SumoOsmosisBridger.network_generators.from_traffic_data.EdgeNetworksGenerator;
import uk.ncl.giacomobergami.components.OsmoticRunner;
import uk.ncl.giacomobergami.components.iot.IoTEntityGenerator;
import uk.ncl.giacomobergami.SumoOsmosisBridger.network_generators.from_traffic_data.TimeTicker;
import uk.ncl.giacomobergami.components.iot.IoTDeviceTabularConfiguration;
import uk.ncl.giacomobergami.components.loader.GlobalConfigurationSettings;
import uk.ncl.giacomobergami.components.loader.SubNetworkConfiguration;
import uk.ncl.giacomobergami.components.networking.TopologyLink;
import uk.ncl.giacomobergami.utils.annotations.Input;
import uk.ncl.giacomobergami.utils.annotations.Output;
import uk.ncl.giacomobergami.utils.asthmatic.WorkloadCSV;
import uk.ncl.giacomobergami.utils.data.YAML;
import uk.ncl.giacomobergami.utils.pipeline_confs.TrafficConfiguration;
import uk.ncl.giacomobergami.utils.shared_data.edge.TimedEdge;
import uk.ncl.giacomobergami.utils.structures.ImmutablePair;

import javax.sql.DataSource;
import java.io.File;
import java.sql.Connection;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

import static uk.ncl.giacomobergami.utils.database.JavaPostGres.*;

public class EnsembleConfigurations {

    private final IoTEntityGenerator ioTEntityGenerator;
    private final EdgeNetworksGenerator edgeNetworkGenerator;
    private final CloudInfrastructureGenerator.Configuration cloud;
    private final  EdgeInfrastructureGenerator.Configuration edge;
    private final WANInfrastructureGenerator.Configuration wan_conf;
    private final static Logger logger = LogManager.getRootLogger();

    public enum MEL_APP_POLICY {
        ANY_MEL,
        NETWORK_MEL,
        THAT_MEL
    }

    public EnsembleConfigurations(IoTEntityGenerator ioTEntityGenerator,
                                  EdgeNetworksGenerator edgeNetworkGenerator,
                                  CloudInfrastructureGenerator.Configuration cloud,
                                  EdgeInfrastructureGenerator.Configuration edge,
                                  WANInfrastructureGenerator.Configuration wan_conf) {
        this.ioTEntityGenerator = ioTEntityGenerator;
        this.edgeNetworkGenerator = edgeNetworkGenerator;

        this.cloud = cloud;
        this.edge = edge;
        this.wan_conf = wan_conf;
    }

    /**
     * Generate the cloud configuration given the parametric settings
     *
     * @param numberOfClouds            Number of total cloud processing infrastructures
     * @param IoTMultiplicityForVMs     Number of VM per IoT device
     * @param nSCC                      The forecasted number of edge elements
     * @param globalCloud               Global configuration shared among all of the subnetworks
     * @return     Cloud configurations
     */
    private List<CloudInfrastructureGenerator.Configuration> setCloudPolicyFromIoTNumbers(int numberOfClouds,
                                                                                         int IoTMultiplicityForVMs,
                                                                                         int nSCC,
                                                                                         CloudInfrastructureGenerator.Configuration globalCloud, DSLContext context) {
        List<CloudInfrastructureGenerator.Configuration> ls = new ArrayList<>();
        int IoTNumber = ioTEntityGenerator.maximumNumberOfCommunicatingVehicles(context);

        // Assuming to set VM in the number of IoTNumber * IoTMultiplicityForVMs / numberOfClouds
        globalCloud.hosts_and_vms.n_vm = IoTNumber * IoTMultiplicityForVMs / numberOfClouds;

        // Assuming that the pes gives an idea of the instantiation, therefore from this we scale dound the hosts
        int HostsNumber = (int)Math.ceil(((double)globalCloud.hosts_and_vms.n_vm) / (((double)globalCloud.hosts_and_vms.hosts_pes) / ((double)globalCloud.hosts_and_vms.vm_pes)));
        globalCloud.n_edges = (int)Math.ceil(((double)HostsNumber) / ((double) globalCloud.hosts_and_vms.n_hosts_per_edges));
        globalCloud.n_cores = nSCC;
        globalCloud.n_aggregates = Math.max((globalCloud.n_edges +globalCloud.n_cores)/IoTMultiplicityForVMs, globalCloud.n_edges*2);

        for (int i = 1; i<=numberOfClouds; i++) {
            // Setting the remaining instance-dependant parameters
            // Cloud name
            globalCloud.network_configuration.datacenter_name = "Cloud_"+i;
            globalCloud.cloud_network_name = "Cloud_"+i;
            // controller name
            globalCloud.network_configuration.controller_name = "dc"+i+"_sdn"+i;
            // gateway name
            globalCloud.gateway_name = "dc"+i+"_gateway";

            // When finished, copy the current configuration
            var local = globalCloud.copy();
            ls.add(local);
        }

        return ls;
    }

    public List<EdgeInfrastructureGenerator.Configuration> generateConfigurationForSimulationTime(@Input final double tick,
                                                                                                  @Input final EdgeInfrastructureGenerator.Configuration conf,
                                                                                                  @Output Map<String, String> mapEdgeToClusterName) {
        for (var cp : edgeNetworkGenerator.chron)
            if ((cp.getLeft() <= tick) && (tick <= cp.getRight()))
                return generateConfigurationForSimulationTime(cp, conf, mapEdgeToClusterName);
        return Collections.emptyList();
    }

    private List<EdgeInfrastructureGenerator.Configuration> generateConfigurationForSimulationTime(@Input final ImmutablePair<Double, Double> cp,
                                                                                                   @Input final EdgeInfrastructureGenerator.Configuration conf,
                                                                                                   @Output Map<String, String> mapEdgeToClusterName) {
        var css = edgeNetworkGenerator.css_in_time.get(cp);
        var edges = edgeNetworkGenerator.retrieved_basic_information.get(cp.getLeft());
        var timedNetwork = edgeNetworkGenerator.timed_connectivity.get(cp.getLeft());
        List<EdgeInfrastructureGenerator.Configuration> resultCSS = new ArrayList<>();
        mapEdgeToClusterName.clear();

        for (var sub_network : css) {
            var candidate = sub_network.iterator().next();

            // Setting the remaining instance-dependant parameters
            // Cloud name
            var local = conf.copy();
            local.network_configuration.datacenter_name = "Edge_"+candidate;
            local.edge_network_name = "Edge_"+candidate;
            // controller name
            local.network_configuration.controller_name = "e_sdn_"+candidate;
            // gateway name
            local.gateway_name = "egateway_"+candidate;
            local.n_edgeDevices_and_edges = sub_network.size();

            // Setting a number of VMs which is proportional to the number given
            local.hosts_and_vms.n_vm = local.hosts_and_vms.n_hosts_per_edges * local.n_edgeDevices_and_edges;
            local.stringToInteger = new ArrayList<>(sub_network);

            for (var edge : sub_network) {
                mapEdgeToClusterName.put(edge, local.edge_network_name);
                var neigh = timedNetwork.get(edge);
                if ((neigh != null) && (!neigh.isEmpty())) {
                    local.edge_switch_network.putAll(edge, neigh);
                }
            }
            resultCSS.add(local);
        }
        return resultCSS;
    }


    public static class Configuration {
        public int numberOfClouds = 1;
        public int IoTMultiplicityForVMs = 3;
        public double global_simulation_terminate;
        public String start_time = null;
        public double start_vehicle_time;               // 0.0
        public double simulation_step;            // 1.0
        public double end_vehicle_time;                 // 100.0
        public String iots;                             // /home/giacomo/IdeaProjects/SimulatorBridger/stats/test_vehicle.json
        public String iot_generators;                   // /home/giacomo/IdeaProjects/SimulatorBridger/iot_generators.yaml
        public String strongly_connected_components;    // /home/giacomo/IdeaProjects/SimulatorBridger/rsu.csv_timed_scc.json"
        public String edge_information;                 // /home/giacomo/IdeaProjects/SimulatorBridger/stats/test_rsu.json
        public String edge_neighbours;                  // /home/giacomo/IdeaProjects/SimulatorBridger/rsu.csv_neighboursChange.json
        public String cloud_general_configuration;      // /home/giacomo/IdeaProjects/SimulatorBridger/cloud_generators.yaml
        public String edge_general_configuration;       // /home/giacomo/IdeaProjects/SimulatorBridger/edge_generators.yaml
        public String wan_general_configuration;       // /home/giacomo/IdeaProjects/SimulatorBridger/edge_generators.yaml
        public String converter_yaml;
        public String mel_app_policy;
        public boolean only_one_mel_per_edge_network;
        public String mel_routing_policy;
        public boolean ignore_csv_apps;
        public String AGENT_CONFIG_FILE;
        public String RES_CONFIG_FILE;
        public String netsim_output;
        public double reset_rsu_communication_radius;
        public int reset_max_vehicle_communication;

        public IoTEntityGenerator first() {
            return new IoTEntityGenerator(new File(iots), new File(iot_generators));
        }

        public TimeTicker ticker() {
            if (converter_yaml != null) {
                Optional<TrafficConfiguration> conf = YAML.parse(TrafficConfiguration.class, new File(converter_yaml));
                if (conf.isPresent()) {
                    start_vehicle_time = conf.get().begin;
                    end_vehicle_time = conf.get().end;
                    simulation_step = conf.get().step;
                } else {
                    logger.fatal("ERROR: the "+converter_yaml+" file from which the simulation times and granularity are stated is missing. Aborting!");
                    System.exit(1);
                }
            }
            return new TimeTicker(start_vehicle_time, simulation_step, end_vehicle_time);
        }

        public EdgeNetworksGenerator second(DSLContext context, boolean isRSUJSON) {
            return new EdgeNetworksGenerator(new File(strongly_connected_components),
                                             new File(edge_information),
                                             new File(edge_neighbours),
                                             ticker(), context, isRSUJSON);
        }

        public CloudInfrastructureGenerator.Configuration third() {
            return YAML.parse(CloudInfrastructureGenerator.Configuration.class, new File(cloud_general_configuration)).orElseThrow();
        }

        public EdgeInfrastructureGenerator.Configuration fourth() {
            return YAML.parse(EdgeInfrastructureGenerator.Configuration.class, new File(edge_general_configuration)).orElseThrow();
        }

        public WANInfrastructureGenerator.Configuration fith() {
            return YAML.parse(WANInfrastructureGenerator.Configuration.class, new File(wan_general_configuration)).orElseThrow();
        }
    }

    public List<GlobalConfigurationSettings> getTimedPossibleConfigurations(EnsembleConfigurations.Configuration conf, Connection conn, DSLContext context) {
        List<GlobalConfigurationSettings> ls = new ArrayList<>();
        List<IoTDeviceTabularConfiguration> iotDevices = ioTEntityGenerator.asIoTSQLCongigurationList(conn, context);
        AtomicInteger global_program_counter = new AtomicInteger(1);
        List<WorkloadCSV> globalApps = ioTEntityGenerator.generateAppSetUp(conf.simulation_step, global_program_counter);
        MEL_APP_POLICY casus = MEL_APP_POLICY.valueOf(conf.mel_app_policy);

        for (var consistent_network_conf : edgeNetworkGenerator.simulation_intervals) {
            List<WorkloadCSV> filteredApps;
            if (conf.ignore_csv_apps) {
                filteredApps = Collections.emptyList();
            } else {
                filteredApps = globalApps
                        .stream()
                        .filter(x -> (consistent_network_conf.getLeft() <= x.StartDataGenerationTime_Sec) &&
                                (consistent_network_conf.getRight() >= x.StopDataGeneration_Sec))
                        .collect(Collectors.toList());
            }

            Map<String, String> nodeToCloudName = new HashMap<>();
            var time = consistent_network_conf.getLeft();
            List<EdgeInfrastructureGenerator.Configuration> edgeNets = generateConfigurationForSimulationTime(time, edge, nodeToCloudName);

            filteredApps.forEach(x -> {
                x.VmName = "VM_1"; // TODO: VM_scheduling, or not, depending on the configuration
                if (casus == null)
                    x.MELName = "*";
                else switch (casus) {
                    case ANY_MEL -> x.MELName = "*";
                    case NETWORK_MEL -> x.MELName = "MEL_"+nodeToCloudName.get(x.MELName)+".*";
                    case THAT_MEL -> x.MELName = "@"+x.MELName;
                }
            });

            int nSCC = edgeNets.size();
            List<CloudInfrastructureGenerator.Configuration> cloudNets = setCloudPolicyFromIoTNumbers(conf.numberOfClouds,
                                                                                                      conf.IoTMultiplicityForVMs,
                                                                                                      nSCC,
                                                                                                      cloud, context);
            ls.add(ensemble(consistent_network_conf.getLeft(),
                            cloudNets,
                            edgeNets,
                            wan_conf,
                            iotDevices,
                            filteredApps,
                            conf)
                    );
        }
        return ls;
    }






    private GlobalConfigurationSettings ensemble(Double left, List<CloudInfrastructureGenerator.Configuration> cloudNets,
                                                 List<EdgeInfrastructureGenerator.Configuration> edgeNets,
                                                 WANInfrastructureGenerator.Configuration conf,
                                                 List<IoTDeviceTabularConfiguration> iotDevices,
                                                 List<WorkloadCSV> apps,
                                                 Configuration confDis) {

        List<TopologyLink> global_network_links = new ArrayList<>();

        Function<String, TimedEdge> f = x -> edgeNetworkGenerator.retriveEdgeLocationInTime(left, x);

        // Generating the edge nets in a way which is IoT independent, and only considering Edge devices
        List<SubNetworkConfiguration> actualEdgeDataCenters = edgeNets
                .stream()
                .map(x -> {
                    x.reset_max_vehicle_communication = confDis.reset_max_vehicle_communication;
                    return EdgeInfrastructureGenerator.generate(x, global_network_links, confDis.only_one_mel_per_edge_network, f);
                })
                .collect(Collectors.toList());

        // Generating the cloud nets
        List<SubNetworkConfiguration> actualCloudDataCenters = cloudNets
                .stream()
                .map(x -> CloudInfrastructureGenerator.generateFromConfiguration(x, global_network_links))
                .collect(Collectors.toList());

        // Generating the sdwan_switches
        List<uk.ncl.giacomobergami.components.networking.Switch> sdwan_switches =
                WANInfrastructureGenerator.generate(cloudNets, edgeNets, conf, global_network_links);


        return new GlobalConfigurationSettings(actualEdgeDataCenters,
                actualCloudDataCenters,
                iotDevices,
                global_network_links,
                sdwan_switches,
                apps,
                conf.sdwan_traffic,
                conf.sdwan_routing,
                conf.sdwan_controller,
                confDis.global_simulation_terminate,
                confDis.start_time,
                confDis.mel_routing_policy,
                confDis.iots,
                confDis.simulation_step,
                confDis.AGENT_CONFIG_FILE,
                confDis.RES_CONFIG_FILE,
                confDis.netsim_output);
    }


    public static boolean generateConfigurationFromFile(@Input File configuration_file, @Input Connection conn, @Input DSLContext context, boolean isRSUJSON) {
        var conf = YAML.parse(EnsembleConfigurations.Configuration.class, configuration_file).orElseThrow();
        var ec = new EnsembleConfigurations(conf.first(), conf.second(context, isRSUJSON), conf.third(), conf.fourth(), conf.fith());
        var ls = ec.getTimedPossibleConfigurations(conf, conn, context);
        var dump = new File(configuration_file.getParentFile(), "dump");
        if (ls.size() == 1) {
            ls.get(0).dump(dump);
        } else {
            for (int i = 0; i<ls.size(); i++) {
                File f = new File(dump, Integer.toString(i));
                ls.get(i).dump(f);
            }
        }
        return true;
    }

    public static boolean runConfigurationFromFile(@Input String file, @Input Connection conn, @Input DSLContext context, boolean isRSUJSON) {
        var configuration_file = new File(file);
        var conf = YAML.parse(EnsembleConfigurations.Configuration.class, configuration_file).orElseThrow();
        var ec = new EnsembleConfigurations(conf.first(), conf.second(context, isRSUJSON), conf.third(), conf.fourth(), conf.fith());
        var ls = ec.getTimedPossibleConfigurations(conf, conn, context);
        for (GlobalConfigurationSettings l : ls) {
            OsmoticRunner.runFromConfiguration(l, conn, context);
        }
        return true;
    }


    public static void main(String args[]) {
        DataSource dataSource = createDataSource();
        Connection conn = ConnectToSource(dataSource);
        DSLContext context = getDSLContext(conn);

        boolean isRSUJSON = false;

        generateConfigurationFromFile(new File("/home/giacomo/IdeaProjects/SimulatorBridger/inputFiles/novel/main.yaml"), conn, context, isRSUJSON);
        DisconnectFromSource(conn);
    }

}
