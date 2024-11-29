package uk.ncl.giacomobergami.components.simulator;

import com.opencsv.exceptions.CsvException;
import org.jooq.DSLContext;
import uk.ncl.giacomobergami.components.loader.GlobalConfigurationSettings;
import uk.ncl.giacomobergami.utils.shared_data.edge.Edge;
import uk.ncl.giacomobergami.utils.shared_data.iot.TimedIoT;

import java.io.IOException;
import java.sql.Connection;
import java.util.List;

public interface SimulatorBridger {
    void init(double start, List<Edge> edgeNodes); // automatically using the YAML configuration files for this set-up, minus the location of the EdgeNodes/RSUs, from which you are deriving the network infrastructure

    boolean run(double delta, double simulationEnd, double injectionTime, List<TimedIoT> timedIoTList) throws IOException, CsvException;

    void fini(); // finalising the simulator when all the events are depleted
}

