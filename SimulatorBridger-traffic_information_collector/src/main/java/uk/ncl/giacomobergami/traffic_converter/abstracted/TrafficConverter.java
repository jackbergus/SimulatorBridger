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

import javax.sql.DataSource;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

import static uk.ncl.giacomobergami.utils.database.JavaPostGres.*;

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
    protected abstract HashMap<Double, List<TimedIoT>> getAllTimedIoT();
    protected abstract StraightforwardAdjacencyList<String> getTimedEdgeNetwork(Double tick);
    protected abstract HashSet<TimedEdge> getTimedEdgeNodes(Double tick);
    protected abstract void endReadSimulatorOutput();

    public boolean run() throws SQLException {
        logger.trace("TRAFFIC CONVERTER: running the simulator as per configuration: " + conf.YAMLConverterConfiguration);
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
        ArrayList<TimedEdge> timedEdgeFullSet = new ArrayList<>();
        for (Double tick : timeUnits) {
            // Writing IoT Devices
            getTimedIoT(tick).forEach(this::writeTimedIoT);

            // Getting all of the IoT Devices
            HashSet<TimedEdge> allEdgeNodes = getTimedEdgeNodes(tick);
            allEdgeNodes.forEach(x -> {
                allTlsS.add(x.getId());
                writeTimedEdge(x);
                timedEdgeFullSet.add(x);
            });
            StraightforwardAdjacencyList<String> network = getTimedEdgeNetwork(tick);

            var scc = new Tarjan<String>().run(network, allEdgeNodes.stream().map(TimedEdge::getId).toList());
            sccPerTimeComponent.put(tick, scc);
            timedNodeAdjacency.put(tick, network.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, x -> new ArrayList<>(x.getValue()))));
        }

        logger.trace("Dumping the last results...");
        HashMap<String, ImmutablePair<ImmutablePair<Double, List<String>>, List<ClusterDifference<String>>>> delta_network_neighbours = ClusterDifference.computeTemporalDifference(timedNodeAdjacency, allTlsS, StringComparator.getInstance());

        write_to_SQL(getAllTimedIoT(), false, timedEdgeFullSet, false, sccPerTimeComponent, false, delta_network_neighbours, false);

        try {
            Files.writeString(Paths.get(new File(conf.RSUCsvFile + "_" + "neighboursChange.json").getAbsolutePath()), gson.toJson(delta_network_neighbours));
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        try {
            Files.writeString(Paths.get(new File(conf.RSUCsvFile + "_" + "timed_scc.json").getAbsolutePath()), gson.toJson(sccPerTimeComponent));
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

    protected void write_to_SQL(Object TimedIoTData, boolean deleteIoTSQLData, Object TimedEdgeData, boolean deleteEdgeSQLData, Object Timed_SCCData, boolean deleteTimed_SCCData, Object NeighbourData, boolean deleteNeighbourData) throws SQLException {
        DataSource dataSource = createDataSource();
        Connection conn = ConnectToSource(dataSource);
        INSERTTimedIoTData(conn, TimedIoTData);
        if (deleteIoTSQLData) emptyTABLE(conn, "vehInformation");
        INSERTTimedEdgeData(conn, TimedEdgeData);
        if (deleteEdgeSQLData) emptyTABLE(conn, "rsuInformation");
        INSERTTimed_SCCData(conn, Timed_SCCData);
        if (deleteTimed_SCCData) emptyTABLE(conn, "timed_scc");
        INSERTNeighbourData(conn, NeighbourData);
        if (deleteNeighbourData) emptyTABLE(conn, "neighboursChange");
        DisconnectFromSource(conn);
    }

    protected void INSERTTimedIoTData(Connection conn, Object writable) throws SQLException {

        if (TABLEsize(conn, "vehInformation") != 0) {
            return;
        }

        int start_ID = 1;

        int noVehicles = ((HashMap) writable).values().size();
        for (int i = 0; i < noVehicles; i++) {
            int diffTimes = ((ArrayList) ((HashMap) writable).values().toArray()[i]).size();
            for (int j = 0; j < diffTimes; j++) {
                TimedIoT entry = (TimedIoT) ((ArrayList) ((HashMap) writable).values().toArray()[i]).get(j);
                PreparedStatement insertStmt = StartINSERTtoTable(conn, "vehInformation(di_entry_id, vehicle_id, x, y, angle, vehicle_type, speed, pos, lane, slope, simtime)");
                int di_entry_ID = INSERTInt(insertStmt, 1, start_ID);
                int vehicle_ID = INSERTString(insertStmt, 2, entry.getId());
                int di_x = INSERTDouble(insertStmt, 3, entry.getX());
                int di_y = INSERTDouble(insertStmt, 4, entry.getY());
                int angle = INSERTDouble(insertStmt, 5, entry.getAngle());
                int vehicle_type = INSERTString(insertStmt, 6, entry.getType());
                int speed = INSERTDouble(insertStmt, 7, entry.getSpeed());
                int pos = INSERTDouble(insertStmt, 8, entry.getPos());
                int lane = INSERTString(insertStmt, 9, entry.getLane());
                int slope = INSERTDouble(insertStmt, 10, entry.getSlope());
                int simtime = INSERTDouble(insertStmt, 11, entry.getSimtime());
                boolean finishedInsert = EndINSERTtoTable(insertStmt);

                start_ID++;
            }
        }
    }

    protected void INSERTTimedEdgeData(Connection conn, Object writable) throws SQLException {

        if (TABLEsize(conn, "rsuInformation") != 0) {
            return;
        }
        int start_ID = 1;

        int noEntries = ((ArrayList) writable).size();
        for (int i = 0; i < noEntries; i++) {
            TimedEdge entry = (TimedEdge) ((ArrayList) writable).get(i);
            PreparedStatement insertStmt = StartINSERTtoTable(conn, "rsuInformation(unique_entry_id, rsu_id, x, y, simtime, communication_radius, max_vehicle_communication)");
            int unique_entry_ID = INSERTInt(insertStmt, 1, start_ID);
            int rsu_ID = INSERTString(insertStmt, 2, entry.getId());
            int ri_x = INSERTDouble(insertStmt, 3, entry.getX());
            int ri_y = INSERTDouble(insertStmt, 4, entry.getY());
            int simtime = INSERTDouble(insertStmt, 5, entry.getSimtime());
            int communication_radius = INSERTDouble(insertStmt, 6, entry.getCommunication_radius());
            int max_vehicle_communication  =INSERTDouble(insertStmt, 7, entry.getMax_vehicle_communication());
            boolean finishedInsert = EndINSERTtoTable(insertStmt);

            start_ID++;
        }
    }

    protected void INSERTTimed_SCCData(Connection conn, Object writable) throws SQLException {

        if (TABLEsize(conn, "timed_scc") != 0) {
            return;
        }
        int start_ID = 1;

        Set allTimes = ((TreeMap) writable).entrySet();
        int noTimeEntries = ((TreeMap) writable).size();
        for (Map.Entry<Double, ArrayList> timeEntry : ((TreeMap<Double, ArrayList>) writable).entrySet()) {
            int noNeighbourEntries = timeEntry.getValue().size();
            for (int i = 0; i < noNeighbourEntries; i++) {
                ArrayList entry  = (ArrayList) timeEntry.getValue().get(i);
                int noNeighbours = entry.size();

                PreparedStatement insertStmt = StartINSERTtoTable(conn, "timed_scc(unique_entry_id, time_of_update, networkneighbours1, networkneighbours2, networkneighbours3, networkneighbours4)");
                int unique_entry_ID = INSERTInt(insertStmt, 1, start_ID);
                int time_of_update = INSERTDouble(insertStmt, 2, timeEntry.getKey());
                int networkneighbours1 = noNeighbours >= 1 ? INSERTString(insertStmt, 3, (String) entry.get(0)) : INSERTString(insertStmt, 3, "null");
                int networkneighbours2 = noNeighbours >= 2 ? INSERTString(insertStmt, 4, (String) entry.get(1)) : INSERTString(insertStmt, 4, "null");
                int networkneighbours3 = noNeighbours >= 3 ? INSERTString(insertStmt, 5, (String) entry.get(2)) : INSERTString(insertStmt, 5, "null");
                int networkneighbours4 = noNeighbours >= 4 ? INSERTString(insertStmt, 6, (String) entry.get(3)) : INSERTString(insertStmt, 6, "null");
                boolean finishedInsert = EndINSERTtoTable(insertStmt);

                start_ID++;
            }
        }
    }

    protected void INSERTNeighbourData(Connection conn, Object writable) throws SQLException {

        if (TABLEsize(conn, "neighboursChange") != 0) {
            return;
        }
        int start_ID = 1;

        var allRSU = ((HashMap) writable).entrySet();
        int noRSUEntries = ((HashMap) writable).size();
        for (int i = 0; i < allRSU.toArray().length; i++) {
            var RSUEntry = (Map.Entry) allRSU.toArray()[i];
            for (int j = 0; j < ((ArrayList) ((ImmutablePair) ((Map.Entry) allRSU.toArray()[i]).getValue()).getValue()).size(); j++) {
                ClusterDifference entry = (ClusterDifference) ((ArrayList) ((ImmutablePair) ((Map.Entry) allRSU.toArray()[i]).getValue()).getValue()).get(j);

                int noNeighbours = ((ArrayList) ((ImmutablePair) ((ImmutablePair) RSUEntry.getValue()).getKey()).getValue()).size();
                int noChanges = entry.getChanges().size();

                PreparedStatement insertStmt = StartINSERTtoTable(conn, "neighboursChange(unique_entry_id, rsu_id, time_of_update, neighbour1, neighbour2, neighbour3, ischange, change1, change2, change3, change4)");
                int unique_entry_ID = INSERTInt(insertStmt, 1, start_ID);
                int rsu_id = INSERTString(insertStmt, 2, (String) RSUEntry.getKey());
                int time_of_update = INSERTDouble(insertStmt, 3, (Double) ((ImmutablePair) ((ImmutablePair) RSUEntry.getValue()).getKey()).getKey());
                int neighbours1 = noNeighbours >= 1 ? INSERTString(insertStmt, 4, (String) ((ArrayList) ((ImmutablePair) ((ImmutablePair) RSUEntry.getValue()).getKey()).getValue()).toArray()[0]) : INSERTString(insertStmt, 4, "null");
                int neighbours2 = noNeighbours >= 2 ? INSERTString(insertStmt, 5, (String) ((ArrayList) ((ImmutablePair) ((ImmutablePair) RSUEntry.getValue()).getKey()).getValue()).toArray()[1]) : INSERTString(insertStmt, 5, "null");
                int neighbours3 = noNeighbours >= 3 ? INSERTString(insertStmt, 6, (String) ((ArrayList) ((ImmutablePair) ((ImmutablePair) RSUEntry.getValue()).getKey()).getValue()).toArray()[2]) : INSERTString(insertStmt, 6, "null");
                int ischange = INSERTString(insertStmt, 7, entry.getChange().name());
                int change1 =  noChanges >= 1 ? INSERTString(insertStmt, 8, (String) entry.getChanges().get(0)) : INSERTString(insertStmt, 8, "null");
                int change2 =  noChanges >= 2 ? INSERTString(insertStmt, 9, (String) entry.getChanges().get(1)) : INSERTString(insertStmt, 9, "null");
                int change3 =  noChanges >= 3 ? INSERTString(insertStmt, 10, (String) entry.getChanges().get(2)) : INSERTString(insertStmt, 10, "null");
                int change4 =  noChanges >= 4 ? INSERTString(insertStmt, 11, (String) entry.getChanges().get(3)) : INSERTString(insertStmt, 11, "null");
                boolean finishedInsert = EndINSERTtoTable(insertStmt);

                start_ID++;
            }
        }
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
