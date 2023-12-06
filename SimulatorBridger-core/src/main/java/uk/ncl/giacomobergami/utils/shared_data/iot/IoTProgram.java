package uk.ncl.giacomobergami.utils.shared_data.iot;

import uk.ncl.giacomobergami.utils.algorithms.ClusterDifference;
import uk.ncl.giacomobergami.utils.shared_data.abstracted.SimulationProgram;
import uk.ncl.giacomobergami.utils.shared_data.edge.TimedEdge;
import uk.ncl.giacomobergami.utils.structures.ImmutablePair;
import uk.ncl.giacomobergami.utils.structures.Union2;

import java.util.*;

public class IoTProgram implements SimulationProgram {

    public void setLocalInformation(Double key, TimedIoT key1) {
        pathingAtEachSimulationTime.get(key).localInformation = key1;
    }

    public class ProgramDetails {
        public final List<Union2<TimedIoT, TimedEdge>> shortest_path;
        public boolean isStartingProgram;
        public List<String> setInitialClusterConnection;
        public ClusterDifference<String> setConnectionVariation;
        public TimedIoT localInformation;

        public List<Union2<TimedIoT, TimedEdge>> getShortest_path() {
            return shortest_path;
        }

        public boolean isStartingProgram() {
            return isStartingProgram;
        }

        public void setStartingProgram(boolean startingProgram) {
            isStartingProgram = startingProgram;
        }

        public List<String> getSetInitialClusterConnection() {
            return setInitialClusterConnection;
        }

        public void setSetInitialClusterConnection(List<String> setInitialClusterConnection) {
            this.setInitialClusterConnection = setInitialClusterConnection;
        }

        public ClusterDifference<String> getSetConnectionVariation() {
            return setConnectionVariation;
        }

        public void setSetConnectionVariation(ClusterDifference<String> setConnectionVariation) {
            this.setConnectionVariation = setConnectionVariation;
        }

        public TimedIoT getLocalInformation() {
            return localInformation;
        }

        public void setLocalInformation(TimedIoT localInformation) {
            this.localInformation = localInformation;
        }

        public ProgramDetails(List<Union2<TimedIoT, TimedEdge>> shortest_path) {
            this.shortest_path = shortest_path;
            isStartingProgram = false;
            setInitialClusterConnection = null;
            setConnectionVariation = null;
            localInformation = null;
        }
    }

    public final TreeMap<Double, ProgramDetails> pathingAtEachSimulationTime;
    public ImmutablePair<ImmutablePair<Double, List<String>>, List<ClusterDifference<String>>> clusterConnection;
    public double startCommunicatingAtSimulationTime = Double.MAX_VALUE;
    public double minTime = 0;
    public double maxTime = 0;

    public TreeMap<Double, ProgramDetails> getPathingAtEachSimulationTime() {
        return pathingAtEachSimulationTime;
    }

    public ImmutablePair<ImmutablePair<Double, List<String>>, List<ClusterDifference<String>>> getClusterConnection() {
        return clusterConnection;
    }

    public void setClusterConnection(ImmutablePair<ImmutablePair<Double, List<String>>, List<ClusterDifference<String>>> clusterConnection) {
        this.clusterConnection = clusterConnection;
    }

    public double getStartCommunicatingAtSimulationTime() {
        return startCommunicatingAtSimulationTime;
    }

    public void setStartCommunicatingAtSimulationTime(double startCommunicatingAtSimulationTime) {
        this.startCommunicatingAtSimulationTime = startCommunicatingAtSimulationTime;
    }

    public double getMinTime() {
        return minTime;
    }
    public void setMinTime(double minStartTime) {
        this.minTime = minStartTime;
    }
    public double getMaxTime() {
        return maxTime;
    }
    public void setMaxTime(double maxTime) {
        this.maxTime = maxTime;
    }

    public IoTProgram(ImmutablePair<ImmutablePair<Double, List<String>>, List<ClusterDifference<String>>> clusterConnection) {
        this.clusterConnection = clusterConnection;
        this.pathingAtEachSimulationTime = new TreeMap<>();
    }

    public void putDeltaRSUAssociation(Double key, List<Union2<TimedIoT, TimedEdge>> retrievePath) {
        pathingAtEachSimulationTime.put(key, new ProgramDetails(retrievePath));
        if (key < startCommunicatingAtSimulationTime) {
            startCommunicatingAtSimulationTime = key;
        }
    }





    public void finaliseProgram() {
        if (clusterConnection.getRight().isEmpty() != pathingAtEachSimulationTime.isEmpty())
            throw new RuntimeException("ERROR");
        else if (clusterConnection.getRight().size()+1 != pathingAtEachSimulationTime.size()) {
            throw new RuntimeException("ERROR");
        }
        var it = pathingAtEachSimulationTime.entrySet().iterator();
        for (int i = 0; i<clusterConnection.getRight().size(); i++) {
            var tick = it.next();
            if (i == 0) {
                tick.getValue().isStartingProgram = true;
                if (!Objects.equals(tick.getKey(), clusterConnection.getKey().getLeft())) {
                    throw new RuntimeException("ERROR!");
                }
                tick.getValue().setInitialClusterConnection = clusterConnection.getLeft().getValue();
            } else {
                tick.getValue().setConnectionVariation = clusterConnection.getValue().get(i-1);
            }
        }
        clusterConnection = null; // Freeing some memory
    }
}
