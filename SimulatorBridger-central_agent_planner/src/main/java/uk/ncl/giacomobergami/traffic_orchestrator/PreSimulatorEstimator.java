/*
 * CentralAgentPlanner.java
 * This file is part of SimulatorBridger-central_agent_planner
 *
 * Copyright (C) 2022 - Giacomo Bergami
 *
 * SimulatorBridger-central_agent_planner is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * SimulatorBridger-central_agent_planner is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with SimulatorBridger-central_agent_planner. If not, see <http://www.gnu.org/licenses/>.
 */


package uk.ncl.giacomobergami.traffic_orchestrator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.dataformat.csv.CsvWriteException;
import com.google.common.collect.Multimaps;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import io.jenetics.ext.moea.Pareto;
import io.vavr.control.Try;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import uk.ncl.giacomobergami.traffic_orchestrator.solver.CandidateSolutionParameters;
import uk.ncl.giacomobergami.traffic_orchestrator.solver.LocalTimeOptimizationProblem;
import uk.ncl.giacomobergami.traffic_orchestrator.solver.TemporalNetworkingRanking;
import uk.ncl.giacomobergami.utils.algorithms.ClusterDifference;
import uk.ncl.giacomobergami.utils.algorithms.StringComparator;
import uk.ncl.giacomobergami.utils.asthmatic.WorkloadCSV;
import uk.ncl.giacomobergami.utils.asthmatic.WorkloadCSVMediator;
import uk.ncl.giacomobergami.utils.asthmatic.WorkloadFromVehicularProgram;
import uk.ncl.giacomobergami.utils.data.CSVMediator;
import uk.ncl.giacomobergami.utils.gir.SquaredCartesianDistanceFunction;
import uk.ncl.giacomobergami.utils.pipeline_confs.OrchestratorConfiguration;
import uk.ncl.giacomobergami.utils.pipeline_confs.TrafficConfiguration;
import uk.ncl.giacomobergami.utils.shared_data.edge.Edge;
import uk.ncl.giacomobergami.utils.shared_data.edge.TimedEdge;
import uk.ncl.giacomobergami.utils.shared_data.edge.TimedEdgeMediator;
import uk.ncl.giacomobergami.utils.shared_data.edge.EdgeProgram;
import uk.ncl.giacomobergami.utils.shared_data.iot.TimedIoT;
import uk.ncl.giacomobergami.utils.shared_data.iot.TimedIoTMediator;
import uk.ncl.giacomobergami.utils.shared_data.iot.IoT;
import uk.ncl.giacomobergami.utils.shared_data.iot.IoTProgram;
import uk.ncl.giacomobergami.utils.structures.ImmutablePair;
import uk.ncl.giacomobergami.utils.structures.ReconstructNetworkInformation;

import com.opencsv.CSVWriter;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class PreSimulatorEstimator {

    Logger logger = LogManager.getRootLogger();
    private final OrchestratorConfiguration conf;
    private final TrafficConfiguration conf2;
    protected TimedEdgeMediator rsum;
    protected TimedIoTMediator vehm;
    Comparator<double[]> comparator;
    Gson gson;
    File statsFolder;
    HashMap<Double, Long> problemSolvingTime;
    CandidateSolutionParameters candidate;
    HashMap<String, IoT> reconstructVehicles;
    ReconstructNetworkInformation timeEvolvingEdges;
    SquaredCartesianDistanceFunction f;
    List<String> tls_s;
    HashMap<Double, HashMap<String, Integer>> belongingMap;

    public PreSimulatorEstimator(OrchestratorConfiguration conf, TrafficConfiguration conf2) {
        logger.info("=== CENTRAL AGENT PLANNER ===");
        logger.trace("CENTRAL AGENT PLANNER: init");
        this.conf = conf;
        this.conf2 = conf2;
        rsum = new TimedEdgeMediator();
        vehm = new TimedIoTMediator();
        gson = new GsonBuilder().setPrettyPrinting().create();
        statsFolder = new File(conf.output_stats_folder);
        candidate = null;
        if (conf.use_pareto_front) {
            comparator = Pareto::dominance;
        } else {
            comparator = Comparator.comparingDouble(o -> o[0] * conf.p1 + o[1] * conf.p2 + o[2] * (1 - conf.p1  - conf.p2));
        }
        problemSolvingTime = new HashMap<>();
        reconstructVehicles = new HashMap<>();
        timeEvolvingEdges = null;
        f = SquaredCartesianDistanceFunction.getInstance();
        tls_s = null;
        belongingMap = new HashMap<>();
    }

    protected boolean write_json(File folder, String filename, Object writable)  {
        if (!folder.exists()) {
            folder.mkdirs();
        }
        if (folder.exists() && folder.isDirectory()) {
            try {
                Files.writeString(Paths.get(new File(/*folder, conf.experiment_name+"_"*/filename).getAbsolutePath()), gson.toJson(writable));
                return true;
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }
        return false;
    }

    protected void write_large_json(File folder, String filename, Object writable){
        if (!folder.exists()) {
            folder.mkdirs();
        }
        if (folder.exists() && folder.isDirectory()) {
            ObjectWriter objectWriter = new ObjectMapper().writer().withDefaultPrettyPrinter();
            Try.withResources(() -> new FileWriter(filename))
                    .of(fileWriter -> Try.withResources(() -> objectWriter.writeValues(fileWriter))
                            .of(sequenceWriter -> sequenceWriter.write(writable)))
                    .get();
        }
    }

    protected boolean write_csv(File folder, String filename, Object writable) throws IOException {
        if (!folder.exists()) {
            folder.mkdirs();
        }
        if (folder.exists() && folder.isDirectory()) {
            toCSV(writable, filename);
            return true;
        }
        return false;
    }

    private static void toCSV(Object object, String filePath) throws IOException {
        try(CSVWriter writer = new CSVWriter(new FileWriter(filePath))) {
            String[] headers = {"id", "x", "y", "angle", "type", "speed", "pos", "lane", "slope", "simtime"};
            writer.writeNext(headers);
            for(int i = 0; i < ((HashMap)object).values().size(); i++){
                for (int j = 0; j < ((IoT)((HashMap)object).values().toArray()[i]).getDynamicInformation().values().size(); j++) {
                    String[] data = getStrings((HashMap)object, i, j);
                    writer.writeNext(data);
                }
            }
        }catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String[] getStrings(HashMap object, int i, int j) {
        TimedIoT vehicle = (TimedIoT) ((IoT) object.values().toArray()[i]).getDynamicInformation().values().toArray()[j];
        String[] data = {String.valueOf(vehicle.getId()), String.valueOf(vehicle.getX()), String.valueOf(vehicle.getY()), String.valueOf(vehicle.getAngle()), String.valueOf(vehicle.getType()), String.valueOf(vehicle.getSpeed()), String.valueOf(vehicle.getPos()), String.valueOf(vehicle.getLane()), String.valueOf(vehicle.getSlope()), String.valueOf(vehicle.getSimtime())};
        return data;
    }

    protected ReconstructNetworkInformation readEdges() {
        return ReconstructNetworkInformation.fromFiles(new File(conf2.RSUCsvFile+"_timed_scc.json").getAbsoluteFile(),
                new File(conf2.RSUCsvFile+"_neighboursChange.json").getAbsoluteFile(),
                new File(conf.RSUCsvFile) );
    }

    protected TreeMap<Double, List<TimedIoT>> readIoT() {
        var reader = vehm.beginCSVRead(new File(conf.vehicleCSVFile));
        TreeMap<Double, List<TimedIoT>> map = new TreeMap<>();
        while (reader.hasNext()) {
            var curr = reader.next();
            if (!map.containsKey(curr.simtime))
                map.put(curr.simtime, new ArrayList<>());
            map.get(curr.simtime).add(curr);
        }
        try {
            reader.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return map;
    }

    public void run() {
        logger.trace("CENTRAL AGENT PLANNER: running");
        candidate = null;
        timeEvolvingEdges = readEdges();
        HashSet<String> vehId = new HashSet<>();
        problemSolvingTime.clear();
        List<Double> temporalOrdering = new ArrayList<>();
        HashMap<Set<String>, Integer> distinct_scc_mapping = new HashMap<>();
        belongingMap.clear();
        reconstructVehicles.clear();
        HashMap<Double, ArrayList<LocalTimeOptimizationProblem.Solution>> simulationSolutions = new HashMap<>();
        var vehSet = readIoT().entrySet();
        if (vehSet.isEmpty()) {
            logger.warn("WARNING: vehicles are empty!");
            return;
        }
        for (var simTimeToVehicles : vehSet) {
            logger.info(simTimeToVehicles.getKey());
            if (!timeEvolvingEdges.hasNext()) {
                throw new RuntimeException("ERROR: the TLS should have the same timing of the Vehicles");
            }
            var current = timeEvolvingEdges.next();
            {
                var tmpMap = Multimaps.asMap(current.edgeToSCC);
                HashMap<String, Integer> bmT = new HashMap<>();
                belongingMap.put(simTimeToVehicles.getKey(), bmT);
                for (var set : tmpMap.values()) {
                    var sS = new HashSet<String>();
                    for (var x : set)
                        sS.add(x.getId());
                    Integer sccId = distinct_scc_mapping.computeIfAbsent(sS, strings -> distinct_scc_mapping.size());
                    for (var x : sS)
                        bmT.put(x, sccId);
                }
            }
            simTimeToVehicles.getValue().forEach(x -> vehId.add(x.id));
            var currTime = simTimeToVehicles.getKey();
            List<TimedIoT> vehs2 = simTimeToVehicles.getValue();
           for (var tv : vehs2) {
               if (!reconstructVehicles.containsKey(tv.id)) {
                   reconstructVehicles.put(tv.id, new IoT());
               }
               reconstructVehicles.get(tv.id).dynamicInformation.put(currTime, tv);
           }
            LocalTimeOptimizationProblem solver = new LocalTimeOptimizationProblem(vehs2, current);
            if (solver.init()) {
                if (conf.do_thresholding) {
                    if (conf.use_nearest_MEL_to_IoT) {
                        solver.setNearestFirstMileMELForIoT();
                    } else {
                        solver.setAllPossibleFirstMileMELForIoT();
                    }

                    if (conf.use_greedy_algorithm) {
                        solver.setGreedyPossibleTargetsForIoT(conf.use_local_demand_forecast);
                    } else if (conf.use_top_k_nearest_targets > 0) {
                        solver.setAllPossibleNearestKTargetsForLastMileCommunication(conf.use_top_k_nearest_targets, conf.use_top_k_nearest_targets_randomOne);
                    } else {
                        solver.setAllPossibleTargetsForLastMileCommunication();
                    }
                } else {
                    solver.alwaysCommunicateWithTheNearestMel();
                }

                ArrayList<LocalTimeOptimizationProblem.Solution> sol =
                        solver.multi_objective_pareto(conf.k1, conf.k2, conf.ignore_cubic, comparator, conf.reduce_to_one, conf.update_after_flow, conf.use_scc_neighbours);

                problemSolvingTime.put(currTime, solver.getRunTime());
                simulationSolutions.put(currTime, sol);
                temporalOrdering.add(currTime);
            }
        }

        tls_s = new ArrayList<>(timeEvolvingEdges.getEdgeNodeForReconstruction().keySet());
        List<String> veh_s = new ArrayList<>(vehId);

        logger.trace("Computing all of the possible Pareto Routing scenarios...");

        if (simulationSolutions.values().stream().anyMatch(ArrayList::isEmpty)) {
            logger.warn("NO viable solution found!");
        } else {
            Double bestResultScore = Double.MAX_VALUE;

            candidate = new CandidateSolutionParameters();
            var multiplicity = simulationSolutions.values().stream().mapToInt(ArrayList::size).reduce((a, b) -> a * b)
                    .orElse(0);
            logger.info("Multiplicity: " + multiplicity);
            long timedBegin = System.currentTimeMillis();
            if (conf.clairvoyance) {
                TemporalNetworkingRanking.oracularBestNetworking(simulationSolutions, temporalOrdering, veh_s, bestResultScore, candidate, conf.removal, conf.addition, comparator);
            } else {
                TemporalNetworkingRanking.nonclairvoyantBestNetworking(simulationSolutions, temporalOrdering, veh_s, bestResultScore, candidate, conf.removal, conf.addition, comparator);
            }
            candidate.networkingRankingTime = (System.currentTimeMillis()- timedBegin);

            // SETTING UP THE VEHICULAR PROGRAMS
            for (var veh : vehId) {
                var vehProgram = new IoTProgram(candidate.delta_associations.get(veh));
                for (var entry : candidate.bestResult.entrySet()) {
                    vehProgram.putDeltaRSUAssociation(entry.getKey(), entry.getValue().slowRetrievePath(veh));
                }
                vehProgram.finaliseProgram();
                reconstructVehicles.get(veh).program = vehProgram;
            }

            TreeMap<Double, Map<String, List<String>>> networkTopology = new TreeMap<>(); // Actually, for RSU programs: saving one iteration cycle
            for (var entry : candidate.bestResult.entrySet()) {
                var npMap = entry.getValue()
                        .RSUNetworkNeighbours
                        .entrySet()
                        .stream()
                        .map(x -> new ImmutablePair<>(x.getKey().id,
                                x.getValue().stream().map(y -> y.id).collect(Collectors.toList())))
                        .collect(Collectors.toMap(Pair::getKey, Pair::getValue));
                networkTopology.put(entry.getKey(), npMap);
                for (var vehs : entry.getValue().getAlphaAssociation()) {
                    reconstructVehicles.get(vehs.getKey().id).program.setLocalInformation(entry.getKey(), vehs.getKey());
                }
            }

            // SETTING UP THE RSU PROGRAMS
            // This concept is relevant, so if we need to remove some nodes from the simulation,
            // and to add others. This also defines with which MELs and Vehicles should an element connect/disconnect
            // for its routing
            var delta_clusters = ClusterDifference.computeTemporalDifference(candidate.inStringTime, tls_s, StringComparator.getInstance());
            var delta_network_neighbours = ClusterDifference.computeTemporalDifference(networkTopology, tls_s, StringComparator.getInstance());

            for (var cp : timeEvolvingEdges.getEdgeNodeForReconstruction().entrySet()) {
                var r = cp.getValue();
                var id = cp.getKey();
                var rsuProgram = new EdgeProgram(candidate.bestResult.keySet());
                rsuProgram.finaliseProgram(delta_clusters.get(id), delta_network_neighbours.get(id));
                r.setProgram(rsuProgram);
            }
        }
    }

    public void serializeAll() {
        logger.trace("Serializing data...");
        logger.trace(" * solver_time ");
        write_json(statsFolder, new File(statsFolder.getAbsoluteFile(),"solver_time.json").toString(), problemSolvingTime);

        logger.trace(" * candidate solution ");
        write_large_json(statsFolder, new File(statsFolder.getAbsoluteFile(),"candidate.json").toString(), candidate);

        logger.trace(" * reconstructed vehicles ");
        write_large_json(statsFolder, conf.vehiclejsonFile, reconstructVehicles);

        logger.trace(" * RSU Programs ");
        write_json(statsFolder, conf.RSUJsonFile, timeEvolvingEdges.getEdgeNodeForReconstruction());

        logger.trace(" * Time for problem solving ");
        try {
            FileOutputStream tlsF = new FileOutputStream(Paths.get(statsFolder.getAbsolutePath(), conf.experiment_name+"_time_benchmark.csv").toFile());
            BufferedWriter flsF2 = new BufferedWriter(new OutputStreamWriter(tlsF));
            flsF2.write("sim_time,bench_time");
            flsF2.newLine();
            for (var x : problemSolvingTime.entrySet()) {
                flsF2.write(x.getKey()+","+x.getValue());
                flsF2.newLine();
            }
            flsF2.close();
            tlsF.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        logger.trace(" * Just Last Mile Occupancy ");
        try {
            FileOutputStream tlsF = new FileOutputStream(Paths.get(statsFolder.getAbsolutePath(), conf.experiment_name+"_tracesMatch_toplot.csv").toFile());
            BufferedWriter flsF2 = new BufferedWriter(new OutputStreamWriter(tlsF));
            flsF2.write("SimTime,Sem,NVehs");
            flsF2.newLine();
            List<TimedIoT> e = Collections.emptyList();
            if ((candidate != null) && (candidate.inCurrentTime != null))
            for (var cp : candidate.inCurrentTime.entrySet()) {
                Double time = cp.getKey();
                for (var sem_cp : timeEvolvingEdges.getEdgeNodeForReconstruction().entrySet()) {
                    var sem_id = sem_cp.getKey();
                    var sem_ls = sem_cp.getValue();
                    flsF2.write(time+","+sem_id +","+cp.getValue().getOrDefault(
                            sem_ls.dynamicInformation.get(time), e).size());
                    flsF2.newLine();
                }
            }
            flsF2.close();
            tlsF.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        logger.trace(" * RSU/EDGE communication devices infrastructure");
        logger.trace("  - [WorkloadCSV]");
        var vehicularConverterToWorkflow = new WorkloadFromVehicularProgram(null);
        AtomicInteger ai = new AtomicInteger();
        CSVMediator<WorkloadCSV>.CSVWriter x = new WorkloadCSVMediator().beginCSVWrite(new File(statsFolder, "AsmathicWorkflow.csv"));
        reconstructVehicles.entrySet().stream()
                        .flatMap((Map.Entry<String, IoT> k) ->{
                            vehicularConverterToWorkflow.setNewVehicularProgram(k.getValue().getProgram());
                            return vehicularConverterToWorkflow.generateFirstMileSpecifications(conf2.step, ai, belongingMap).stream();
                        }).forEach(x::write);
        try {
            x.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
