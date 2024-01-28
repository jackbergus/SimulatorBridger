package org.cloudbus.cloudsim.osmesis.examples.uti;

public class AccurateBatteryInformation {
    private int flowId;
    public String actualIoTDeviceName;
    public double time;
    public double consumption;
    public double noPackets;

    public AccurateBatteryInformation() {
        actualIoTDeviceName = "";
        time = 0.0;
        consumption = 0.0;
        noPackets = 0.0;
    }

    public AccurateBatteryInformation(String IoTDeviceName, double time, double consume, long communicate, int flowId) {
        this.actualIoTDeviceName = IoTDeviceName;
        this.time = time;
        this.consumption = consume;
        this.noPackets = communicate;
        this.flowId = flowId;
    }

    public int getFlowId() {
        return flowId;
    }

    public String getActualIoTDeviceName() {
        return actualIoTDeviceName;
    }

    public void setActualIoTDeviceName(String IoTDeviceName) {
        this.actualIoTDeviceName = IoTDeviceName;
    }

    public double getTime() {
        return time;
    }

    public void setTime(double time) {
        this.time = time;
    }

    public double getConsumption() {
        return consumption;
    }

    public void setConsumption(double consumption) {
        this.consumption = consumption;
    }

    public double getNoPackets() {
        return noPackets;
    }

    public void setNoPackets(double noPackets) {
        this.noPackets = noPackets;
    }
}
