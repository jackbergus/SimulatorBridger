package uk.ncl.giacomobergami.traffic_converter.abstracted;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import uk.ncl.giacomobergami.utils.algorithms.ClusterDifference;
import uk.ncl.giacomobergami.utils.algorithms.StringComparator;
import uk.ncl.giacomobergami.utils.algorithms.Tarjan;
import uk.ncl.giacomobergami.utils.data.CSVMediator;
import uk.ncl.giacomobergami.utils.pipeline_confs.TrafficConfiguration;
import uk.ncl.giacomobergami.utils.shared_data.edge.TimedEdge;
import uk.ncl.giacomobergami.utils.shared_data.edge.TimedEdgeMediator;
import uk.ncl.giacomobergami.utils.shared_data.iot.TimedIoT;
import uk.ncl.giacomobergami.utils.shared_data.iot.TimedIoTMediator;
import uk.ncl.giacomobergami.utils.structures.ImmutablePair;
import uk.ncl.giacomobergami.utils.structures.StraightforwardAdjacencyList;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public abstract class TrafficConverter {

    private final String RSUCsvFile;
    private final String vehicleCSVFile;
    private final TrafficConfiguration conf;
    private final Gson gson;
    protected TimedEdgeMediator rsum;
    protected TimedIoTMediator vehm;
    protected CSVMediator<TimedEdge>.CSVWriter rsuwrite;
    protected CSVMediator<TimedIoT>.CSVWriter vehwrite;
    private static Logger logger = LogManager.getRootLogger();

    public TrafficConverter(TrafficConfiguration conf) {
        logger.info("=== TRAFFIC CONVERTER ===");
        logger.trace("TRAFFIC CONVERTER: init");
        this.conf = conf;
        this.RSUCsvFile = conf.RSUCsvFile;
        vehicleCSVFile = conf.VehicleCsvFile;
        rsum = new TimedEdgeMediator();
        rsuwrite = null;
        vehm = new TimedIoTMediator();
        vehwrite = null;
        gson = new GsonBuilder().setPrettyPrinting().create();
    }

    protected abstract boolean initReadSimulatorOutput();
    protected abstract List<Double> getSimulationTimeUnits();
    protected abstract Collection<TimedIoT> getTimedIoT(Double tick);
    protected abstract StraightforwardAdjacencyList<String> getTimedEdgeNetwork(Double tick);
    protected abstract HashSet<TimedEdge> getTimedEdgeNodes(Double tick);
    protected abstract void endReadSimulatorOutput();

    public boolean run() {
        logger.trace("TRAFFIC CONVERTER: running the simulator as per configuration: " +conf.YAMLConverterConfiguration);
        runSimulator(conf.begin, conf.end, conf.step);
        if (!initReadSimulatorOutput()) {
            logger.info("Not generating the already-provided results");
            return false;
        } else {
            logger.trace("Collecting the data from the simulator output");
        }
        List<Double> timeUnits = getSimulationTimeUnits();
        Collections.sort(timeUnits);
        TreeMap<Double, List<List<String>>> sccPerTimeComponent = new TreeMap<>();
        TreeMap<Double, Map<String, List<String>>> timedNodeAdjacency = new TreeMap<>();
        HashSet<String> allTlsS = new HashSet<>();
        for (Double tick : timeUnits) {
            // Writing IoT Devices
            getTimedIoT(tick).forEach(this::writeTimedIoT);

            // Getting all of the IoT Devices
            HashSet<TimedEdge> allEdgeNodes = getTimedEdgeNodes(tick);
            allEdgeNodes.forEach(x -> {
                allTlsS.add(x.getId());
                writeTimedEdge(x);
            });
            StraightforwardAdjacencyList<String> network = getTimedEdgeNetwork(tick);

            var scc = new Tarjan<String>().run(network, allEdgeNodes.stream().map(TimedEdge::getId).toList());
            sccPerTimeComponent.put(tick, scc);
            timedNodeAdjacency.put(tick, network.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, x->new ArrayList<>(x.getValue()))));
        }

        logger.trace("Dumping the last results...");
        HashMap<String, ImmutablePair<ImmutablePair<Double, List<String>>, List<ClusterDifference<String>>>> delta_network_neighbours = ClusterDifference.computeTemporalDifference(timedNodeAdjacency, allTlsS, StringComparator.getInstance());
        try {
            Files.writeString(Paths.get(new File(conf.RSUCsvFile+"_"+"neighboursChange.json").getAbsolutePath()), gson.toJson(delta_network_neighbours));
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        try {
            Files.writeString(Paths.get(new File(conf.RSUCsvFile+"_"+"timed_scc.json").getAbsolutePath()), gson.toJson(sccPerTimeComponent));
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        logger.trace("quitting...");
        closeWritingTimedIoT();
        closeWritingTimedEdge();
        endReadSimulatorOutput();
        logger.info("=========================");
        return true;
    }



    protected boolean writeTimedEdge(TimedEdge object) {
        if (rsuwrite == null) {
            rsuwrite = rsum.beginCSVWrite(new File(RSUCsvFile));
            if (rsuwrite == null) return false;
        }
        return rsuwrite.write(object);
    }

    protected boolean closeWritingTimedEdge() {
        if (rsuwrite != null) {
            try {
                rsuwrite.close();
                return true;
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }
        return true;
    }

    protected boolean writeTimedIoT(TimedIoT object) {
        if (vehwrite == null) {
            vehwrite = vehm.beginCSVWrite(new File(vehicleCSVFile));
            if (vehwrite == null) return false;
        }
        return vehwrite.write(object);
    }

    protected boolean closeWritingTimedIoT() {
        if (vehwrite != null) {
            try {
                vehwrite.close();
                return true;
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }
        return true;
    }

    public abstract boolean runSimulator(long begin, long end, double step);

}
