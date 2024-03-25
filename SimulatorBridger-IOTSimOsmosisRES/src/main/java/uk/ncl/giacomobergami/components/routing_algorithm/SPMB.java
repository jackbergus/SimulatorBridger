package uk.ncl.giacomobergami.components.routing_algorithm;

public class SPMB extends routing_algorithms{ //Shortest Path Maximum Bandwidth
    public static final String DEVICE_AGENT_CLASS="uk.ncl.giacomobergami.SumoOsmosisBridger.meap.agents.device_agent.DeviceAgentNearestScanner";
    public static final String CENTRAL_AGENT_CLASS="uk.ncl.giacomobergami.SumoOsmosisBridger.meap.agents.central_agent.NearestCentralAgent";
    public static final String CLOUD_ROUTING_POLICY_CLASS="uk.ncl.giacomobergami.components.sdn_routing.SDNRoutingTraditionalShortestPath";
    public static final String EDGE_ROUTING_POLICY_CLASS="uk.ncl.giacomobergami.components.sdn_routing.SDNRoutingTraditionalShortestPath";

    public SPMB() { super("SPMB", DEVICE_AGENT_CLASS, CENTRAL_AGENT_CLASS, CLOUD_ROUTING_POLICY_CLASS, EDGE_ROUTING_POLICY_CLASS);}
}
