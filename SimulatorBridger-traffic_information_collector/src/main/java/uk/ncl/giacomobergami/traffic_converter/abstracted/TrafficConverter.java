package uk.ncl.giacomobergami.traffic_converter.abstracted;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jooq.BatchBindStep;
import org.jooq.DSLContext;
import uk.ncl.giacomobergami.utils.algorithms.ClusterDifference;
import uk.ncl.giacomobergami.utils.algorithms.StringComparator;
import uk.ncl.giacomobergami.utils.algorithms.Tarjan;
import uk.ncl.giacomobergami.utils.data.CSVMediator;
import uk.ncl.giacomobergami.utils.database.jooq.tables.Neighbourschange;
import uk.ncl.giacomobergami.utils.database.jooq.tables.TimedScc;
import uk.ncl.giacomobergami.utils.pipeline_confs.TrafficConfiguration;
import uk.ncl.giacomobergami.utils.shared_data.edge.TimedEdge;
import uk.ncl.giacomobergami.utils.shared_data.edge.TimedEdgeMediator;
import uk.ncl.giacomobergami.utils.shared_data.iot.TimedIoT;
import uk.ncl.giacomobergami.utils.shared_data.iot.TimedIoTMediator;
import uk.ncl.giacomobergami.utils.structures.ImmutablePair;
import uk.ncl.giacomobergami.utils.structures.StraightforwardAdjacencyList;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;


import static uk.ncl.giacomobergami.utils.database.JavaPostGres.*;

public abstract class TrafficConverter {

    private final String RSUCsvFile;
    protected final String vehicleCSVFile;
    private final TrafficConfiguration conf;
    protected TimedEdgeMediator rsum;
    protected TimedIoTMediator vehm;
    protected CSVMediator<TimedEdge>.CSVWriter rsuwrite;
    protected CSVMediator<TimedIoT>.CSVWriter vehwrite;
    private static Logger logger = LogManager.getRootLogger();
    private static Gson gson;

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

    public boolean run(Connection conn, DSLContext context) throws SQLException {
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
        System.out.print("Starting collection and upload of traffic information to SQL database...\n");
        for (Double tick : timeUnits) {
            // Writing IoT Devices
            /*if(conf.isOutputVehicleCsvFile()) {
                getTimedIoT(tick).forEach(this::writeTimedIoT);
            }*/
            // Getting all of the IoT Devices
            HashSet<TimedEdge> allEdgeNodes = getTimedEdgeNodes(tick);
            allEdgeNodes.forEach(x -> {
                allTlsS.add(x.getId());
                if(conf.isOutputRSUCsvFile()) {
                    writeTimedEdge(x);
                }
                timedEdgeFullSet.add(x);
            });
            StraightforwardAdjacencyList<String> network = getTimedEdgeNetwork(tick);

            var scc = new Tarjan<String>().run(network, allEdgeNodes.stream().map(TimedEdge::getId).toList());
            sccPerTimeComponent.put(tick, scc);
            timedNodeAdjacency.put(tick, network.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, x -> new ArrayList<>(x.getValue()))));
        }

        logger.trace("Dumping the last results...");
        HashMap<String, ImmutablePair<ImmutablePair<Double, List<String>>, List<ClusterDifference<String>>>> delta_network_neighbours = ClusterDifference.computeTemporalDifference(timedNodeAdjacency, allTlsS, StringComparator.getInstance());

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
        //closeWritingTimedIoT();
        closeWritingTimedEdge();
        logger.trace("Transferring results to SQL Database...");
        write_to_SQL(conn, context,  true, true, sccPerTimeComponent, true, delta_network_neighbours, true);
        logger.trace("quitting...");
        endReadSimulatorOutput();
        System.out.print("Traffic information uploaded to SQL database\n");
        logger.info("=========================");
        return true;
    }

    protected void write_to_SQL(Connection conn, DSLContext context, boolean deleteIoTSQLData, boolean deleteEdgeSQLData, Object Timed_SCCData, boolean deleteTimed_SCCData, Object NeighbourData, boolean deleteNeighbourData) {
        if (deleteIoTSQLData) emptyTABLE(conn, "vehInformation");
        INSERTTimedIoTData(conn);
        indexVEHINFORMATION(conn);
        emptyTABLE(conn, "vehInformation_import");
        if (deleteEdgeSQLData) emptyTABLE(conn, "rsuInformation");
        INSERTTimedEdgeData(conn);
        emptyTABLE(conn, "rsuInformation_import");
        if (deleteTimed_SCCData) emptyTABLE(conn, "timed_scc");
        INSERTTimed_SCCData(conn, context, Timed_SCCData);
        if (deleteNeighbourData) emptyTABLE(conn, "neighboursChange");
        INSERTNeighbourData(conn, context, NeighbourData);
    }

    protected void INSERTTimedIoTData(Connection conn) {
        String targetTABLE = "vehInformation";
        if (TABLEsize(conn, targetTABLE) != 0) {
            return;
        }
        System.out.print("Organising vehInformation Data...\n");
        long startTime = System.nanoTime();
        copyCSVDATA(conn, vehicleCSVFile, targetTABLE);
        transferDATABetweenTables(conn, "vehInformation (vehicle_ID,x,y,angle,vehicle_type,speed,pos,lane,slope,simtime)",
                "vehicle_ID,x,y,angle,vehicle_type,speed,pos,lane,slope,simtime", targetTABLE);
        long endTime = System.nanoTime();
        long executionTime = (endTime - startTime) / 1000000;
        System.out.print("Sending vehInformation to SQL Database\n");
        System.out.println("This takes " + executionTime + "ms");
    }

    protected void INSERTTimedEdgeData(Connection conn) {

        String targetTABLE = "rsuInformation";
        if (TABLEsize(conn, targetTABLE) != 0) {
            return;
        }
        System.out.print("Organising rsuInformation Data...\n");
        long startTime = System.nanoTime();
        copyCSVDATA(conn, RSUCsvFile, targetTABLE);
        transferDATABetweenTables(conn, "rsuInformation(rsu_id, x, y, simtime, communication_radius, max_vehicle_communication)",
                "rsu_id, x, y, simtime, communication_radius, max_vehicle_communication", targetTABLE);
        long endTime = System.nanoTime();
        long executionTime = (endTime - startTime) / 1000000;
        System.out.print("Sending rsuInformation to SQL Database\n");
        System.out.println("This takes " + executionTime + "ms");
    }

    protected void INSERTTimed_SCCData(Connection conn, DSLContext context ,Object writable) {

        if (TABLEsize(conn, "timed_scc") != 0) {
            return;
        }
        System.out.print("Organising timed_SCC Data...\n");
        long startTime = System.nanoTime();
        var query = context.insertInto(TimedScc.TIMED_SCC, TimedScc.TIMED_SCC.UNIQUE_ENTRY_ID, TimedScc.TIMED_SCC.TIME_OF_UPDATE,
                        TimedScc.TIMED_SCC.NETWORKNEIGHBOURS1, TimedScc.TIMED_SCC.NETWORKNEIGHBOURS2,
                        TimedScc.TIMED_SCC.NETWORKNEIGHBOURS3, TimedScc.TIMED_SCC.NETWORKNEIGHBOURS4)
                .values((Integer) null, (Double) null, (String) null, (String) null, (String) null, (String) null);
        int start_ID = 1;
        int k = 0;
        int batchSize = 25000;
        BatchBindStep step = context.batch(query);

        for (Map.Entry<Double, ArrayList> timeEntry : ((TreeMap<Double, ArrayList>) writable).entrySet()) {
            int noNeighbourEntries = timeEntry.getValue().size();
            for (int i = 0; i < noNeighbourEntries; i++) {
                ArrayList entry  = (ArrayList) timeEntry.getValue().get(i);
                int noNeighbours = entry.size();
                step.bind(start_ID, timeEntry.getKey(), noNeighbours >= 1 ? entry.get(0) : "null", noNeighbours >= 2 ? entry.get(1) : "null",
                        noNeighbours >= 3 ? entry.get(2) : "null", noNeighbours >= 4 ? entry.get(3) : "null");
                start_ID++;
            }
            if (step.size() >= batchSize || k + 1 == ((TreeMap<Double, ArrayList>) writable).entrySet().size()) {
                step.execute();
                step = context.batch(query);
            }
            k++;
        }
        long endTime = System.nanoTime();
        long executionTime = (endTime - startTime) / 1000000;
        System.out.print("Sending timed_SCC to SQL Database\n");
        System.out.println("This takes " + executionTime + "ms");
    }

    protected void INSERTNeighbourData(Connection conn, DSLContext context, Object writable) {

        if (TABLEsize(conn, "neighboursChange") != 0) {
            return;
        }
        System.out.print("Organising neighboursInfo Data...\n");
        long startTime = System.nanoTime();
        var query = context.insertInto(Neighbourschange.NEIGHBOURSCHANGE, Neighbourschange.NEIGHBOURSCHANGE.UNIQUE_ENTRY_ID, Neighbourschange.NEIGHBOURSCHANGE.RSU_ID,
                        Neighbourschange.NEIGHBOURSCHANGE.TIME_OF_UPDATE, Neighbourschange.NEIGHBOURSCHANGE.NEIGHBOUR1, Neighbourschange.NEIGHBOURSCHANGE.NEIGHBOUR2,
                        Neighbourschange.NEIGHBOURSCHANGE.NEIGHBOUR3, Neighbourschange.NEIGHBOURSCHANGE.ISCHANGE, Neighbourschange.NEIGHBOURSCHANGE.CHANGE1,
                        Neighbourschange.NEIGHBOURSCHANGE.CHANGE2, Neighbourschange.NEIGHBOURSCHANGE.CHANGE3, Neighbourschange.NEIGHBOURSCHANGE.CHANGE4)
                .values((Integer) null, (String) null, (Double) null, (String) null, (String) null, (String) null, (String) null,
                        (String) null, (String) null, (String) null, (String) null);
        int start_ID = 1;
        //int batchNo = 1;
        int batchSize = 25000;//Math.min(((TreeMap<Double, ArrayList>) writable).size(), 25000);
        BatchBindStep step = context.batch(query);
        //ProgressBar pb = new ProgressBar("neighboursInfo batch 1", batchSize);
        var allRSU = ((HashMap) writable).entrySet();
        for (int i = 0; i < allRSU.toArray().length; i++) {
            var RSUEntry = (Map.Entry) allRSU.toArray()[i];
            for (int j = 0; j < ((ArrayList) ((ImmutablePair) ((Map.Entry) allRSU.toArray()[i]).getValue()).getValue()).size(); j++) {
                ClusterDifference entry = (ClusterDifference) ((ArrayList) ((ImmutablePair) ((Map.Entry) allRSU.toArray()[i]).getValue()).getValue()).get(j);

                int noNeighbours = ((ArrayList) ((ImmutablePair) ((ImmutablePair) RSUEntry.getValue()).getKey()).getValue()).size();
                int noChanges = entry.getChanges().size();

                step.bind(start_ID, (String) RSUEntry.getKey(), (Double) ((ImmutablePair) ((ImmutablePair) RSUEntry.getValue()).getKey()).getKey(),
                        noNeighbours >= 1 ? (String) ((ArrayList) ((ImmutablePair) ((ImmutablePair) RSUEntry.getValue()).getKey()).getValue()).toArray()[0] : "null",
                        noNeighbours >= 2 ? (String) ((ArrayList) ((ImmutablePair) ((ImmutablePair) RSUEntry.getValue()).getKey()).getValue()).toArray()[1] : "null",
                        noNeighbours >= 3 ? (String) ((ArrayList) ((ImmutablePair) ((ImmutablePair) RSUEntry.getValue()).getKey()).getValue()).toArray()[2] : "null",
                        entry.getChange().name(), noChanges >= 1 ? (String) entry.getChanges().get(0) : "null", noChanges >= 2 ? (String) entry.getChanges().get(1) : "null",
                        noChanges >= 3 ? (String) entry.getChanges().get(2) : "null", noChanges >= 4 ? (String) entry.getChanges().get(3) : "null");
                //pb.step();
                start_ID++;
            }
            if (step.size() >= batchSize || i + 1 == allRSU.toArray().length) {
                //System.out.print("Sending neighboursInfo to SQL Database from batch " + batchNo + "\n");
                //pb.stepTo(step.size());
                step.execute();
                step = context.batch(query);
                /*if(i + 1 < allRSU.toArray().length) {
                    batchNo++;
                    //pb = new ProgressBar("neighboursInfo batch " + batchNo, batchSize);
                }*/
            }
        }
        long endTime = System.nanoTime();
        long executionTime = (endTime - startTime) / 1000000;
        System.out.print("Sending neighboursInfo to SQL Database\n");
        System.out.println("This takes " + executionTime + "ms");
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
