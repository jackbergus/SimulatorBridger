package uk.ncl.giacomobergami.components.simulator;

import uk.ncl.giacomobergami.utils.shared_data.edge.Edge;
import uk.ncl.giacomobergami.utils.shared_data.iot.TimedIoT;

import java.util.List;

public interface SimulatorBridger {
    void init(List<Edge> edgeNodes); // automatically using the YAML configuration files for this set-up, minus the location of the EdgeNodes/RSUs, from which you are deriving the network infrastructure

    boolean run(double start, double end, List<TimedIoT> injectedCommunicationEvents);

    void fini(); // finalising the simulator when all the events are depleted
}

