package uk.ncl.giacomobergami.SumoOsmosisBridger.traffic_converter;

import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvParser;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cloudbus.cloudsim.osmesis.examples.uti.PrintResults;
import uk.ncl.giacomobergami.components.iot.IoTDeviceTabularConfiguration;
import uk.ncl.giacomobergami.components.iot.IoTEntityGenerator;
import uk.ncl.giacomobergami.components.network_type.NetworkTypingGeneratorFactory;
import uk.ncl.giacomobergami.traffic_converter.abstracted.TrafficConverter;
import uk.ncl.giacomobergami.traffic_orchestrator.rsu_network.netgen.NetworkGenerator;
import uk.ncl.giacomobergami.traffic_orchestrator.rsu_network.netgen.NetworkGeneratorFactory;
import uk.ncl.giacomobergami.traffic_orchestrator.rsu_network.rsu.RSUUpdater;
import uk.ncl.giacomobergami.traffic_orchestrator.rsu_network.rsu.RSUUpdaterFactory;
import uk.ncl.giacomobergami.utils.data.YAML;
import uk.ncl.giacomobergami.utils.pipeline_confs.TrafficConfiguration;
import uk.ncl.giacomobergami.utils.shared_data.dft.DfTEntry;
import uk.ncl.giacomobergami.utils.shared_data.edge.TimedEdge;
import uk.ncl.giacomobergami.utils.shared_data.iot.TimedIoT;
import uk.ncl.giacomobergami.utils.structures.ImmutablePair;
import uk.ncl.giacomobergami.utils.structures.StraightforwardAdjacencyList;

import java.io.*;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.fasterxml.jackson.dataformat.csv.CsvSchema.emptySchema;

/**
 * This is the new DfT Converter, based upon the paper R. Almutairi, R Gillgallon, G. Bergami, G. Morgan "Approximating
 * Real-Time IoT Interaction through Connection Counting: A QoS Perspective".
 *
 * @author  Giacomo Bergami, Reham Almutairi
 */
public class DfTConverter2 extends TrafficConverter {
    private final NetworkGenerator netGen;
    private long earliestTime;
    private final RSUUpdater rsuUpdater;
    private SUMOConfiguration concreteConf;
//    private final DocumentBuilderFactory dbf;
//    private DocumentBuilder db;
    List<Double> temporalOrdering;
//    Document networkFile;
    StraightforwardAdjacencyList<String> connectionPath;
    HashMap<Double, List<TimedIoT>> timedIoTDevices;
    HashSet<TimedEdge> roadSideUnits;
    private static Logger logger = LogManager.getRootLogger();
//    String connection_per_sim_time = "clean_example/1_newdft_input/connectionPerSimTime.csv";
//    String rsu_csv = "clean_example/1_newdft_input/rsu.csv";

    String path = "clean_example/3_extIOTSim_configuration/iot_generators.yaml";
    transient final IoTEntityGenerator.IoTGlobalConfiguration conf = YAML.parse(IoTEntityGenerator.IoTGlobalConfiguration .class, new File(path)).orElseThrow();

    public DfTConverter2(TrafficConfiguration conf)  {
        super(conf);
        concreteConf = YAML.parse(SUMOConfiguration.class, new File(conf.YAMLConverterConfiguration)).orElseThrow();
        temporalOrdering = new ArrayList<>();
        timedIoTDevices = new HashMap<>();
        roadSideUnits = new HashSet<>();
        netGen = NetworkGeneratorFactory.generateFacade(concreteConf.generateRSUAdjacencyList);
        rsuUpdater = RSUUpdaterFactory.generateFacade(concreteConf.updateRSUFields,
                concreteConf.default_rsu_communication_radius,
                concreteConf.default_max_vehicle_communication);
        connectionPath = new StraightforwardAdjacencyList<>();
    }

    @Override
    protected boolean initReadSimulatorOutput(double latency) {
        connectionPath.clear();
        temporalOrdering.clear();
        timedIoTDevices.clear();
        AtomicInteger ai = new AtomicInteger(1);

        File file = new File(concreteConf.DfT_file_path);

        CSVReader reader = null;
        List<DfTEntry> rows;
        try {
            MappingIterator<DfTEntry> personIter;
            personIter = new CsvMapper().enable(CsvParser.Feature.SKIP_EMPTY_LINES).readerFor(DfTEntry.class)
                    .with(emptySchema().withHeader().withNullValue("")).readValues(file);
            rows = personIter.readAll();
        } catch (IOException  e) {
            throw new RuntimeException(e);
        }

        // Initialize earliest and latest DateTime to extreme values
        LocalDateTime earliestDateTime = LocalDateTime.MAX;
        LocalDateTime latestDateTime = LocalDateTime.MIN;

        for (DfTEntry row : rows) {
            LocalDateTime dateTime = row.getFullDate();
            if (dateTime.isBefore(earliestDateTime)) {
                earliestDateTime = dateTime;
            }
            if (dateTime.isAfter(latestDateTime)) {
                latestDateTime = dateTime;
            }
        }

        earliestTime = earliestDateTime.toEpochSecond(ZoneOffset.UTC);
        earliestTime-=3;
//        TreeSet<Double> times = new TreeSet<>();
        long latestTime = latestDateTime.toEpochSecond(ZoneOffset.UTC);

        // Adjust configuration based on the calculated times
        getConf().begin = 0;
        getConf().end = latestTime - earliestTime;
        getConf().step = 3600.0; // Assuming each step is 1 second
        rows.sort(Comparator.comparing(DfTEntry::comparablePair));
        HashMap<String, TimedEdge> timedEdgeMap = new HashMap<>();
//        Multimap<Double, TimedIoT> multiIots = HashMultimap.create();
        File debug = new File("clean_example", "debug.info");
        FileWriter fw;
        try {
            fw = new FileWriter(debug);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        var collector = new BaseCollectorParser(temporalOrdering, vehicleCSVFile);
        collector.startDocument();
        for (DfTEntry row : rows) {
            double currTime = (row.getSimtime() - earliestTime);
            collector.addTimestamp(currTime);
            if (!timedEdgeMap.containsKey(row.getId())) {
                timedEdgeMap.put(row.getId(), new TimedEdge(row.getId(), row.getX(), row.getY(), 0, 0, 0));
            } else {
                var ref = timedEdgeMap.get(row.getId());
                if (ref.x != row.getX())
                    throw new RuntimeException("ERROR: different x");
                if (ref.y != row.getY())
                    throw new RuntimeException("ERROR: different y");
            }
            int N = row.getAll_motor_vehicles();
            for (int i = 0; i<N; i++) {
                TimedIoT TI = new TimedIoT();
                TI.setId("id_" + ai.getAndIncrement());
                TI.setX(row.getX());
                TI.setY(row.getY());
                TI.setSimtime(currTime);
                TI.setType("no_type_info");
                TI.setLane("no_lane_info");
                try {
                    fw.write(TI.getId()+" communicating with "+ row.getId()+" at time "+currTime+System.lineSeparator());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                collector.addIoTDevice(TI);
            }
        }
        try {
            fw.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        collector.endDocument();
        System.out.print("SAX parsing of SUMO XML data complete\n");

        List<IoTDeviceTabularConfiguration> IoTDevices = generateIoTDeviceConfigList(BaseCollectorParser.getFirstEntry(), BaseCollectorParser.getSecondEntry());
        TreeSet<Double> wakeupTimes = BaseCollectorParser.getWakeUpTimes();
        SerializeIoTDeviceConfigList(IoTDevices);
        SerializeWakeupTimes(wakeupTimes);

        for (var curr : timedEdgeMap.values()) {
            var rsu = new TimedEdge(curr.id, curr.x, curr.y,
                    concreteConf.default_rsu_communication_radius,
                    concreteConf.default_max_vehicle_communication, 0);
            rsuUpdater.accept(rsu);
            roadSideUnits.add(rsu);
        }
        connectionPath.clear();
        var tmp = netGen.apply(roadSideUnits);
        tmp.forEach((k, v) -> {
            connectionPath.put(k.id, v.id);
        });
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
        File name =
                Path.of("clean_example", "1_traffic_information_collector_output", "IoTDeviceInfo.ser").toFile();
        try {
            name.createNewFile();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        try {
            FileOutputStream fos = new FileOutputStream(name);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(iotDevices);
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
        File name = Path.of("clean_example", "1_traffic_information_collector_output", "WakeupTimes.ser").toFile();
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
        return temporalOrdering;
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
        temporalOrdering.clear();
        timedIoTDevices.clear();
        connectionPath.clear();
    }

    @Override
    public boolean runSimulator(TrafficConfiguration conf) {
        var conf1 = YAML.parse(IoTEntityGenerator.IoTGlobalConfiguration.class, new File("clean_example/3_extIOTSim_configuration/iot_generators.yaml")).orElseThrow();
//        var conf2 = YAML.parse(SUMOConfiguration.class, new File("clean_example/sumo.yaml")).orElseThrow();
        var latency = conf1.networkType.equals("custom") ? conf1.latency: NetworkTypingGeneratorFactory.generateFacade(conf1.networkType).getNTLat();
        conf.step = conf1.match ?  latency : conf.step;
        return true;
    }
}