package uk.ncl.giacomobergami.SumoOsmosisBridger;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.cloudbus.cloudsim.core.MainEventManager;
import org.jooq.DSLContext;
import uk.ncl.giacomobergami.SumoOsmosisBridger.network_generators.EnsembleConfigurations;
import uk.ncl.giacomobergami.components.OsmoticRunner;
import uk.ncl.giacomobergami.components.loader.GlobalConfigurationSettings;
import uk.ncl.giacomobergami.components.simulator.SimulatorBridger;
import uk.ncl.giacomobergami.traffic_converter.TrafficConverterRunner;
import uk.ncl.giacomobergami.traffic_converter.abstracted.TrafficConverter;
import uk.ncl.giacomobergami.traffic_orchestrator.CentralAgentPlannerRunner;
import uk.ncl.giacomobergami.traffic_orchestrator.PreSimulatorEstimator;
import uk.ncl.giacomobergami.utils.data.YAML;
import uk.ncl.giacomobergami.utils.pipeline_confs.OrchestratorConfiguration;
import uk.ncl.giacomobergami.utils.pipeline_confs.TrafficConfiguration;
import uk.ncl.giacomobergami.utils.shared_data.edge.Edge;
import uk.ncl.giacomobergami.utils.shared_data.iot.TimedIoT;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import org.jooq.codegen.GenerationTool;

import javax.sql.DataSource;

import static uk.ncl.giacomobergami.utils.database.JavaPostGres.*;

public class SimulatorManager implements SimulatorBridger {

    DataSource dataSource = createDataSource();
    Connection conn = ConnectToSource(dataSource);
    DSLContext context = getDSLContext(conn);

    private static final String converter_out = "1_traffic_information_collector_output";
    private static final String converter_out_RSUCsvFile = "rsu.csv";
    private static final String converter_out_VehicleCsvFile = "vehicle.csv";
    private static final String orchestrator_out = "2_central_agent_oracle_output";
    private static final String orchestrator_out_rsuJsonFile = "rsu.json";
    private static final String orchestrator_out_vehicleJsonFile = "vehicle.json";
    private static final String orchestrator_out_output_stats_folder = "stats";
    private static final String orchestrator_out_output_experiment_name = "test";
    private static final String final_out = "3_extIOTSim_output";

    String converter = "clean_example/converter.yaml";
    String orchestrator = "clean_example/orchestrator.yaml";
    String simulator_runner = "clean_example/IoTSim.yaml";

    boolean step1, step2, step3;
    double simBegin, simEnd, deltaTime;

    File output_folder_1;
    File output_folder_2;
    File output_folder_3;
    File orchestrator_file;
    Optional<OrchestratorConfiguration> conf2;

    int maxAcceptableVehiclesPerEdgeNode;
    double maxCommunicationRadiusPerEdgeNode;

    File configuration_file;
    EnsembleConfigurations.Configuration conf3;
    GlobalConfigurationSettings globalConfigurationSettings = new GlobalConfigurationSettings();

    static {
        File file = new File("log4j2.xml");
        LoggerContext context = (LoggerContext) LogManager.getContext(false);
        context.setConfigLocation(file.toURI());
    }

    public void generateJooQ() {
        try {
            GenerationTool.generate(Files.readString(Path.of("jooq-config.xml")));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void configStep1(File converter_file, String finalOrchestrator, TrafficConfiguration y, Connection conn, DSLContext context) {
        output_folder_1 = new File(converter_file.getParentFile(), converter_out);
        if (!output_folder_1.exists()) {
            output_folder_1.mkdirs();
        }
        y.RSUCsvFile = new File(output_folder_1, converter_out_RSUCsvFile).getAbsolutePath();
        y.VehicleCsvFile = new File(output_folder_1, converter_out_VehicleCsvFile).getAbsolutePath();
        TrafficConverter conv1 = TrafficConverterRunner.generateFacade(y);
        if (step1) {
            try {
                conv1.run(conn, context);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }

        orchestrator_file = new File(finalOrchestrator).getAbsoluteFile();
        conf2 = YAML.parse(OrchestratorConfiguration.class, orchestrator_file);
    }

    public void configStep2(File orchestrator_file, OrchestratorConfiguration x, TrafficConfiguration y) {

        output_folder_2 = new File(orchestrator_file.getParentFile(), orchestrator_out);
        if (!output_folder_2.exists()) {
            output_folder_2.mkdirs();
        }
        x.RSUCsvFile = y.RSUCsvFile;
        x.vehicleCSVFile = y.VehicleCsvFile;
        x.RSUJsonFile = new File(output_folder_2, orchestrator_out_rsuJsonFile).getAbsolutePath();
        x.vehiclejsonFile = new File(output_folder_2, orchestrator_out_vehicleJsonFile).getAbsolutePath();
        x.output_stats_folder = new File(output_folder_2, orchestrator_out_output_stats_folder).getAbsolutePath();
        x.experiment_name = orchestrator_out_output_experiment_name;
        PreSimulatorEstimator conv2 = null;

        try {
            conv2 = CentralAgentPlannerRunner.generateFacade(x, y);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        if (step2) {
            conv2.run();
            try {
                conv2.serializeAll();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }

        maxAcceptableVehiclesPerEdgeNode = x.reset_max_vehicle_communication;
        maxCommunicationRadiusPerEdgeNode = x.reset_rsu_communication_radius;
    }

    public void configStep3(String finalSimulator_runner, File converter_file, File output_folder_1, OrchestratorConfiguration x, Connection conn, DSLContext context) {
        configuration_file = new File(finalSimulator_runner).getAbsoluteFile();
        conf3 = YAML.parse(EnsembleConfigurations.Configuration.class, configuration_file).orElseThrow();
        conf3.converter_yaml = converter_file.getAbsolutePath();
        conf3.strongly_connected_components = new File(output_folder_1, converter_out_RSUCsvFile + "_timed_scc.json").getAbsolutePath();
        conf3.edge_neighbours = new File(output_folder_1, converter_out_RSUCsvFile + "_neighboursChange.json").getAbsolutePath();
        conf3.iots = x.vehiclejsonFile;
        conf3.edge_information = x.RSUJsonFile;
        conf3.reset_rsu_communication_radius = x.reset_rsu_communication_radius;
        conf3.reset_max_vehicle_communication = x.reset_max_vehicle_communication;
        output_folder_3 = new File(configuration_file.getParentFile(), final_out);
        if (!output_folder_3.exists()) {
            output_folder_3.mkdirs();
        }
        conf3.netsim_output = output_folder_3.getAbsolutePath();
    }

    public void collectGlobalConfigurationSettings(Connection conn, DSLContext context) {
        if(step3) {
            var conv3 = new EnsembleConfigurations(conf3.first(), conf3.second(), conf3.third(), conf3.fourth(), conf3.fifth(context, step2, conf3.fourth().getMovingEdges()));
            List<GlobalConfigurationSettings> configuration_for_each_network_change = conv3.getTimedPossibleConfigurations(conf3, conn, context);
            globalConfigurationSettings = (GlobalConfigurationSettings) configuration_for_each_network_change.get(0);
        }
    }

    public double getSimBegin() {
        return simBegin;
    }

    public double getSimEnd() {
        return simEnd;
    }

    public double getDeltaTime() {
        return deltaTime;
    }

    @Override
    public void init(double start, List<Edge> edgeNodes) {

        boolean generate = false;
        step1 = true;
        step2 = false;
        step3 = true;

        if(generate) generateJooQ();

        String finalOrchestrator = orchestrator;
        String finalSimulator_runner = simulator_runner;

        var converter_file = new File(converter).getAbsoluteFile();
        Optional<TrafficConfiguration> conf1 = YAML.parse(TrafficConfiguration.class, converter_file);

        conf1.ifPresent(y -> {
            simBegin = conf1.get().getBegin();
            simEnd = conf1.get().getEnd();
            deltaTime = conf1.get().getStep();
            configStep1(converter_file, finalOrchestrator, y, conn, context);
            conf2.ifPresent(x -> {
                configStep2(orchestrator_file, x, y);
                if(step3) {
                    configStep3(finalSimulator_runner, converter_file, output_folder_1, x, conn, context);
                    collectGlobalConfigurationSettings(conn, context);
                    System.out.print("Starting Running from Configuration\n");
                    double simulationStart = start == simBegin ? deltaTime : start;
                    OsmoticRunner.runFromConfiguration(globalConfigurationSettings, conn, context, simBegin, simulationStart);
                }
            });
        });
        System.out.println("End of Setup!");
    }


    @Override
    public boolean run(double end, double delta, double simulationEnd, List<TimedIoT> injectedCommunicationEvents) {
        if(step3) {
            end += delta;
            end = (double) Math.round(end * 1000) / 1000;
            return MainEventManager.legacy_run(conn, context, end, delta) < simulationEnd;
        }
        return true;
    }

    @Override
    public void fini() {
        MainEventManager.finishSimulation(conn, context, deltaTime);
        MainEventManager.runStop();
        OsmoticRunner.LogOutput(globalConfigurationSettings, conn, context);
        DisconnectFromSource(conn);
    }
}
