package uk.ncl.giacomobergami.components.network_type;

public abstract class networkTyping {

    protected String name;
    protected float bw;
    protected double latency;
    protected float signalRange;

    public networkTyping(String name, float bw, double latency, float signalRange) {
        this.name = name;
        this.bw = bw;
        this.latency = latency;
        this.signalRange = signalRange;
    }

    public String getNetType() { return name; }

    public float getNTBW() { return bw; }

    public double getNTLat() { return latency; }

    public float getNTSR() { return signalRange; }
}
