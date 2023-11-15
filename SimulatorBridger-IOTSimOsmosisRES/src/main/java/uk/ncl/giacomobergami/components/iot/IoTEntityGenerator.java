package uk.ncl.giacomobergami.components.iot;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import uk.ncl.giacomobergami.utils.annotations.Input;
import uk.ncl.giacomobergami.utils.annotations.Output;
import uk.ncl.giacomobergami.utils.asthmatic.WorkloadCSV;
import uk.ncl.giacomobergami.utils.asthmatic.WorkloadFromVehicularProgram;
import uk.ncl.giacomobergami.utils.data.YAML;
import uk.ncl.giacomobergami.utils.pipeline_confs.TrafficConfiguration;
import uk.ncl.giacomobergami.utils.shared_data.iot.IoT;

import java.io.*;
import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class IoTEntityGenerator {
    final TreeMap<String, IoT> timed_iots;
    final IoTGlobalConfiguration conf;
    static final HashSet<Double> setWUT = new HashSet<>();

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
        Type sccType = new TypeToken<TreeMap<String, IoT>>() {}.getType();
        if (configuration != null)
            conf = YAML.parse(IoTGlobalConfiguration.class, configuration).orElseThrow();
        else
            conf = null;
        Gson gson = new Gson();
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
        }
    }

    public Collection<Double> collectionOfWakeUpTimes() {
        File converter_file = new File("clean_example/converter.yaml");
        Optional<TrafficConfiguration> time_conf = YAML.parse(TrafficConfiguration.class, converter_file);
        double begin = time_conf.get().getBegin();
        double end = time_conf.get().getEnd();
        double latency = time_conf.get().getStep();
        latency = Math.max(latency, 0.01);

        for(double i = begin; i <= end ; i = i + latency) {
            setWUT.add((double)Math.round(i * 1000)/1000);
        }

        if(latency == 0.01) {
            for (var x : timed_iots.values()) {
                //x.dynamicInformation.keySet().stream().min(Comparator.naturalOrder()).ifPresent(set::add);
                for (var j = Collections.min(x.dynamicInformation.keySet()); j <= Collections.max(x.dynamicInformation.keySet()); j = j + latency) {
                    setWUT.add((double) Math.round(j * 1000) / 1000);
                }//patch missing wake up times
                setWUT.add(x.program.startCommunicatingAtSimulationTime);
            }
        }

        return setWUT;
    }

    public void updateIoTDevice(@Input @Output IoTDevice toUpdateWithTime,
                                @Input double simTimeLow,
                                @Input double simTimeUp) {
        simTimeLow = (double) Math.round(simTimeLow * 1000) /1000;
        simTimeUp = (double) Math.round(simTimeUp * 1000) /1000;
        File converter_file = new File("clean_example/converter.yaml");
        Optional<TrafficConfiguration> time_conf = YAML.parse(TrafficConfiguration.class, converter_file);
        double begin = time_conf.get().getBegin();
        var ls = timed_iots.get(toUpdateWithTime.getName());
        if ((simTimeLow >= begin)) { //ls.program.startCommunicatingAtSimulationTime)) {
            var times = new TreeSet<>(ls.dynamicInformation.keySet());
            /*(simTimeUp > times.last())  {
                toUpdateWithTime.transmit = false;
            } else*/ {
                Double expectedLow = times.contains(simTimeLow) ? ((Double) simTimeLow) : times.lower(simTimeLow);
                var dist = simTimeUp - simTimeLow;
                if ((expectedLow != null) && (expectedLow <= simTimeUp)) {
                    toUpdateWithTime.transmit = true;
//                System.out.println(toUpdateWithTime.getName()+" Transmitting at "+expectedLow);
                    var expObj = ls.dynamicInformation.get(expectedLow);
                    toUpdateWithTime.mobility.range.beginX = (int) (toUpdateWithTime.mobility.location.x = expObj.x);
                    toUpdateWithTime.mobility.range.beginY = (int) (toUpdateWithTime.mobility.location.y = expObj.y);
                    toUpdateWithTime.mobility.location.y = ls.dynamicInformation.get(expectedLow).y;
                    toUpdateWithTime.mobility.location.x = ls.dynamicInformation.get(expectedLow).x;
                    if (toUpdateWithTime.getName().equals("0") && ((simTimeLow - Math.floor(simTimeLow) <= 0.1))) {
                        System.out.println(simTimeLow+" time: " +toUpdateWithTime.mobility.range.beginX+"->"+toUpdateWithTime.mobility.location.x+", "+toUpdateWithTime.mobility.range.beginY+"->"+toUpdateWithTime.mobility.location.y);
                    }
                    Double expectedUp = simTimeUp + dist;
                    expectedUp = times.contains(expectedUp) ? expectedUp : times.lower(expectedUp);
                    if (expectedUp != null) {
                        expObj = ls.dynamicInformation.get(expectedUp);
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
