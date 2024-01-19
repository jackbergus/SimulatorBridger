package uk.ncl.giacomobergami.components.iot;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import uk.ncl.giacomobergami.utils.algorithms.ClusterDifference;
import uk.ncl.giacomobergami.utils.annotations.Input;
import uk.ncl.giacomobergami.utils.annotations.Output;
import uk.ncl.giacomobergami.utils.asthmatic.WorkloadCSV;
import uk.ncl.giacomobergami.utils.asthmatic.WorkloadFromVehicularProgram;
import uk.ncl.giacomobergami.utils.data.YAML;
import uk.ncl.giacomobergami.utils.pipeline_confs.TrafficConfiguration;
import uk.ncl.giacomobergami.utils.shared_data.edge.TimedEdge;
import uk.ncl.giacomobergami.utils.shared_data.iot.IoT;
import uk.ncl.giacomobergami.utils.shared_data.iot.IoTProgram;
import uk.ncl.giacomobergami.utils.shared_data.iot.TimedIoT;
import uk.ncl.giacomobergami.utils.structures.Union2;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Time;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static java.lang.Double.parseDouble;

public class IoTEntityGenerator {
    final TreeMap<String, IoT> timed_iots;
    final IoTGlobalConfiguration conf;
    static final HashSet<Double> setWUT = new HashSet<>();
    final File converter_file = new File("clean_example/converter.yaml");
    final Optional<TrafficConfiguration> time_conf = YAML.parse(TrafficConfiguration.class, converter_file);
    final double begin = time_conf.get().getBegin();
    final double end = time_conf.get().getEnd();
    double latency = time_conf.get().getStep();

    public static HashSet<Double> getSetWUT() {
        return setWUT;
    }

    public static class IoTGlobalConfiguration {
        public String networkType;
        public String stepSizeEditorPath;
        public String communicationProtocol;
        public double bw;
        public double max_battery_capacity;
        public double battery_sensing_rate;
        public double battery_sending_rate;
        public String ioTClassName;
        public double signalRange;
        public double latency;
        public boolean match;
    }

    public IoTEntityGenerator(TreeMap<String, IoT> timed_scc,
                              IoTGlobalConfiguration conf) {
        this.timed_iots = timed_scc;
        this.conf = conf;
    }

    public IoTEntityGenerator(File iotFiles,
                              File configuration) {
        if (configuration != null)
            conf = YAML.parse(IoTGlobalConfiguration.class, configuration).orElseThrow();
        else
            conf = null;
        Gson gson = new Gson();
        Type sccType = new TypeToken<TreeMap<String, IoT>>() {}.getType();
        BufferedReader reader1 = null;
        try {
            reader1 = new BufferedReader(new FileReader(iotFiles.getAbsoluteFile()));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            System.exit(1);
        }
        timed_iots = gson.fromJson(reader1, sccType);
        try {
            reader1.close();
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }/*
        try {
            timed_iots = readLargeJson(String.valueOf(iotFiles.getAbsoluteFile()));//
        } catch (IOException e) {
            throw new RuntimeException(e);
        }*/
    }

    private TreeMap<String, IoT> readLargeJson(String path) throws IOException {
        Gson gson = new Gson();
        TreeMap<String, IoT> timed_IoTs = new TreeMap<>();
        try (
                InputStream inputStream = Files.newInputStream(Path.of(path));
                JsonReader reader = new JsonReader(new InputStreamReader(inputStream));
        ) {
            reader.beginObject();
            while (reader.hasNext()) {
                String name = reader.nextName();
                HashMap<Double, TimedIoT> DYNAMICINFORMATION = new HashMap<>();
                IoTProgram IProg = new IoTProgram(null);
                reader.beginObject();
                String Info = reader.nextName();
                JsonToken check;
                while (reader.hasNext()) {
                    if (Objects.equals(Info, "dynamicInformation")) {
                        reader.beginObject();
                        while (reader.hasNext()) {
                            double time = parseDouble(reader.nextName());
                            reader.beginObject();
                            while (reader.hasNext()) {
                                String idTag = reader.nextName();
                                String id = reader.nextString();
                                String xTag = reader.nextName();
                                double x = parseDouble(reader.nextString());
                                String yTag = reader.nextName();
                                double y = parseDouble(reader.nextString());
                                String angleTag = reader.nextName();
                                double angle = parseDouble(reader.nextString());
                                String typeTag = reader.nextName();
                                String type = reader.nextString();
                                String speedTag = reader.nextName();
                                double speed = parseDouble(reader.nextString());
                                String posTag = reader.nextName();
                                double pos = parseDouble(reader.nextString());
                                String laneTag = reader.nextName();
                                String lane = reader.nextString();
                                String slopeTag = reader.nextName();
                                double slope = parseDouble(reader.nextString());
                                String simTimeTag = reader.nextName();
                                double simTime = parseDouble(reader.nextString());

                                TimedIoT TIoT = new TimedIoT(id, x, y, angle, type, simTime, pos, lane, slope, simTime);
                                DYNAMICINFORMATION.put(time, TIoT);
                            }
                            reader.endObject();
                        }
                        reader.endObject();
                        Info = reader.nextName();
                        if (Objects.equals(Info, "program")) {
                            List<Union2<TimedIoT, TimedEdge>> shortest_path = new ArrayList<>();
                            boolean isStartingProgram = false;
                            List<String> setInitialClusterConnection = null;
                            ClusterDifference<String> setConnectionVariation = null;
                            TimedIoT localInformation = null;
                            reader.beginObject();
                            while (reader.hasNext()) {
                                String pathTag = reader.nextName();
                                reader.beginObject();
                                while (reader.hasNext()) {
                                    double time = parseDouble(reader.nextName());
                                    reader.beginObject();
                                    String spTAG = reader.nextName();
                                    check = reader.peek();
                                    TimedIoT spIoT;
                                    TimedEdge spEdge = null;
                                    TimedEdge spEdge2 = null;
                                    TimedEdge spEdge3 = null;
                                    if (Objects.equals(check.name(), "NULL")) {
                                        spIoT = null;
                                        reader.nextNull();
                                    } else {
                                        reader.beginArray();
                                        reader.beginObject();
                                        String val1 = reader.nextName();
                                        reader.beginObject();
                                        String idTag = reader.nextName();
                                        String id = reader.nextString();
                                        String xTag = reader.nextName();
                                        double x = parseDouble(reader.nextString());
                                        String yTag = reader.nextName();
                                        double y = parseDouble(reader.nextString());
                                        String angleTag = reader.nextName();
                                        double angle = parseDouble(reader.nextString());
                                        String typeTag = reader.nextName();
                                        String type = reader.nextString();
                                        String speedTag = reader.nextName();
                                        double speed = parseDouble(reader.nextString());
                                        String posTag = reader.nextName();
                                        double pos = parseDouble(reader.nextString());
                                        String laneTag = reader.nextName();
                                        String lane = reader.nextString();
                                        String slopeTag = reader.nextName();
                                        double slope = parseDouble(reader.nextString());
                                        String simTimeTag = reader.nextName();
                                        double simTime = parseDouble(reader.nextString());
                                        spIoT = new TimedIoT(id, x, y, angle, type, simTime, pos, lane, slope, simTime);
                                        reader.endObject();
                                        String val2 = reader.nextName();
                                        reader.nextNull();
                                        String firstTag = reader.nextName();
                                        boolean firstBool = reader.nextBoolean();
                                        reader.endObject();
                                        reader.beginObject();
                                        String val1Tag = reader.nextName();
                                        reader.nextNull();
                                        String val2Tag = reader.nextName();
                                        reader.beginObject();
                                        xTag = reader.nextName();
                                        x = parseDouble(reader.nextString());
                                        yTag = reader.nextName();
                                        y = parseDouble(reader.nextString());
                                        simTimeTag = reader.nextName();
                                        simTime = parseDouble(reader.nextString());
                                        String commRadTag = reader.nextName();
                                        double commRad = parseDouble(reader.nextString());
                                        String maxVehComTag = reader.nextName();
                                        double maxVehCom = parseDouble(reader.nextString());
                                        idTag = reader.nextName();
                                        id = reader.nextString();
                                        spEdge = new TimedEdge(id, x, y, commRad, maxVehCom, simTime);
                                        reader.endObject();
                                        firstTag = reader.nextName();
                                        firstBool = reader.nextBoolean();
                                        reader.endObject();
                                        check = reader.peek();
                                        if (Objects.equals(check.name(), "END_ARRAY")) {
                                            reader.endArray();
                                        } else {
                                            reader.beginObject();
                                            val1Tag = reader.nextName();
                                            reader.nextNull();
                                            val2Tag = reader.nextName();
                                            reader.beginObject();
                                            xTag = reader.nextName();
                                            x = parseDouble(reader.nextString());
                                            yTag = reader.nextName();
                                            y = parseDouble(reader.nextString());
                                            simTimeTag = reader.nextName();
                                            simTime = parseDouble(reader.nextString());
                                            commRadTag = reader.nextName();
                                            commRad = parseDouble(reader.nextString());
                                            maxVehComTag = reader.nextName();
                                            maxVehCom = parseDouble(reader.nextString());
                                            idTag = reader.nextName();
                                            id = reader.nextString();
                                            spEdge2 = new TimedEdge(id, x, y, commRad, maxVehCom, simTime);
                                            reader.endObject();
                                            firstTag = reader.nextName();
                                            firstBool = reader.nextBoolean();
                                            reader.endObject();
                                            check = reader.peek();
                                            if (Objects.equals(check.name(), "END_ARRAY")) {
                                                reader.endArray();
                                            } else {
                                                reader.beginObject();
                                                val1Tag = reader.nextName();
                                                reader.nextNull();
                                                val2Tag = reader.nextName();
                                                reader.beginObject();
                                                xTag = reader.nextName();
                                                x = parseDouble(reader.nextString());
                                                yTag = reader.nextName();
                                                y = parseDouble(reader.nextString());
                                                simTimeTag = reader.nextName();
                                                simTime = parseDouble(reader.nextString());
                                                commRadTag = reader.nextName();
                                                commRad = parseDouble(reader.nextString());
                                                maxVehComTag = reader.nextName();
                                                maxVehCom = parseDouble(reader.nextString());
                                                idTag = reader.nextName();
                                                id = reader.nextString();
                                                spEdge3 = new TimedEdge(id, x, y, commRad, maxVehCom, simTime);
                                                reader.endObject();
                                                firstTag = reader.nextName();
                                                firstBool = reader.nextBoolean();
                                                reader.endObject();
                                                reader.endArray();
                                            }
                                        }
                                        Union2<TimedIoT, TimedEdge> temp1 = new Union2<>();
                                        temp1.setVal1(spIoT);
                                        temp1.setVal2(spEdge);
                                        shortest_path.add(temp1);
                                        if(spEdge2 != null) {
                                            Union2<TimedIoT, TimedEdge> temp2 = new Union2<>();
                                            temp2.setVal1(spIoT);
                                            temp2.setVal2(spEdge2);
                                            shortest_path.add(temp2);
                                        }
                                        if(spEdge3 != null) {
                                            Union2<TimedIoT, TimedEdge> temp3 = new Union2<>();
                                            temp3.setVal1(spIoT);
                                            temp3.setVal2(spEdge3);
                                            shortest_path.add(temp3);
                                        }
                                    }
                                    String iSPTag = reader.nextName();
                                    isStartingProgram = reader.nextBoolean();
                                    String sICCTag = reader.nextName();
                                    check = reader.peek();
                                    if (Objects.equals(check.name(), "BEGIN_ARRAY")) {
                                        reader.beginArray();
                                        check = reader.peek();
                                        if (Objects.equals(check.name(), "END_ARRAY")) {
                                            reader.endArray();
                                        } else {
                                            String sICC = reader.nextString();
                                            reader.endArray();
                                        }
                                    } else if (Objects.equals(check.name(), "NULL")) {
                                        reader.nextNull();
                                    }
                                    String sCVTag = reader.nextName();
                                    ClusterDifference.type change = ClusterDifference.type.UNCHANGED;
                                    ;
                                    Map<String, ClusterDifference.typeOfChange> changesMap = new HashMap<>();
                                    check = reader.peek();
                                    if (Objects.equals(check.name(), "NULL")) {
                                        reader.nextNull();
                                    } else {
                                        reader.beginObject();
                                        String changeTag = reader.nextName();
                                        change = ClusterDifference.type.valueOf(reader.nextString());
                                        String changesTag = reader.nextName();
                                        reader.beginObject();
                                        check = reader.peek();
                                        if (Objects.equals(check.name(), "END_OBJECT")) {
                                            reader.endObject();
                                        } else {
                                            while (reader.hasNext()) {
                                                String edgeTag = reader.nextName();
                                                ClusterDifference.typeOfChange thisChange = ClusterDifference.typeOfChange.valueOf(reader.nextString());
                                                changesMap.put(edgeTag, thisChange);
                                            }
                                            reader.endObject();
                                        }
                                        reader.endObject();
                                    }
                                    setConnectionVariation = new ClusterDifference(change, changesMap);
                                    String liTag = reader.nextName();
                                    check = reader.peek();
                                    if (Objects.equals(check.name(), "NULL")) {
                                        reader.nextNull();
                                    } else {
                                        reader.beginObject();
                                        String idTag = reader.nextName();
                                        String id = reader.nextString();
                                        String xTag = reader.nextName();
                                        double x = parseDouble(reader.nextString());
                                        String yTag = reader.nextName();
                                        double y = parseDouble(reader.nextString());
                                        String angleTag = reader.nextName();
                                        double angle = parseDouble(reader.nextString());
                                        String typeTag = reader.nextName();
                                        String type = reader.nextString();
                                        String speedTag = reader.nextName();
                                        double speed = parseDouble(reader.nextString());
                                        String posTag = reader.nextName();
                                        double pos = parseDouble(reader.nextString());
                                        String laneTag = reader.nextName();
                                        String lane = reader.nextString();
                                        String slopeTag = reader.nextName();
                                        double slope = parseDouble(reader.nextString());
                                        String simTimeTag = reader.nextName();
                                        double simTime = parseDouble(reader.nextString());
                                        reader.endObject();
                                        localInformation = new TimedIoT(id, x, y, angle, type, speed, pos, lane, slope, simTime);
                                    }
                                    String stpTag = reader.nextName();
                                    isStartingProgram = reader.nextBoolean();
                                    reader.endObject();
                                    IProg.putDeltaRSUAssociation(time, shortest_path);
                                }
                            }
                        }
                    }
                    check = reader.peek();
                    if (Objects.equals(check.name(), "END_OBJECT")) {
                        reader.endObject();
                    }
                    String cConTag = reader.nextName();
                    reader.nextNull();
                    String sCASTTag = reader.nextName();
                    double sCAST = parseDouble(reader.nextString());
                    String minTimeTag = reader.nextName();
                    double minTime = parseDouble(reader.nextString());
                    String maxTimeTag = reader.nextName();
                    double maxTime = parseDouble(reader.nextString());
                    reader.endObject();
                }
                reader.endObject();
                IoT IoTEntry = new IoT(DYNAMICINFORMATION, IProg);
                //IoT IoTEntry = new IoT(DYNAMICINFORMATION, new IoTProgram(null));
                timed_IoTs.put(name, IoTEntry);
            }
        }
        return timed_IoTs;
    }

    public Collection<Double> collectionOfWakeUpTimes() {
        latency = Math.max(latency, 0.01);

        if(latency == 0.01) {
            for (var x : timed_iots.values()) {
                for (double i = begin; i <= end; i = i + latency) {
                    setWUT.add((double) Math.round(i * 1000) / 1000);
                }
                for (var j = Collections.min(x.dynamicInformation.keySet()); j <= Collections.max(x.dynamicInformation.keySet()); j = j + latency) {
                    setWUT.add((double) Math.round(j * 1000) / 1000);
                }//patch missing wake up times
                x.program.setMinTime(Collections.min(x.dynamicInformation.keySet()));
                x.program.setMaxTime(Collections.max(x.dynamicInformation.keySet()));
            }
        } else {
            for (var x : timed_iots.values()) {
                for (double i = begin; i <= end; i = i + latency) {
                    setWUT.add((double) Math.round(i * 1000) / 1000);
                }
                x.program.setMinTime(Collections.min(x.dynamicInformation.keySet()));
                x.program.setMaxTime(Collections.max(x.dynamicInformation.keySet()));
            }
        }
        return setWUT;
    }

    public void updateIoTDevice(@Input @Output IoTDevice toUpdateWithTime,
                                @Input double simTimeLow,
                                @Input double simTimeUp) {
        simTimeLow = (double) Math.round(simTimeLow * 1000) /1000;
        simTimeUp = (double) Math.round(simTimeUp * 1000) /1000;
        var lat = latency == 0.01 ? 0.001 : latency;
        var ls = timed_iots.get(toUpdateWithTime.getName());
        if (simTimeLow >= begin) { //ls.program.startCommunicatingAtSimulationTime) {
            //var times = new TreeSet<>(ls.dynamicInformation.keySet());
            /*(simTimeUp > times.last())  {
                toUpdateWithTime.transmit = false;
            } else*/ {
                Double expectedLow = Math.min(simTimeLow, ls.program.getMaxTime());
                expectedLow = (double) Math.round(Math.floor(expectedLow / lat) * lat * 1000) /1000;//times.contains(simTimeLow) ? ((Double) simTimeLow) : times.lower(simTimeLow);
                double dist = simTimeUp - simTimeLow;
                double lowTime = Math.floor(ls.program.getMinTime() / lat) * lat;
                var expObj = ls.dynamicInformation.get(expectedLow);
                if(expObj == null) {
                    var times = new TreeSet<>(ls.dynamicInformation.keySet());
                    expectedLow = times.contains(simTimeLow) ? ((Double) simTimeLow) : times.lower(simTimeLow);
                    expObj = ls.dynamicInformation.get(expectedLow);
                }
                if (simTimeLow >= lowTime && expectedLow != null && expObj != null) {
                    toUpdateWithTime.transmit = true;
//                System.out.println(toUpdateWithTime.getName()+" Transmitting at "+expectedLow);
                    toUpdateWithTime.mobility.range.beginX = (int) (toUpdateWithTime.mobility.location.x = expObj.x);
                    toUpdateWithTime.mobility.range.beginY = (int) (toUpdateWithTime.mobility.location.y = expObj.y);
                    toUpdateWithTime.mobility.location.y = ls.dynamicInformation.get(expectedLow).y;
                    toUpdateWithTime.mobility.location.x = ls.dynamicInformation.get(expectedLow).x;
                    if (toUpdateWithTime.getName().equals("0") && ((simTimeLow - Math.floor(simTimeLow) <= 0.1))) {
                        System.out.println(simTimeLow+" time: " +toUpdateWithTime.mobility.range.beginX+"->"+toUpdateWithTime.mobility.location.x+", "+toUpdateWithTime.mobility.range.beginY+"->"+toUpdateWithTime.mobility.location.y);
                    }
                    Double expectedUp = simTimeUp + dist;
                    //expectedUp = times.contains(expectedUp) ? expectedUp : times.lower(expectedUp);
                    expectedUp = (double) Math.round(Math.floor(expectedUp / lat) * lat * 1000) /1000;
                    expObj = ls.dynamicInformation.get(expectedUp);
                    if(expObj == null) {
                        var times = new TreeSet<>(ls.dynamicInformation.keySet());
                        expectedUp = times.contains(expectedUp) ? expectedUp : times.lower(expectedUp);
                        expObj = ls.dynamicInformation.get(expectedUp);
                    }
                    if (expectedUp <= end && expObj != null) {
                        //expectedUp = expectedUp == end ? end - lat : expectedUp;//(expectedUp != null) {
                        //expObj = ls.dynamicInformation.get(expectedUp);
                        toUpdateWithTime.mobility.range.endX = (int) expObj.x;
                        toUpdateWithTime.mobility.range.endY = (int) expObj.y;
                    }
                } else {
                    toUpdateWithTime.transmit = false;
                }
            }
        } else {
            toUpdateWithTime.transmit = false;
        }
    }

    public List<WorkloadCSV> generateAppSetUp(double simulation_step,
                                              AtomicInteger global_program_counter) {
        var vehicularConverterToWorkflow = new WorkloadFromVehicularProgram(null);
        List<WorkloadCSV> ls = new ArrayList<>();
        for (var k : timed_iots.entrySet()) {
            vehicularConverterToWorkflow.setNewVehicularProgram(k.getValue().getProgram());
            ls.addAll(vehicularConverterToWorkflow.generateFirstMileSpecifications(simulation_step, global_program_counter, null));
        }
        ls.sort(Comparator
                .comparingDouble((WorkloadCSV o) -> o.StartDataGenerationTime_Sec)
                .thenComparingDouble(o -> o.StopDataGeneration_Sec)
                .thenComparing(o -> o.IoTDevice));
        return ls;
    }

    public int maximumNumberOfCommunicatingVehicles() {
        return timed_iots.size();
    }

    public List<IoTDeviceTabularConfiguration> asIoTJSONConfigurationList() {
        return timed_iots.values()
                .stream()
                .map(x -> {
                    var ls = new TreeSet<>(x.dynamicInformation.keySet());
                    var firstTime = ls.first();
                    ls.remove(firstTime);
                    var min = x.dynamicInformation.get(firstTime);
                    var iot = new IoTDeviceTabularConfiguration();
                    iot.beginX = (int) min.x;
                    iot.beginY = (int) min.y;
                    iot.movable = ls.size() > 0;
                    if (iot.movable) {
                        iot.hasMovingRange = true;
                        var nextTime = ls.first();
                        var minNext = x.dynamicInformation.get(nextTime);
                        iot.endX = (int) minNext.x;
                        iot.endY = (int) minNext.y;
                    }
                    iot.latency = conf.latency;
                    iot.match = conf.match;
                    iot.signalRange = conf.signalRange;
                    iot.associatedEdge = null;
                    iot.networkType = conf.networkType;
                    iot.stepSizeEditorPath = conf.stepSizeEditorPath;
                    iot.velocity = min.speed;
                    iot.name = min.id;
                    iot.communicationProtocol = conf.communicationProtocol;
                    iot.bw = conf.bw;
                    iot.max_battery_capacity = conf.max_battery_capacity;
                    iot.battery_sensing_rate = conf.battery_sensing_rate;
                    iot.battery_sending_rate = conf.battery_sending_rate;
                    iot.ioTClassName = conf.ioTClassName;
                    return iot;
                }).collect(Collectors.toList());
    }

}
