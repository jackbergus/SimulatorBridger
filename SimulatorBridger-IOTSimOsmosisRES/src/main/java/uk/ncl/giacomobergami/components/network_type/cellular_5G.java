package uk.ncl.giacomobergami.components.network_type;

public class cellular_5G extends networkTyping {
    private static final float NETWORK_TYPE_BANDWIDTH=10000.0f;
    private static final double NETWORK_TYPE_LATENCY=(double) 5/1000;
    private static final float NETWORK_TYPE_SIGNAL_STRENGTH=100.0f;

    public cellular_5G() { super("5G", NETWORK_TYPE_BANDWIDTH, NETWORK_TYPE_LATENCY, NETWORK_TYPE_SIGNAL_STRENGTH);}
}
