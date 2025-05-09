package uk.ncl.giacomobergami.SumoOsmosisBridger.traffic_converter;

import com.opencsv.exceptions.CsvException;
import org.apache.commons.lang3.builder.CompareToBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import uk.ncl.giacomobergami.components.iot.IoTDeviceTabularConfiguration;
import uk.ncl.giacomobergami.components.iot.IoTEntityGenerator;
import uk.ncl.giacomobergami.traffic_converter.abstracted.TrafficConverter;
import uk.ncl.giacomobergami.traffic_orchestrator.rsu_network.netgen.NetworkGenerator;
import uk.ncl.giacomobergami.traffic_orchestrator.rsu_network.netgen.NetworkGeneratorFactory;
import uk.ncl.giacomobergami.traffic_orchestrator.rsu_network.rsu.RSUUpdater;
import uk.ncl.giacomobergami.traffic_orchestrator.rsu_network.rsu.RSUUpdaterFactory;
import uk.ncl.giacomobergami.utils.data.XPathUtil;
import uk.ncl.giacomobergami.utils.data.YAML;
import uk.ncl.giacomobergami.utils.pipeline_confs.TrafficConfiguration;
import uk.ncl.giacomobergami.utils.shared_data.edge.TimedEdge;
import uk.ncl.giacomobergami.utils.shared_data.iot.TimedIoT;
import uk.ncl.giacomobergami.utils.structures.ImmutablePair;
import uk.ncl.giacomobergami.utils.structures.StraightforwardAdjacencyList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import java.io.*;
import java.sql.Time;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.io.FileReader;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;
import com.opencsv.*;


@Deprecated
public class DfTConverter extends TrafficConverter {

    private final SUMOConfiguration concreteConf;
    private long earliestTime;
    private final NetworkGenerator netGen;
    private final RSUUpdater rsuUpdater;
    private DocumentBuilder db;
    Document networkFile;
    StraightforwardAdjacencyList<String> connectionPath;
    HashMap<Double, List<TimedIoT>> timedIoTDevices;
    HashSet<TimedEdge> roadSideUnits;
    // private static Logger logger = LogManager.getRootLogger();
    List<String[]> data = new ArrayList<>();
    List<TimedIoT> timedIoTs = new ArrayList<>();
    List<TimedEdge> timedEdges = new ArrayList<>();
    List<String> rows = new ArrayList<>();
    List<Double> temporalOrdering;
    private static Logger logger = LogManager.getRootLogger();
    String path = "clean_example/3_extIOTSim_configuration/iot_generators.yaml";
    CSVWriter writer = null;
    static TreeSet<Double> wakeUpTimes = new TreeSet<>();
    static HashMap<String, TimedIoT> FirstEntry = new HashMap<>();
    static HashMap<String, TimedIoT> SecondEntry = new HashMap<>();

    transient final IoTEntityGenerator.IoTGlobalConfiguration conf = YAML.parse(IoTEntityGenerator.IoTGlobalConfiguration .class, new File(path)).orElseThrow();

    public DfTConverter(TrafficConfiguration conf) {
        super(conf);
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        try {
            db = dbf.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
            db = null;
        }
        concreteConf = YAML.parse(SUMOConfiguration.class, new File(conf.YAMLConverterConfiguration)).orElseThrow();
        temporalOrdering = new ArrayList<>();
        networkFile = null;
        timedIoTDevices = new HashMap<>();
        roadSideUnits = new HashSet<>();
        netGen = NetworkGeneratorFactory.generateFacade(concreteConf.generateRSUAdjacencyList);
        rsuUpdater = RSUUpdaterFactory.generateFacade(concreteConf.updateRSUFields,
                concreteConf.default_rsu_communication_radius,
                concreteConf.default_max_vehicle_communication);
        connectionPath = new StraightforwardAdjacencyList<>();
    }

    AtomicInteger in = new AtomicInteger(0);
    TreeMap<ImmutablePair<Integer,Integer>, Integer> deviceMapping = new TreeMap<>();

    @Override
    protected boolean initReadSimulatorOutput(double latency) {
        connectionPath.clear();
        temporalOrdering.clear();
        timedIoTDevices.clear();
        networkFile = null;

        File file = new File(concreteConf.DfT_file_path);
//        Document DfTFile = null;
        try {
            CSVReader reader = new CSVReader(new FileReader(file));
            List<String[]> rows = reader.readAll();
            //determining the indices of columns
//            int timeColumnIndex = Arrays.asList(rows.get(0)).indexOf("hour");
            int VehColumnIndex = Arrays.asList(rows.get(0)).indexOf("All_motor_vehicles");
            int eastColumnIndex = Arrays.asList(rows.get(0)).indexOf("Easting");
            int northColumnIndex = Arrays.asList(rows.get(0)).indexOf("Northing");
            int laneColumnIndex = Arrays.asList(rows.get(0)).indexOf("Direction_of_travel");
            int dateColumnIndex = Arrays.asList(rows.get(0)).indexOf("Count_date");
            int idColumnIndex = Arrays.asList(rows.get(0)).indexOf("Count_point_id");
            int hourColumnIndex = Arrays.asList(rows.get(0)).indexOf("hour");
            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

            Function<String[], ImmutablePair<LocalDateTime, Integer>> f = o1 -> {
                String dateString = o1[dateColumnIndex];
                String hourString = o1[hourColumnIndex];
                LocalDateTime dateTime = LocalDateTime.parse(dateString, dateFormatter);
                // dateTime = LocalDate.parse(dateString, dateFormatter).atStartOfDay();
                int hour = Integer.parseInt(hourString);
                dateTime = dateTime.withHour(hour); // add the time in "hour" to the date
                var id = o1[idColumnIndex];
                return new ImmutablePair<>(dateTime, Integer.parseInt(id));
            };
            var body = rows.subList(1, rows.size());
            body.sort(Comparator.comparing(f::apply));

            try {
                writer = new CSVWriter(new FileWriter(vehicleCSVFile));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            String[] headers = {"vehicle_ID", "x", "y", "angle", "type", "speed", "pos", "lane", "slope", "simtime"};
            writer.writeNext(headers);

            for (String[] row : body) {
                //   String curr = String.valueOf(row[dateColumnIndex]);
                //  double currTime = Double.parseDouble(row[timeColumnIndex]); //
                //double currTime = 1; // bec each row has 1 hour which is 3600 sec
                double x = Double.parseDouble(row[eastColumnIndex]);
                double y = Double.parseDouble(row[northColumnIndex]);

                String lane = row[laneColumnIndex];
                String dateString = row[dateColumnIndex];
                String hourString = row[hourColumnIndex];
                //  String dateTimeString = dateString + "  " + hourString;
                //  System.out.println("dateString" + dateString);
                LocalDateTime dateTime = LocalDateTime.parse(dateString, dateFormatter);
                // dateTime = LocalDate.parse(dateString, dateFormatter).atStartOfDay();
                int hour = Integer.parseInt(hourString);
                dateTime = dateTime.withHour(hour); // add the time in "hour" to the date
                double currTime = (dateTime.toEpochSecond(ZoneOffset.UTC) - earliestTime);
                wakeUpTimes.add(currTime);
                temporalOrdering.add(currTime);
                var ls = new ArrayList<TimedIoT>();
                timedIoTDevices.put(currTime, ls);

                //String[] data = {String.valueOf(vehicle.getId()), String.valueOf(vehicle.getX()), String.valueOf(vehicle.getY()), String.valueOf(vehicle.getAngle()), String.valueOf(vehicle.getType()), String.valueOf(vehicle.getSpeed()), String.valueOf(vehicle.getPos()), String.valueOf(vehicle.getLane()), String.valueOf(vehicle.getSlope()), String.valueOf(vehicle.getSimtime())};
                //writer.writeNext(data);
                int N = Integer.parseInt(row[VehColumnIndex]);
                // generate ID for vehicles
                for (int counter = 0; counter < N; ) {
                    TimedIoT rec = new TimedIoT();
                    ImmutablePair<Integer, Integer> cp = new ImmutablePair<>(counter, Integer.valueOf(row[idColumnIndex]));
                    if (!deviceMapping.containsKey(cp)) {
                        deviceMapping.put(cp, deviceMapping.size());
                    }
                    rec.id = "id_" + counter; //deviceMapping.get(cp);
                    //rec.numberOfVeh = N; // need to check
                    rec.x = x;
                    rec.y = y;
                    rec.lane = lane;
                    rec.simtime = currTime; //need to solve it!! let it i?
                    ls.add(rec);

                    String[] data = {String.valueOf(rec.getId()), String.valueOf(rec.getX()), String.valueOf(rec.getY()), String.valueOf(rec.getAngle()), String.valueOf(rec.getType()), String.valueOf(rec.getSpeed()), String.valueOf(rec.getPos()), String.valueOf(rec.getLane()), String.valueOf(rec.getSlope()), String.valueOf(rec.getSimtime())};
                    writer.writeNext(data);
                    counter++;
                    if (SecondEntry.containsKey(rec.getId())) {
                        continue;
                    }
                    if (FirstEntry.containsKey(rec.getId())) {
                        SecondEntry.putIfAbsent(rec.getId(), rec);
                        continue;
                    }
                    FirstEntry.putIfAbsent(rec.getId(), rec);
                }
            }
            try {
                writer.flush();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            try {
                writer.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            List<IoTDeviceTabularConfiguration> IoTDevices = generateIoTDeviceConfigList(FirstEntry, SecondEntry);
            SerializeIoTDeviceConfigList(IoTDevices);
            SerializeWakeupTimes(wakeUpTimes);

            // 1. Extract all ID values
            Set<String> uniqueIds = new HashSet<>();
            for (int i = 0; i < body.size(); i++) {
                uniqueIds.add(body.get(i)[idColumnIndex]);
            }
            List<String> allIds = new ArrayList<>();
            for (int i = 0; i < body.size(); i++) {
                allIds.add(body.get(i)[idColumnIndex]);
            }
            // 2. Filter out duplicates
            Set<String> traffic_lights = new HashSet<>(allIds);
            // 3. Loop over the unique ID values
            for (String id : traffic_lights) {
                for (int i = 0; i < body.size(); i++) {
                    if (body.get(i)[idColumnIndex].equals(id)) {
                        var curr = rows.get(i + 1);
                        var rsu = new TimedEdge(
                                String.valueOf(curr[idColumnIndex]),
                                Double.parseDouble(curr[eastColumnIndex]),
                                Double.parseDouble(curr[northColumnIndex]),
                                concreteConf.default_rsu_communication_radius,
                                concreteConf.default_max_vehicle_communication, 0);
                        rsuUpdater.accept(rsu);
                        roadSideUnits.add(rsu);
                        break;
                    }
                }
            }
            var tmp = netGen.apply(roadSideUnits);
            tmp.forEach((k, v) -> {
                connectionPath.put(k.id, v.id);
            });
        }
        catch (FileNotFoundException e) {
            System.out.println("File not found: " + file);
            e.printStackTrace();
        } catch (IOException e) {
            System.out.println("Error reading file: " + file);
            e.printStackTrace();
        } catch (CsvException e) {
            System.out.println("Error parsing CSV file: " + file);
            e.printStackTrace();
        }
        return true;
    }

    public List<IoTDeviceTabularConfiguration> generateIoTDeviceConfigList(HashMap<String, TimedIoT> FirstSet, HashMap<String, TimedIoT> SecondSet) {
        System.out.print("Starting IoT Device Info Configuration...\n");
        List<IoTDeviceTabularConfiguration> IDTCList = new ArrayList<>();
        Set<String> allVehs = FirstSet.keySet();

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
            IDTCList.add(idtc);
        }
        System.out.print("IoT Device Info Configuration Completed\n");
        return IDTCList;
    }

    private void SerializeIoTDeviceConfigList(List<IoTDeviceTabularConfiguration> iotDevices) {
        System.out.print("Starting Serialization of IoT Device Config Info...\n");
        File name = new File( "clean_example\\1_traffic_information_collector_output\\IoTDeviceInfo.ser");
        try {
            name.createNewFile();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        try {
            FileOutputStream fos = new FileOutputStream(name);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            // write object to file
            oos.writeObject(iotDevices);
            //System.out.println("Done");
            // closing resources
            oos.close();
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        }
        System.out.print("IoT Device Config Info Serialization Complete\n");
    }

    private void SerializeWakeupTimes(TreeSet<Double> wakeupTimes) {
        System.out.print("Starting Serialization of Wakeup Times...\n");
        File name = new File( "clean_example\\1_traffic_information_collector_output\\WakeupTimes.ser");
        try {
            name.createNewFile();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        try {
            FileOutputStream fos = new FileOutputStream(name);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            // write object to file
            oos.writeObject(wakeupTimes);
            //System.out.println("Done");
            // closing resources
            oos.close();
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        }
        System.out.print("Wakeup Times Serialization Complete\n");
    }
    @Override
    protected List<Double> getSimulationTimeUnits() {
        return new ArrayList<>(new TreeSet<>(temporalOrdering));
    }

    @Override
    protected Collection<TimedIoT> getTimedIoT(Double tick) {
        return timedIoTDevices.get(tick);
    }

    protected HashMap<Double, List<TimedIoT>> getAllTimedIoT() {
        return timedIoTDevices;
    }

    @Override
    protected StraightforwardAdjacencyList<String> getTimedEdgeNetwork(Double tick) {
        return connectionPath;
    }

    @Override
    protected HashSet<TimedEdge> getTimedEdgeNodes(Double tick) {
        return roadSideUnits.stream().map(x -> {
            var ls = x.copy();
            ls.setSimtime(tick);
            return ls;
        }).collect(Collectors.toCollection(HashSet<TimedEdge>::new));
    }

    @Override
    protected void endReadSimulatorOutput() {
        data.clear();
        timedIoTs.clear();
        timedEdges.clear();
        temporalOrdering.clear();
        timedIoTDevices.clear();
        networkFile = null;
        connectionPath.clear();
    }

    @Override
    public boolean runSimulator(TrafficConfiguration conf) {
        File file = new File(concreteConf.DfT_file_path);
        try {
            CSVReader reader = new CSVReader(new FileReader(file));
            List<String[]> allRows = reader.readAll();
            String[] headers = allRows.get(0);
            int dateColumnIndex = Arrays.asList(headers).indexOf("Count_date");
            int hourColumnIndex = Arrays.asList(headers).indexOf("hour");
            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

            // Initialize earliest and latest DateTime to extreme values
            LocalDateTime earliestDateTime = LocalDateTime.MAX;
            LocalDateTime latestDateTime = LocalDateTime.MIN;

            for (String[] row : allRows.subList(1, allRows.size())) {
                String dateString = row[dateColumnIndex];
                int hour = Integer.parseInt(row[hourColumnIndex]);
                LocalDateTime dateTime = LocalDate.parse(dateString, dateFormatter).atStartOfDay().withHour(hour);

                if (dateTime.isBefore(earliestDateTime)) {
                    earliestDateTime = dateTime;
                }
                if (dateTime.isAfter(latestDateTime)) {
                    latestDateTime = dateTime;
                }
            }

            earliestTime = earliestDateTime.toEpochSecond(ZoneOffset.UTC);
            earliestTime-=3;
            long latestTime = latestDateTime.toEpochSecond(ZoneOffset.UTC);

            // Adjust configuration based on the calculated times
            conf.begin = 0;
            conf.end = latestTime - earliestTime;
            conf.step = 3600; // Assuming each step is 1 second


        } catch (FileNotFoundException e) {
            System.out.println("File not found: " + file);
            e.printStackTrace();
            return false;
        } catch (IOException | CsvException e) {
            System.out.println("Error reading file: " + file);
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public static TreeSet<Double> getWakeUpTimes() {
        return wakeUpTimes;
    }

    public static HashMap<String, TimedIoT> getFirstEntry() {
        return FirstEntry;
    }

    public static HashMap<String, TimedIoT> getSecondEntry() {
        return SecondEntry;
    }

}
