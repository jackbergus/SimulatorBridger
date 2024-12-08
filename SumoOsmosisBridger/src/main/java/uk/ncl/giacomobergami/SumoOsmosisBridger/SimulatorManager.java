package uk.ncl.giacomobergami.SumoOsmosisBridger;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import com.opencsv.exceptions.CsvException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.cloudbus.cloudsim.core.MainEventManager;
import org.jooq.DSLContext;
import uk.ncl.giacomobergami.SumoOsmosisBridger.network_generators.EnsembleConfigurations;
import uk.ncl.giacomobergami.components.OsmoticRunner;
import uk.ncl.giacomobergami.components.iot.IoTDeviceTabularConfiguration;
import uk.ncl.giacomobergami.components.iot.IoTEntityGenerator;
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

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;

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

    boolean allowInjectedData = true;
    boolean step1, step2, step3;
    double simBegin, simEnd, deltaTime;
    public double loopDuration;
    double lastRunTime = 0;

    File output_folder_1;
    File output_folder_2;
    File output_folder_3;
    File orchestrator_file;
    Optional<OrchestratorConfiguration> conf2;

    int maxAcceptableVehiclesPerEdgeNode;
    double maxCommunicationRadiusPerEdgeNode;

    File configuration_file;
    BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

    EnsembleConfigurations conv3;
    EnsembleConfigurations.Configuration conf3;
    List<GlobalConfigurationSettings> configuration_for_each_network_change;
    List<IoTDeviceTabularConfiguration> deviceList;
    static HashMap<String, TimedIoT> FirstSet = new HashMap<>();
    static HashMap<String, TimedIoT> SecondSet = new HashMap<>();
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
            conv3 = new EnsembleConfigurations(conf3.first(), conf3.second(), conf3.third(), conf3.fourth(), conf3.fifth(context, step2, conf3.fourth().getMovingEdges()));
            configuration_for_each_network_change = conv3.getTimedPossibleConfigurations(conf3, conn, context);
            globalConfigurationSettings = configuration_for_each_network_change.get(0);
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

    public void injectCSVData(String vehicleCSVFile, boolean updatedCSV) throws IOException, CsvException {

        deviceList = ((GlobalConfigurationSettings) ((ArrayList) configuration_for_each_network_change).get(0)).iotDevices;

        if (conf3.isInjectData) {
            updateCSV(vehicleCSVFile, updatedCSV);
            addToDevicesToList();
            OsmoticRunner.addIoTDevices(globalConfigurationSettings, deviceList);
            uploadInjectedDataToSQL(vehicleCSVFile);
        }
        System.out.println("You injected new events!");
        allowInjectedData = false;
    }

    protected void updateCSV(String vehicleCSVFile, boolean updatedCSV) throws IOException, CsvException {
        CSVReader reader = new CSVReader(new FileReader(vehicleCSVFile));
        List<String[]> csvBody = reader.readAll();
        for (int i = 1; i < csvBody.size(); i++) {
            if(!updatedCSV) {
                csvBody.get(i)[0] = csvBody.get(i)[0] + "_injected";
                csvBody.get(i)[10] = "true";
            }
            toTimedIoT(csvBody.get(i));
        }
        CSVWriter writer = new CSVWriter(new FileWriter(vehicleCSVFile));
        writer.writeAll(csvBody);
        writer.flush();
        System.out.println("Injected data updated");
    }

    public void injectTimedIoTData(List<TimedIoT> timedIoTList, double start, double loopEndTime) throws IOException, CsvException {
        List<TimedIoT> processedEvents = new ArrayList<>();
        for(TimedIoT vehicle : timedIoTList) {
            if (vehicle.simtime < start) {
                processedEvents.add(vehicle);
            }
        }

        timedIoTList.removeAll(processedEvents);
        processedEvents.clear();

        List<TimedIoT> currentEvents = new ArrayList<>();
        for (TimedIoT vehicle : timedIoTList) {
            if (vehicle.simtime >= lastRunTime && vehicle.simtime < loopEndTime) {
                currentEvents.add(vehicle);
                processedEvents.add(vehicle);
            }
        }

        timedIoTList.removeAll(processedEvents);
        processedEvents.clear();

        if (!currentEvents.isEmpty()) {
            injectListDataToSQL(currentEvents);
            System.out.println("You injected new events!");
            currentEvents.clear();
        }
    }

    private void toTimedIoT(String[] strings) {
        TimedIoT TI = new TimedIoT();
        TI.setId(strings[0]);
        TI.setX(Double.parseDouble(strings[1]));
        TI.setY(Double.parseDouble(strings[2]));
        TI.setAngle(Double.parseDouble(strings[3]));
        TI.setType(strings[4]);
        TI.setSpeed(Double.parseDouble(strings[5]));
        TI.setPos(Double.parseDouble(strings[6]));
        TI.setLane(strings[7]);
        TI.setSlope(Double.parseDouble(strings[8]));
        TI.setSimtime(Double.parseDouble(strings[9]));
        TI.setInjected(Boolean.parseBoolean(strings[10]));

        if (SecondSet.containsKey(TI.getId())) {
            return;
        }
        if (FirstSet.containsKey(TI.getId())) {
            SecondSet.putIfAbsent(TI.getId(), TI);
            return;
        }
        FirstSet.putIfAbsent(TI.getId(), TI);
    }

    private void uploadInjectedDataToSQL(String vehicleCSVFile) {
        String targetTABLE = "vehInformation";
        System.out.print("Organising new vehInformation Data...\n");
        long startTime = System.nanoTime();
        copyCSVDATA(conn, vehicleCSVFile, targetTABLE);
        transferDATABetweenTables(conn, "vehInformation (vehicle_ID,x,y,angle,vehicle_type,speed,pos,lane,slope,simtime,injected)",
                "vehicle_ID,x,y,angle,vehicle_type,speed,pos,lane,slope,simtime,injected", targetTABLE);
        emptyTABLE(conn, targetTABLE+"_import");
        long endTime = System.nanoTime();
        long executionTime = (endTime - startTime) / 1000000;
        System.out.print("Sending vehInformation to SQL Database\n");
        System.out.println("This takes " + executionTime + "ms");
    }

    private void addToDevicesToList(){
        System.out.print("Starting IoT Device Info Configuration...\n");
        Set<String> allVehs = FirstSet.keySet();
        IoTEntityGenerator.IoTGlobalConfiguration conf = conv3.ioTEntityGenerator.conf;

        for (String allVeh : allVehs) {
            IoTDeviceTabularConfiguration idtc = new IoTDeviceTabularConfiguration();
            idtc.beginX = (int) FirstSet.get(allVeh).getX();
            idtc.beginY = (int) FirstSet.get(allVeh).getY();
            idtc.movable = SecondSet.containsKey(allVeh);
            if (idtc.movable) {
                idtc.hasMovingRange = true;
                idtc.endX = (int) SecondSet.get(allVeh).getX();
                idtc.endY = (int) SecondSet.get(allVeh).getY();
            }
            idtc.latency = conf.latency;
            idtc.match = conf.match;
            idtc.signalRange = conf.signalRange;
            idtc.associatedEdge = null;
            idtc.networkType = conf.networkType;
            idtc.stepSizeEditorPath = conf.stepSizeEditorPath;
            idtc.velocity = FirstSet.get(allVeh).getSpeed();
            idtc.name = allVeh;
            idtc.communicationProtocol = conf.communicationProtocol;
            idtc.bw = conf.bw;
            idtc.max_battery_capacity = conf.max_battery_capacity;
            idtc.battery_sensing_rate = conf.battery_sensing_rate;
            idtc.battery_sending_rate = conf.battery_sending_rate;
            idtc.ioTClassName = conf.ioTClassName;
            deviceList.add(idtc);
        }
        System.out.print("IoT Device Info Configuration Completed\n");
    }

    private void injectListDataToSQL(List<TimedIoT> timedIoTList) throws IOException, CsvException {
        String timedIoTFile = writeToCSV(timedIoTList);
        injectCSVData(timedIoTFile, true);
    }

    private String writeToCSV(List<TimedIoT> timedIoTList) throws IOException {
        String CSVFilePath = "example-list-data.csv";
        CSVWriter writer;

        writer = new CSVWriter(new FileWriter(CSVFilePath));

        String[] headers = {"id", "x", "y", "angle", "type", "speed", "pos", "lane", "slope", "simtime", "injected"};
        writer.writeNext(headers);

        for (TimedIoT vehicle : timedIoTList) {
            String[] data = {String.valueOf(vehicle.getId()), String.valueOf(vehicle.getX()), String.valueOf(vehicle.getY()), String.valueOf(vehicle.getAngle()), String.valueOf(vehicle.getType()), String.valueOf(vehicle.getSpeed()), String.valueOf(vehicle.getPos()), String.valueOf(vehicle.getLane()), String.valueOf(vehicle.getSlope()), String.valueOf(vehicle.getSimtime()), String.valueOf(vehicle.isInjected())};
            writer.writeNext(data);
        }

        writer.flush();

        return CSVFilePath;
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

    public boolean run(double start, double delta, List<TimedIoT> timedIoTList) {
        boolean cont = false;
        try {
           cont = innerRun(start, delta, timedIoTList);
        } catch (IOException | CsvException e) {
            throw new RuntimeException(e);
        }
        return cont;
    }

    public boolean innerRun(double start, double delta, List<TimedIoT> timedIoTList) throws IOException, CsvException {
        double loopEndTime = (start > lastRunTime) ? start : loopDuration;

        if (!step3) {
            return true;
        }

        if (!timedIoTList.isEmpty()) {
            injectTimedIoTData(timedIoTList, start, loopEndTime);
        } else if (conf3.isInjectData && allowInjectedData) {
            injectCSVData(conf3.injectedData, false);
        }

        loopEndTime += delta;
        loopEndTime = (double) Math.round(loopEndTime * 1000) / 1000;
        loopDuration = (double) Math.round(loopEndTime * 1000) / 1000;
        lastRunTime = MainEventManager.clock();
        return MainEventManager.legacy_run(conn, context, loopEndTime, delta) < simEnd;
    }

    @Override
    public void fini() {
        MainEventManager.finishSimulation(conn, context, deltaTime);
        MainEventManager.runStop();
        OsmoticRunner.LogOutput(globalConfigurationSettings, conn, context);
        DisconnectFromSource(conn);
    }
}
