package org.cloudbus.agent;

public class CentralAgent extends AbstractAgent{
    private static CentralAgent instance;

    public static String CENTRAL_AGENT_NAME="CentralOsmoticAgent";

    public static CentralAgent getInstance()
    {
        if(instance==null){
            instance=new CentralAgent();
            instance.setName(CENTRAL_AGENT_NAME);
        }
        return instance;
    }

    @Override
    public void monitor() {
        //In Central Agent messages are received in the monitor phase from simple distributed agents
        //using getReceivedMessages().
    }

    @Override
    public void analyze() {

    }

    @Override
    public void plan(String PowerModel) {

    }

    @Override
    public void execute() {
        //In Central Agent messages are published to distributed agents in the execute phase
        //using publishMessage(AgentMessage message).
    }
}
