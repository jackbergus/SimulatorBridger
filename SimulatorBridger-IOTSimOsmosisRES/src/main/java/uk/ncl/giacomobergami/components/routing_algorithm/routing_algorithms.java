package uk.ncl.giacomobergami.components.routing_algorithm;

public abstract class routing_algorithms {

    protected String name;
    protected String device_agent_class;
    protected String central_agent_class;
    protected String cloud_routing_policy_class;
    protected String edge_routing_policy_class;

    public routing_algorithms(String name, String device_agent_class, String central_agent_class, String cloud_routing_policy_class, String edge_routing_policy_class) {
        this.name = name;
        this.device_agent_class = device_agent_class;
        this.central_agent_class = central_agent_class;
        this.cloud_routing_policy_class = cloud_routing_policy_class;
        this.edge_routing_policy_class = edge_routing_policy_class;
    }

    public String getName() {
        return name;
    }

    public String getDevice_agent_class() {
        return device_agent_class;
    }

    public String getCentral_agent_class() {
        return central_agent_class;
    }

    public String getCloud_routing_policy_class() {
        return cloud_routing_policy_class;
    }

    public String getEdge_routing_policy_class() {
        return edge_routing_policy_class;
    }
}
