package uk.ncl.giacomobergami.components.network_type;

public class wifi extends networkTyping {
    private static final float NETWORK_TYPE_BANDWIDTH=100.0f;
    private static final double NETWORK_TYPE_LATENCY=(double) 1/1000;
    private static final float NETWORK_TYPE_SIGNAL_STRENGTH=100.0f;

    public wifi() { super("wifi", NETWORK_TYPE_BANDWIDTH, NETWORK_TYPE_LATENCY, NETWORK_TYPE_SIGNAL_STRENGTH);}
}
