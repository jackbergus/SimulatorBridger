package uk.ncl.giacomobergami.components.simulator;

import org.jooq.DSLContext;
import uk.ncl.giacomobergami.components.loader.GlobalConfigurationSettings;
import uk.ncl.giacomobergami.utils.shared_data.edge.Edge;
import uk.ncl.giacomobergami.utils.shared_data.iot.TimedIoT;

import java.sql.Connection;
import java.util.List;

public interface SimulatorBridger {
    void init(Connection conn, DSLContext context, List<Edge> edgeNodes); // automatically using the YAML configuration files for this set-up, minus the location of the EdgeNodes/RSUs, from which you are deriving the network infrastructure

    boolean run(double start, double end, double delta, List<TimedIoT> injectedCommunicationEvents, Connection conn, DSLContext context);

    void fini(Connection conn, DSLContext context); // finalising the simulator when all the events are depleted
}

