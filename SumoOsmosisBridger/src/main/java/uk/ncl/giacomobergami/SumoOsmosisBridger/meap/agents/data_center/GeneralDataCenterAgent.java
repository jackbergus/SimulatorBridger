package uk.ncl.giacomobergami.SumoOsmosisBridger.meap.agents.data_center;

import org.cloudbus.agent.DCAgent;
import org.cloudbus.osmosis.core.OsmoticDatacenter;
import uk.ncl.giacomobergami.SumoOsmosisBridger.meap.agents.AbstractNetworkAgent;
import uk.ncl.giacomobergami.SumoOsmosisBridger.meap.agents.AbstractNetworkAgentPolicy;

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
    public void plan(String PowerModel) {
        super.plan(PowerModel);
        abstractNetworkAgent.plan("None");
    }

    @Override
    public void execute() {
        super.execute();
        abstractNetworkAgent.execute();
    }
}
