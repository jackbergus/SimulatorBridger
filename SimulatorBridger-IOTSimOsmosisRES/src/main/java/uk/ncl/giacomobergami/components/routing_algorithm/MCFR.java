package uk.ncl.giacomobergami.components.routing_algorithm;

public class MCFR extends routing_algorithms { //Minimum-Cost Flow Routing
    public static final String DEVICE_AGENT_CLASS="uk.ncl.giacomobergami.SumoOsmosisBridger.meap.agents.device_agent.DeviceAgentGlobalScanner";
    public static final String CENTRAL_AGENT_CLASS="uk.ncl.giacomobergami.SumoOsmosisBridger.meap.agents.central_agent.FlowCentralAgent";
    public static final String CLOUD_ROUTING_POLICY_CLASS="uk.ncl.giacomobergami.components.sdn_routing.SDNRoutingLoadBalancing";
    public static final String EDGE_ROUTING_POLICY_CLASS="uk.ncl.giacomobergami.components.sdn_routing.MaximumFlowRoutingPolicy";

    public MCFR() { super("MCFR", DEVICE_AGENT_CLASS, CENTRAL_AGENT_CLASS, CLOUD_ROUTING_POLICY_CLASS, EDGE_ROUTING_POLICY_CLASS);}
}
