package org.cloudbus.agent;

import org.cloudbus.osmosis.core.OsmoticDatacenter;
import org.cloudbus.res.EnergyController;
import org.jooq.DSLContext;

import java.sql.Connection;

public class DCAgent extends AbstractAgent {
    protected OsmoticDatacenter osmesisDatacenter;
    protected EnergyController energyController;

    public DCAgent(OsmoticDatacenter osmesisDatacenter) {
        this.osmesisDatacenter = osmesisDatacenter;
    }

    public DCAgent() { }

    public void setOsmesisDatacenter(OsmoticDatacenter osmesisDatacenter) {
        this.osmesisDatacenter = osmesisDatacenter;
    }

    public OsmoticDatacenter getOsmesisDatacenter() {
        return osmesisDatacenter;
    }

    public void setEnergyController(EnergyController energyController) {
        this.energyController = energyController;
    }

    @Override
    public void monitor() { }
    @Override
    public void analyze() { }
    @Override
    public void plan(String PowerModel, Connection conn, DSLContext context)    { }
    @Override
    public void execute() { }
}
