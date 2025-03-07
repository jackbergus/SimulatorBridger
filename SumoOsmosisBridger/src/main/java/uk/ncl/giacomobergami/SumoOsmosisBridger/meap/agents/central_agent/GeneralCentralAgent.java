package uk.ncl.giacomobergami.SumoOsmosisBridger.meap.agents.central_agent;

import org.cloudbus.agent.CentralAgent;
import uk.ncl.giacomobergami.SumoOsmosisBridger.meap.agents.AbstractNetworkAgent;
import uk.ncl.giacomobergami.SumoOsmosisBridger.meap.agents.AbstractNetworkAgentPolicy;

public class GeneralCentralAgent extends CentralAgent {

    AbstractNetworkAgent abstractNetworkAgent;
    public GeneralCentralAgent() {
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
        abstractNetworkAgent.plan(PowerModel);
    }

    @Override
    public void execute() {
        super.execute();
        abstractNetworkAgent.execute();
    }
}
