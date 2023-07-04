package uk.ncl.giacomobergami.components.network_type;

public class cellular_4G extends networkTyping {
    private static final float NETWORK_TYPE_BANDWIDTH=1000.0f;
    private static final double NETWORK_TYPE_LATENCY=(double) 75/1000;
    private static final float NETWORK_TYPE_SIGNAL_STRENGTH=100.0f;

    public cellular_4G() { super("4G", NETWORK_TYPE_BANDWIDTH, NETWORK_TYPE_LATENCY, NETWORK_TYPE_SIGNAL_STRENGTH);}
}
