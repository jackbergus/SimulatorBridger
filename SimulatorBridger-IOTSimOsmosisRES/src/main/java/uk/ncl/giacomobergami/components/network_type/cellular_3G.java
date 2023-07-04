package uk.ncl.giacomobergami.components.network_type;

public class cellular_3G extends networkTyping {
    private static final float NETWORK_TYPE_BANDWIDTH=56.0f;
    private static final double NETWORK_TYPE_LATENCY=(double) 56/1000;
    private static final float NETWORK_TYPE_SIGNAL_STRENGTH=100.0f;

    public cellular_3G() { super("3G", NETWORK_TYPE_BANDWIDTH, NETWORK_TYPE_LATENCY, NETWORK_TYPE_SIGNAL_STRENGTH);}
}
