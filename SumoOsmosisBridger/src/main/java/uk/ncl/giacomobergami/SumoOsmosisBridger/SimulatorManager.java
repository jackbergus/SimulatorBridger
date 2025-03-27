package uk.ncl.giacomobergami.SumoOsmosisBridger;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.mysql.cj.jdbc.result.ResultSetImpl;
import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import com.opencsv.exceptions.CsvException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.cloudbus.cloudsim.core.MainEventManager;
import org.cloudbus.osmosis.core.OsmoticBroker;
import org.jooq.DSLContext;
import org.jooq.Result;
import uk.ncl.giacomobergami.SumoOsmosisBridger.network_generators.EnsembleConfigurations;
import uk.ncl.giacomobergami.SumoOsmosisBridger.traffic_converter.SUMOConverter;
import uk.ncl.giacomobergami.components.OsmoticRunner;
import uk.ncl.giacomobergami.components.iot.IoTDeviceTabularConfiguration;
import uk.ncl.giacomobergami.components.iot.IoTEntityGenerator;
import uk.ncl.giacomobergami.components.loader.GlobalConfigurationSettings;
import uk.ncl.giacomobergami.components.simulator.OsmoticWrapper;
import uk.ncl.giacomobergami.components.simulator.SimulatorBridger;
import uk.ncl.giacomobergami.traffic_converter.TrafficConverterRunner;
import uk.ncl.giacomobergami.traffic_converter.abstracted.TrafficConverter;
import uk.ncl.giacomobergami.traffic_orchestrator.CentralAgentPlannerRunner;
import uk.ncl.giacomobergami.traffic_orchestrator.PreSimulatorEstimator;
import uk.ncl.giacomobergami.utils.data.YAML;
import uk.ncl.giacomobergami.utils.database.jooq.tables.Ambulanceinformation;
import uk.ncl.giacomobergami.utils.database.jooq.tables.Vehinformation;
import uk.ncl.giacomobergami.utils.database.jooq.tables.records.AmbulanceinformationRecord;
import uk.ncl.giacomobergami.utils.pipeline_confs.OrchestratorConfiguration;
import uk.ncl.giacomobergami.utils.pipeline_confs.TrafficConfiguration;
import uk.ncl.giacomobergami.utils.shared_data.edge.Edge;
import uk.ncl.giacomobergami.utils.shared_data.iot.TimedIoT;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.*;

import org.jooq.codegen.GenerationTool;

import javax.sql.DataSource;

import static java.lang.Double.parseDouble;
import static org.cloudbus.cloudsim.core.CloudSimTags.MAPE_WAKEUP_FOR_COMMUNICATION;
import static uk.ncl.giacomobergami.utils.database.JavaPostGres.*;

public class SimulatorManager implements SimulatorBridger {

    DataSource dataSource;
    Connection conn;
    DSLContext context;

    private static final String converter_out = "1_traffic_information_collector_output";
    private static final String converter_out_RSUCsvFile = "rsu.csv";
    private static final String converter_out_VehicleCsvFile = "vehicle.csv";
    private static final String orchestrator_out = "2_central_agent_oracle_output";
    private static final String orchestrator_out_rsuJsonFile = "rsu.json";
    private static final String orchestrator_out_vehicleJsonFile = "vehicle.json";
    private static final String orchestrator_out_output_stats_folder = "stats";
    private static final String orchestrator_out_output_experiment_name = "test";
    private static final String final_out = "3_extIOTSim_output";

    String converter;
    String orchestrator;
    String simulator_runner;

    boolean step1, step2, step3;
    static double simBegin;
    static double simEnd;
    static double deltaTime;
    public double loopDuration;

    File output_folder_1;
    File output_folder_2;
    File output_folder_3;
    File orchestrator_file;
    Optional<TrafficConfiguration> conf1;
    Optional<OrchestratorConfiguration> conf2;

    int maxAcceptableVehiclesPerEdgeNode;
    double maxCommunicationRadiusPerEdgeNode;

    File configuration_file;
    BufferedReader br;

    EnsembleConfigurations conv3;
    EnsembleConfigurations.Configuration conf3;
    List<GlobalConfigurationSettings> configuration_for_each_network_change;
    List<IoTDeviceTabularConfiguration> deviceList;
    static HashMap<String, TimedIoT> FirstSet = new HashMap<>();
    static HashMap<String, TimedIoT> SecondSet = new HashMap<>();
    HashMap<String, String> patientAmbulance = new HashMap<>();
    GlobalConfigurationSettings globalConfigurationSettings = new GlobalConfigurationSettings();

    private boolean firstLoop = true;
    private boolean addNewCSVData = true;
    private boolean addNewJSONData = true;
    DecimalFormat df = new DecimalFormat("#.###");

    private double normalLatency;
    private double boostedLatency;
    private double currentLatency;
    public double loopEndTime = 0;

    public HashSet<String> IoTDevices = new HashSet<>();

    static {
        File file = new File("log4j2.xml");
        LoggerContext context = (LoggerContext) LogManager.getContext(false);
        context.setConfigLocation(file.toURI());
    }

    public SimulatorManager() {}

    public void generateJooQ() {
        try {
            GenerationTool.generate(Files.readString(Path.of("jooq-config.xml")));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void configStep1(File converter_file, String finalOrchestrator, TrafficConfiguration y, Connection conn, DSLContext context, double latency) {
        output_folder_1 = new File(converter_file.getParentFile(), converter_out);
        if (!output_folder_1.exists()) {
            output_folder_1.mkdirs();
        }
        y.RSUCsvFile = new File(output_folder_1, converter_out_RSUCsvFile).getAbsolutePath();
        y.VehicleCsvFile = new File(output_folder_1, converter_out_VehicleCsvFile).getAbsolutePath();
        TrafficConverter conv1 = TrafficConverterRunner.generateFacade(y);
        if (step1) {
            try {
                conv1.run(conn, context, latency);
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
        deltaTime = this.currentLatency;
        return deltaTime;
    }

    public void injectCSVData(String vehicleCSVFile, boolean updatedCSV, double newLatency) throws IOException, CsvException {
        deviceList = ((GlobalConfigurationSettings) ((ArrayList) configuration_for_each_network_change).get(0)).iotDevices;
        updateCSV(vehicleCSVFile, updatedCSV);
        addToDevicesToList();
        OsmoticRunner.addIoTDevices(globalConfigurationSettings, deviceList);
        uploadInjectedDataToSQL(vehicleCSVFile);
        System.out.println("You injected new events!");
        updateCurrentLatency(newLatency);
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

    public void injectTimedIoTData(List<TimedIoT> timedIoTList, double start, double loopEndTime, double newLatency) throws IOException, CsvException {
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
            if (vehicle.simtime >= loopEndTime && vehicle.simtime < loopEndTime + normalLatency) {
                currentEvents.add(vehicle);
                IoTEntityGenerator.addNewWakeUpTimes(vehicle.simtime);
            }
        }

        if (!currentEvents.isEmpty()) {
            injectListDataToSQL(currentEvents, newLatency);
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

        IoTEntityGenerator.addNewWakeUpTimes(Double.parseDouble(strings[9]));

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
            if(!MainEventManager.IoTDeviceList.contains(allVeh)) {
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
                idtc.setInjected(true);
                deviceList.add(idtc);
            }
        }
        System.out.print("IoT Device Info Configuration Completed\n");
    }

    private void injectListDataToSQL(List<TimedIoT> timedIoTList, double newLatency) throws IOException, CsvException {
        String timedIoTFile = writeToCSV(timedIoTList);
        injectCSVData(timedIoTFile, true, newLatency);
    }

    private String writeToCSV(List<TimedIoT> timedIoTList) throws IOException {
        String CSVFilePath = "injected-list-data.csv";
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

    private double distance(double x1, double y1, double x2, double y2) {
        double ac = Math.abs(y2 - y1);
        double cb = Math.abs(x2 - x1);
        return Math.hypot(ac, cb);
    }

    public List<TimedIoT> parseJSONHealthData(String path) {
        double dist = 100.0;
        List<TimedIoT> jsonTimedIoTList = new ArrayList<>();
        try (
                InputStream inputStream = Files.newInputStream(Path.of(path));
                JsonReader reader = new JsonReader(new InputStreamReader(inputStream));
        ) {
            JsonToken check;
            reader.beginArray();
            while (reader.hasNext()) {
                while (reader.hasNext()) {
                    check = reader.peek();
                    switch (check.name()) {
                        case "BEGIN_ARRAY" -> reader.beginArray();
                        case "BEGIN_OBJECT" -> {
                            reader.beginObject();
                            String idTag = reader.nextName();
                            String id = reader.nextString();
                            String xTag = reader.nextName();
                            double x = parseDouble(reader.nextString());
                            String yTag = reader.nextName();
                            double y = parseDouble(reader.nextString());
                            String riskTag = reader.nextName();
                            boolean risk = reader.nextBoolean();
                            String simTimeTag = reader.nextName();
                            double simTime = parseDouble(reader.nextString());
                            Result<AmbulanceinformationRecord> dataRange = context.select(Ambulanceinformation.AMBULANCEINFORMATION.VEHICLE_ID, Ambulanceinformation.AMBULANCEINFORMATION.X, Ambulanceinformation.AMBULANCEINFORMATION.Y, Ambulanceinformation.AMBULANCEINFORMATION.SIMTIME, Ambulanceinformation.AMBULANCEINFORMATION.INJECTED).from(Ambulanceinformation.AMBULANCEINFORMATION).where("simtime between " + (simTime - deltaTime / 2) + " and " + (simTime + deltaTime / 2)).orderBy(Ambulanceinformation.AMBULANCEINFORMATION.SIMTIME).fetchInto(Ambulanceinformation.AMBULANCEINFORMATION);
                            if (risk) {
                                for (AmbulanceinformationRecord amb : dataRange) {
                                    if (distance(amb.get(Ambulanceinformation.AMBULANCEINFORMATION.X), amb.get(Ambulanceinformation.AMBULANCEINFORMATION.Y), x, y) < dist) {
                                        if (amb.get(Ambulanceinformation.AMBULANCEINFORMATION.VEHICLE_ID).contains("from"))
                                            patientAmbulance.putIfAbsent(id, amb.get(Ambulanceinformation.AMBULANCEINFORMATION.VEHICLE_ID));
                                        break;
                                    }
                                }
                            }

                            Result<AmbulanceinformationRecord> attachedVehicle = context.select(Ambulanceinformation.AMBULANCEINFORMATION.VEHICLE_ID, Ambulanceinformation.AMBULANCEINFORMATION.X, Ambulanceinformation.AMBULANCEINFORMATION.Y, Ambulanceinformation.AMBULANCEINFORMATION.SIMTIME, Ambulanceinformation.AMBULANCEINFORMATION.INJECTED).from(Ambulanceinformation.AMBULANCEINFORMATION).where("simtime between " + (simTime - deltaTime / 2) + " and " + (simTime + deltaTime / 2) + " AND vehicle_id ='" + patientAmbulance.get(id) + "'").orderBy(Ambulanceinformation.AMBULANCEINFORMATION.SIMTIME).fetchInto(Ambulanceinformation.AMBULANCEINFORMATION);
                            if (!attachedVehicle.isEmpty() && patientAmbulance.get(id) != null) {
                                x = attachedVehicle.get(0).get(Ambulanceinformation.AMBULANCEINFORMATION.X);
                                y = attachedVehicle.get(0).get(Ambulanceinformation.AMBULANCEINFORMATION.Y);
                            }

                            TimedIoT TIoT = new TimedIoT(id, x, y, 0, "patient", 0.0, 0.0, "", 0.0, simTime, true);
                            jsonTimedIoTList.add(TIoT);
                            reader.endObject();
                        }
                    }
                }
                reader.endArray();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return jsonTimedIoTList;
    }

    private void updateCurrentLatency(double newLatency) {
        this.currentLatency = newLatency;
    }

    public double getCurrentLatency() { return currentLatency; }

    public double getSimulationBegin() { return simBegin; }

    public static double getSimulationEnd() { return simEnd; }

    public double getLoopEndTime() {return loopEndTime;}

    public void scheduleNewWakeUpTime(Collection<Double> wakeUpTimes, double chron) {
        for (Double forcedWakeUpTime : wakeUpTimes) {
            double time = Double.parseDouble(df.format(forcedWakeUpTime)) - chron;
            if (time >= 0.0 && chron + getDeltaTime() <= simEnd) {
                MainEventManager.send(OsmoticBroker.brokerID, OsmoticBroker.brokerID, time, MAPE_WAKEUP_FOR_COMMUNICATION, null);
            }
        }
        IoTEntityGenerator.clearNewWakeUpTimes();
    }

    @Override
    public boolean init(double start, List<Edge> edgeNodes) {

        try {
            dataSource = createDataSource();
            conn = ConnectToSource(dataSource);
            context = getDSLContext(conn);
            converter = "clean_example/converter.yaml";
            orchestrator = "clean_example/orchestrator.yaml";
            simulator_runner = "clean_example/IoTSim.yaml";
            br = new BufferedReader(new InputStreamReader(System.in));

            boolean generate = false;
            step1 = true;
            step2 = false;
            step3 = true;

            if (generate) generateJooQ();

            String finalOrchestrator = orchestrator;
            String finalSimulator_runner = simulator_runner;

            var converter_file = new File(converter).getAbsoluteFile();
            conf1 = YAML.parse(TrafficConfiguration.class, converter_file);

            conf1.ifPresent(y -> {
                simBegin = conf1.get().getBegin();
                simEnd = conf1.get().getEnd();
                deltaTime = conf1.get().getStep();
                normalLatency = (conf1.get().boostLatency) ? conf1.get().normalLatency : conf1.get().step;
                boostedLatency = (conf1.get().boostLatency) ? conf1.get().boostedLatency : normalLatency;
                configStep1(converter_file, finalOrchestrator, y, conn, context, normalLatency);
                conf2.ifPresent(x -> {
                    configStep2(orchestrator_file, x, y);
                    if (step3) {
                        configStep3(finalSimulator_runner, converter_file, output_folder_1, x, conn, context);
                        collectGlobalConfigurationSettings(conn, context);
                        System.out.print("Starting Running from Configuration\n");
                        double simulationStart = start == simBegin ? deltaTime : start;
                        updateCurrentLatency(normalLatency);
                        OsmoticRunner.runFromConfiguration(globalConfigurationSettings, conn, context, simBegin, simulationStart);
                    }
                });
            });
            System.out.println("End of Setup!");
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public double run(double start, double delta, List<TimedIoT> timedIoTList) {
        boolean cont = false;
        try {
           return innerRun(start, delta, timedIoTList);
        } catch (IOException | CsvException e) {
            throw new RuntimeException(e);
        }
    }

    public double innerRun(double start, double delta, List<TimedIoT> timedIoTList) throws IOException, CsvException {

        updateCurrentLatency(delta);

        if (!step3) {
            return Integer.MAX_VALUE; //true;
        }

        if(firstLoop) {
            updateCurrentLatency(normalLatency);
            boostedLatency = conf1.get().boostLatency ? boostedLatency : delta;
            firstLoop = false;
        }

        if (Double.parseDouble(df.format(MainEventManager.clock() % normalLatency)) == 0.0) {
            updateCurrentLatency(normalLatency);
        }

        if(conf1.get().isInjectJSONData && addNewJSONData) {
            String JsonPath = conf1.get().getInjectedJSONData();
            timedIoTList.addAll(parseJSONHealthData(JsonPath));
            addNewJSONData = false;
        }

        if (!timedIoTList.isEmpty()) {
            injectTimedIoTData(timedIoTList, start, loopEndTime, boostedLatency);
        } else if (conf1.get().isInjectCSVData() && addNewCSVData) {
            injectCSVData(conf1.get().getInjectedCSVData(), false, boostedLatency);
            addNewCSVData = false;
        }

        OsmoticRunner.numberOfActiveCommsPerEdge();
        OsmoticRunner.numberOfDevicesPerEdge();

        loopDuration = (double) Math.round(normalLatency * 1000) / 1000;
        scheduleNewWakeUpTime(IoTEntityGenerator.getNewWakeUpTimes(), Double.parseDouble(df.format(MainEventManager.clock())));
        loopEndTime = MainEventManager.legacy_run(conn, context, loopEndTime, currentLatency);
        return loopEndTime;// < simEnd;
    }

    @Override
    public void fini() {
        double endTime = MainEventManager.finishSimulation(conn, context, deltaTime);
        MainEventManager.runStop();
        OsmoticRunner.LogOutput(globalConfigurationSettings, conn, context, endTime);
        DisconnectFromSource(conn);
    }
}
