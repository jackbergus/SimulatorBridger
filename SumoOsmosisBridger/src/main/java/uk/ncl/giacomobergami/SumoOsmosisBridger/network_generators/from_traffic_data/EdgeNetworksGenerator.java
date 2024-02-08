package uk.ncl.giacomobergami.SumoOsmosisBridger.network_generators.from_traffic_data;

import com.google.common.collect.HashMultimap;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.jooq.DSLContext;
import uk.ncl.giacomobergami.SumoOsmosisBridger.network_generators.EdgeInfrastructureGenerator;
import uk.ncl.giacomobergami.utils.algorithms.ClusterDifference;
import uk.ncl.giacomobergami.utils.algorithms.ReconstructorIterator;
import uk.ncl.giacomobergami.utils.algorithms.StringComparator;
import uk.ncl.giacomobergami.utils.data.YAML;
import uk.ncl.giacomobergami.utils.database.jooq.tables.Rsuinformation;
import uk.ncl.giacomobergami.utils.database.jooq.tables.records.RsuinformationRecord;
import uk.ncl.giacomobergami.utils.pipeline_confs.TrafficConfiguration;
import uk.ncl.giacomobergami.utils.shared_data.edge.Edge;
import uk.ncl.giacomobergami.utils.shared_data.edge.TimedEdge;
import uk.ncl.giacomobergami.utils.structures.ImmutablePair;
import uk.ncl.giacomobergami.utils.structures.MutablePair;

import javax.sql.DataSource;
import java.io.*;
import java.lang.reflect.Type;
import java.sql.Connection;
import java.util.*;

import static org.jooq.impl.DSL.field;
import static uk.ncl.giacomobergami.utils.database.JavaPostGres.*;

public class EdgeNetworksGenerator {

    /**
     * Detecting the mobility in the connectivity network
     */
    public HashMap<ImmutablePair<Double, Double>, Set<Set<String>>> css_in_time;
    public HashMap<Double, HashMap<String, TimedEdge>> retrieved_basic_information;
    public HashMap<Double, HashMultimap<String, String>> timed_connectivity;
    public List<ImmutablePair<Double, Double>> chron;
    public TreeSet<MutablePair<Double, Double>> simulation_intervals;
    private static final File converter_file = new File("clean_example/converter.yaml");
    protected static final Optional<TrafficConfiguration> time_conf = YAML.parse(TrafficConfiguration.class, converter_file);

    public TimedEdge retriveEdgeLocationInTime(double time, String rsu) {
        var map = retrieved_basic_information.get(time);
        if (map == null) return null;
        return map.get(rsu);
    }

    public EdgeNetworksGenerator(File scc_json,
                                 File rsu_json,
                                 File neigh_json,
                                 TimeTicker traffic_simulator_ticker,
                                 DSLContext context,
                                 boolean isRSUJSON, boolean movingEdges) {

        simulation_intervals = new TreeSet<MutablePair<Double, Double>>((o1, o2) -> {
            if (o1 == o2)
                return 0;
            else if (o1 == null)
                return -1;
            else if (o2 == null)
                return 1;
            else {
                var cmp = o1.getLeft().compareTo(o2.getLeft());
                if (cmp != 0) return cmp;
                return o1.getRight().compareTo(o2.getRight());
            }
        });
        css_in_time = new HashMap<>();
        chron = traffic_simulator_ticker.getChron();
        timed_connectivity = new HashMap<>();
        Type sccType = new TypeToken<TreeMap<Double, Set<Set<String>>>>() {}.getType();
        Type sccType2 = new TypeToken<HashMap<String, Edge>>() {}.getType();
        Type networkType = new TypeToken<HashMap<String, ImmutablePair<ImmutablePair<Double, List<String>>, List<ClusterDifference<String>>>>>() {}.getType();
        Gson gson = new Gson();

        BufferedReader reader1 = null, reader2 = null,reader3 = null;

        retrieved_basic_information = new HashMap<>();
        try {
            reader1 = new BufferedReader(new FileReader(scc_json.getAbsoluteFile()));
            if(isRSUJSON) {
                reader2 = new BufferedReader(new FileReader(rsu_json.getAbsoluteFile()));
            }
            reader3 = new BufferedReader(new FileReader(neigh_json.getAbsoluteFile()));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            System.exit(1);
        }
        {
            // Getting the static information of the SCCs
            TreeMap<Double, Set<Set<String>>> subnets_in_time;
            subnets_in_time = gson.fromJson(reader1, sccType);
            HashMap<Set<Set<String>>, TreeSet<Double>> elements = new HashMap<>();
            subnets_in_time.forEach((x,y)-> elements.computeIfAbsent(y, sets -> new TreeSet<>()).add(x));
            subnets_in_time.clear();
            elements.forEach((x, s) -> {
                List<ImmutablePair<Double, Double>> ls = new ArrayList<>();
                if (s.size() == 1) {
                    var f = s.first();
                    ls.add(traffic_simulator_ticker.reconstructIntervals(f, f));
                } else {
                    Queue<Double> cp = new LinkedList<>(s);
                    var curr = cp.poll();
                    while (!cp.isEmpty()) {
                        var next = cp.poll();
                        if(!time_conf.get().getIsBatch()) {
                            ls.add(traffic_simulator_ticker.reconstructIntervals(curr, next));
                        }else {
                            if (curr >= time_conf.get().getBatchStart() && next <= time_conf.get().getBatchEnd()) {
                                ls.add(traffic_simulator_ticker.reconstructIntervals(curr, next));
                            }
                        }
                        curr = next;
                    }
                }
                simulation_intervals.addAll(TimeTicker.mergeIntervals(ls));
                ls.forEach(interval -> css_in_time.put(interval, x));
            });

            // Retrieving the geoloc of the RSU
            TreeSet<Double> ticks;
            System.out.print("Starting RSU Information Retrieval...\n");
            if(isRSUJSON) {
                HashMap<String, Edge> edges_in_time = gson.fromJson(reader2, sccType2);
                edges_in_time.forEach((k, v) -> {
                    v.dynamicInformation.forEach((c, a) -> {
                        retrieved_basic_information.computeIfAbsent(c, any -> new HashMap<>()).put(k, a);
                    });
                });
                edges_in_time.clear();

                ticks = new TreeSet<>(retrieved_basic_information.keySet());
            } else {
                var rsuTimeData = context.select(field("simtime")).distinctOn(field("simtime")).from(Rsuinformation.RSUINFORMATION).fetch();
                ticks = (TreeSet<Double>) new TreeSet<>(rsuTimeData.getValues(0));
                System.out.print("Fetching RSU data from SQL table...\n");
                var allRSUData = context.select().from(Rsuinformation.RSUINFORMATION).where("simtime = 0.0").orderBy(field("simtime")).fetch();
                System.out.print("RSU data fetched\n");
                System.out.print("Starting organisation of RSU data...\n");
                int noRSU = Math.max(allRSUData.size()/ticks.size(),16);
                int iterateSize = movingEdges ? ticks.size() : 1;
                for(int j = 0; j < iterateSize; j++) {
                    HashMap<String, TimedEdge> rbi_entry = new HashMap<>();
                    RsuinformationRecord entry;
                    double currentTime = 0.0;
                    for (int i = 0; i < noRSU; i++) {
                        long k = (j * noRSU) + i;
                        entry = ((RsuinformationRecord) allRSUData.toArray()[(int) k]);
                        TimedEdge currentRSUInfo = new TimedEdge(entry.getRsuId(), entry.getX(), entry.getY(), entry.getCommunicationRadius(), entry.getMaxVehicleCommunication(), entry.getSimtime());
                        rbi_entry.put(entry.getRsuId(), currentRSUInfo);
                        currentTime = entry.getSimtime();
                    }
                    double entryTime = Math.ceil(time_conf.get().getBatchStart()/time_conf.get().getStep()) * time_conf.get().getStep();
                    currentTime = time_conf.get().getIsBatch() ? entryTime : currentTime;
                    retrieved_basic_information.put(currentTime, rbi_entry);
                }
                System.out.print("RSU data organised\n");
            }
            // Reconstructing the edges' neighbours
            System.out.print("Retrieved RSU Information\n");
            HashMap<String, ImmutablePair<ImmutablePair<Double, List<String>>, List<ClusterDifference<String>>>>
                    adjacencyListVariationInTime =  gson.fromJson(reader3, networkType);
            for (var cp: adjacencyListVariationInTime.entrySet()) {
                var it = new ReconstructorIterator<>(cp.getValue(), StringComparator.getInstance());
                for (Double tick : ticks) {
                    if (!it.hasNext()) throw new RuntimeException("ERROR!");
                    timed_connectivity.computeIfAbsent(tick, any -> HashMultimap.create()).putAll(cp.getKey(), it.next());
                }
            }
        }
        try {
            reader1.close();
            if(reader2 != null) {
                reader2.close();
            }
            reader3.close();
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    /*public void updateEdgeDevice(EdgeDevice device, double lower, double upper) {
        var retrieve = retriveEdgeLocationInTime(lower, device.getDeviceName());
        if (retrieve == null) return;
        device.location.x = retrieve.x;
        device.location.y = retrieve.y;
        device.signalRange = retrieve.communication_radius;
        device.max_vehicle_communication = retrieve.max_vehicle_communication;
    }*/

    public static void main(String args[]) {
        DataSource dataSource = createDataSource();
        Connection conn = ConnectToSource(dataSource);
        DSLContext context = getDSLContext(conn);

        boolean isRSUJSON = false;


        boolean isMovingEdges = false;
        new EdgeNetworksGenerator(
                new File("/home/giacomo/IdeaProjects/SimulatorBridger/rsu.csv_timed_scc.json"),
                new File("/home/giacomo/IdeaProjects/SimulatorBridger/stats/test_rsu.json"),
                new File("/home/giacomo/IdeaProjects/SimulatorBridger/rsu.csv_neighboursChange.json"),
                new TimeTicker(0, 100, 1), context, false, isMovingEdges);
    }
}
