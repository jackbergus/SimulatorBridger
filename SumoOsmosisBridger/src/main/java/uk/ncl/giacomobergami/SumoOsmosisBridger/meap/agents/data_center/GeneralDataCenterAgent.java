package uk.ncl.giacomobergami.SumoOsmosisBridger.meap.agents.data_center;

import org.cloudbus.agent.DCAgent;
import org.cloudbus.osmosis.core.OsmoticDatacenter;
import org.jooq.DSLContext;
import uk.ncl.giacomobergami.SumoOsmosisBridger.meap.agents.AbstractNetworkAgent;
import uk.ncl.giacomobergami.SumoOsmosisBridger.meap.agents.AbstractNetworkAgentPolicy;

import java.sql.Connection;

public class GeneralDataCenterAgent extends DCAgent {

    AbstractNetworkAgent abstractNetworkAgent;

    public GeneralDataCenterAgent(OsmoticDatacenter osmesisDatacenter) {
        super(osmesisDatacenter);
        abstractNetworkAgent = new AbstractNetworkAgent(this);
    }

    public GeneralDataCenterAgent() {
        abstractNetworkAgent = new AbstractNetworkAgent(this);
    }

    public AbstractNetworkAgentPolicy getPolicy() { return abstractNetworkAgent.getPolicy(); }
    public void setPolicy(AbstractNetworkAgentPolicy policy) { this.abstractNetworkAgent.setPolicy(policy); }

    @Override
    public void monitor() {
        super.monitor();
        abstractNetworkAgent.monitor();
    }

    @Override
    public void analyze() {
        super.analyze();
        abstractNetworkAgent.analyze();
    }

    @Override
    public void plan(String PowerModel, Connection conn, DSLContext context) {
        super.plan(PowerModel, conn, context);
        abstractNetworkAgent.plan("None", conn, context);
    }

    @Override
    public void execute() {
        super.execute();
        abstractNetworkAgent.execute();
    }
}
