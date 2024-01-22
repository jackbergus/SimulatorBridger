package uk.ncl.giacomobergami.SumoOsmosisBridger;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.jooq.codegen.GenerationTool;
import org.postgresql.ds.PGSimpleDataSource;
import uk.ncl.giacomobergami.SumoOsmosisBridger.network_generators.EnsembleConfigurations;
import uk.ncl.giacomobergami.components.OsmoticRunner;
import uk.ncl.giacomobergami.traffic_converter.TrafficConverterRunner;
import uk.ncl.giacomobergami.traffic_converter.abstracted.TrafficConverter;
import uk.ncl.giacomobergami.traffic_orchestrator.PreSimulatorEstimator;
import uk.ncl.giacomobergami.traffic_orchestrator.CentralAgentPlannerRunner;
import uk.ncl.giacomobergami.utils.data.YAML;
import uk.ncl.giacomobergami.utils.pipeline_confs.OrchestratorConfiguration;
import uk.ncl.giacomobergami.utils.pipeline_confs.TrafficConfiguration;

import javax.sql.DataSource;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.util.Optional;

public class MainExample {

    private static final String converter_out = "1_traffic_information_collector_output";
    private static final String converter_out_RSUCsvFile = "rsu.csv";
    private static final String converter_out_VehicleCsvFile = "vehicle.csv";
    private static final String orchestrator_out = "2_central_agent_oracle_output";
    private static final String orchestrator_out_rsujsonFile = "rsu.json";
    private static final String orchestrator_out_vehiclejsonFile = "vehicle.json";
    private static final String orchestrator_out_output_stats_folder = "stats";
    private static final String orchestrator_out_output_experiment_name = "test";
    private static final String final_out = "3_extIOTSim_output";

    static {
        File file = new File("log4j2.xml");
        LoggerContext context = (LoggerContext) LogManager.getContext(false);
        context.setConfigLocation(file.toURI());
    }

    public static void main(String args[]) {

        boolean generate = false;
        if(generate) {
            try {
                GenerationTool.generate(Files.readString(Path.of("jooq-config.xml")));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        String converter = "clean_example/converter.yaml";
        String orchestrator = "clean_example/orchestrator.yaml";
        String simulator_runner = "clean_example/IoTSim.yaml";
        if (args.length >= 3) {
            converter = args[0];
            orchestrator = args[1];
            simulator_runner = args[2];
        }
        String finalOrchestrator = orchestrator;
        String finalSimulator_runner = simulator_runner;

        // Dumping the traffic simulation
        var converter_file = new File(converter).getAbsoluteFile();
        Optional<TrafficConfiguration> conf1 = YAML.parse(TrafficConfiguration.class, converter_file);
        // First configuration step
        conf1.ifPresent(y -> {
            var output_folder_1 = new File(converter_file.getParentFile(), converter_out);
            if (!output_folder_1.exists()) {
                output_folder_1.mkdirs();
            }
            y.RSUCsvFile = new File(output_folder_1, converter_out_RSUCsvFile).getAbsolutePath();
            y.VehicleCsvFile = new File(output_folder_1, converter_out_VehicleCsvFile).getAbsolutePath();
            TrafficConverter conv1 = TrafficConverterRunner.generateFacade(y);
            try {
                conv1.run();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
            var orchestrator_file = new File(finalOrchestrator).getAbsoluteFile();
            Optional<OrchestratorConfiguration> conf2 = YAML.parse(OrchestratorConfiguration.class, orchestrator_file);

            // Second configuration step
            conf2.ifPresent(x -> {
                var output_folder_2 = new File(orchestrator_file.getParentFile(), orchestrator_out);
                if (!output_folder_2.exists()) {
                    output_folder_2.mkdirs();
                }
                x.RSUCsvFile = y.RSUCsvFile;
                x.vehicleCSVFile = y.VehicleCsvFile;
                x.RSUJsonFile = new File(output_folder_2, orchestrator_out_rsujsonFile).getAbsolutePath();
                x.vehiclejsonFile = new File(output_folder_2, orchestrator_out_vehiclejsonFile).getAbsolutePath();
                x.output_stats_folder = new File(output_folder_2, orchestrator_out_output_stats_folder).getAbsolutePath();
                x.experiment_name = orchestrator_out_output_experiment_name;
                PreSimulatorEstimator conv2 = null;
                try {
                    conv2 = CentralAgentPlannerRunner.generateFacade(x, y);
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
                conv2.run();
                try {
                    conv2.serializeAll();
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }

                var maxAcceptableVehiclesPerEdgeNode = x.reset_max_vehicle_communication;
                var maxCommunicationRadiusPerEdgeNode = x.reset_rsu_communication_radius;

                // Third configuration step
                var configuration_file = new File(finalSimulator_runner).getAbsoluteFile();
                var conf3 = YAML.parse(EnsembleConfigurations.Configuration.class, configuration_file).orElseThrow();
                conf3.converter_yaml = converter_file.getAbsolutePath();
                conf3.strongly_connected_components = new File(output_folder_1, converter_out_RSUCsvFile+"_timed_scc.json").getAbsolutePath();
                conf3.edge_neighbours = new File(output_folder_1, converter_out_RSUCsvFile+"_neighboursChange.json").getAbsolutePath();
                conf3.iots = x.vehiclejsonFile;
                conf3.edge_information = x.RSUJsonFile;
                conf3.reset_rsu_communication_radius = x.reset_rsu_communication_radius;
                conf3.reset_max_vehicle_communication = x.reset_max_vehicle_communication;
                var output_folder_3 = new File(configuration_file.getParentFile(), final_out);
                if (!output_folder_3.exists()) {
                    output_folder_3.mkdirs();
                }
                conf3.netsim_output = output_folder_3.getAbsolutePath();
                var conv3 = new EnsembleConfigurations(conf3.first(), conf3.second(), conf3.third(), conf3.fourth(), conf3.fith());
                var configuration_for_each_network_change = conv3.getTimedPossibleConfigurations(conf3);
                configuration_for_each_network_change.forEach(OsmoticRunner::runFromConfiguration);
            });
        });
    }

}
