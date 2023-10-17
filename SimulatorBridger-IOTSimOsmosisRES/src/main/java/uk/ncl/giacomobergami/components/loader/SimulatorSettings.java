package uk.ncl.giacomobergami.components.loader;

import org.cloudbus.osmosis.core.OsmoticBroker;
import org.cloudbus.osmosis.core.OsmoticDatacenter;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class SimulatorSettings {
    public AtomicInteger hostId;
    public AtomicInteger vmId;
    public AtomicInteger flowId;
    public AtomicInteger edgeLetId;
    public List<OsmoticDatacenter> osmesisDatacentres;

    public SimulatorSettings() {
        hostId = new AtomicInteger(1);
        vmId = new AtomicInteger(1);
        flowId = new AtomicInteger(1);
        edgeLetId = new AtomicInteger(1);
        osmesisDatacentres = new ArrayList<>();
    }

    public OsmoticBroker newBroker(String name) {
        // TODO: make this as a singleton, so to return only the currently available instance, while ensuring uniqueness
        return OsmoticBroker.getInstance(name, edgeLetId, flowId);
    }
}
