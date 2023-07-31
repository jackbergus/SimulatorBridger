package uk.ncl.giacomobergami.components.iot;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import org.cloudbus.cloudsim.edge.core.edge.LegacyConfiguration;
import org.cloudbus.cloudsim.edge.core.edge.Mobility;
import uk.ncl.giacomobergami.components.networking.Host;
import uk.ncl.giacomobergami.utils.data.CSVMediator;

@JsonPropertyOrder({"name", "ioTClassName", "bw", "pesNumber", "movable", "hasMovingRange", "beginX", "beginY", "z", "endX", "endY", "velocity", "totalMovingDistance", "signalRange",
        "initial_battery_capacity", "battery_voltage", "max_battery_capacity", "battery_sensing_rate", "max_charging_current",
        "battery_sending_rate", "res_powered", "solar_peak_power"
        ,"communicationProtocol", "networkType", "data_frequency", "dataGenerationTime", "complexityOfDataPackage", "dataSize", "processingAbility",
"cloudletId", "cloudletLength", "cloudletFileSize", "cloudletOutputSize", "utilizationModelCpu", "utilizationModelRam", "utilizationModelBw", "associatedEdge"})
public class IoTDeviceTabularConfiguration {
    public String name;
    public double bw;
    public double max_battery_capacity;
    public double  battery_sensing_rate;
    public double battery_sending_rate;
    public String ioTClassName;
    public boolean movable;
    public boolean hasMovingRange;
    public double z;
    public double velocity;
    public int beginX;
    public int endX;
    public int beginY;
    public int endY;
    public double signalRange;
    public String communicationProtocol;
    public String networkType;
    public String stepSizeEditorPath;
    public double data_frequency;
    public double dataGenerationTime;
    public int complexityOfDataPackage;
    public int dataSize;
    public double initial_battery_capacity;
    public double battery_voltage;
    public  boolean res_powered;
    public double solar_peak_power;
    public double max_charging_current;
    public double processingAbility;
    public double totalMovingDistance;
    public  int cloudletId;
    public  long cloudletLength;
    public int pesNumber;
    public long cloudletFileSize;
    public long cloudletOutputSize;
    public String utilizationModelCpu;
    public String utilizationModelRam;
    public String utilizationModelBw;
    public String associatedEdge;
    ////////////////////////////
    public double latency;
    public boolean match;


    private static CSVMediator<IoTDeviceTabularConfiguration> readerWriter = null;
    public static CSVMediator<IoTDeviceTabularConfiguration> csvReader() {
        if (readerWriter == null)
            readerWriter = new CSVMediator<>(IoTDeviceTabularConfiguration.class);
        return readerWriter;
    }

    /**
     * Conversion from the original representation to the current CSV-based one
     * @param legacy
     * @return
     */
    public static IoTDeviceTabularConfiguration fromLegacy(LegacyConfiguration.IotDeviceEntity legacy) {
        IoTDeviceTabularConfiguration result = new IoTDeviceTabularConfiguration();
        result.ioTClassName = legacy.ioTClassName;
        result.setName(legacy.getName());
        result.setData_frequency(legacy.getData_frequency());
        result.setDataGenerationTime(legacy.getDataGenerationTime());
        result.setComplexityOfDataPackage(legacy.getComplexityOfDataPackage());
        result.setDataSize(legacy.getDataSize());
        result.setMax_battery_capacity(legacy.getMax_battery_capacity());
        result.setInitial_battery_capacity(legacy.getInitial_battery_capacity());
        result.setBattery_sensing_rate(legacy.getBattery_sensing_rate());
        result.setBattery_sending_rate(legacy.getBattery_sending_rate());
        result.setBattery_voltage(legacy.getBattery_voltage());
        result.setRes_powered(legacy.isRes_powered());
        result.setSolar_peak_power(legacy.getSolar_peak_power());
        result.setMax_charging_current(legacy.getMax_charging_current());
        result.setProcessingAbility(legacy.getProcessingAbility());
        result.setBw(legacy.getBw());
        var ele = legacy.getDataTemplate();
        if (ele != null) {
            result.setCloudletId(ele.getCloudletId());
            result.setCloudletLength(ele.getCloudletLength());
            result.setCloudletFileSize(ele.getCloudletFileSize());
            result.setPesNumber(ele.getPesNumber());
            result.setCloudletOutputSize(ele.getCloudletOutputSize());
            result.setUtilizationModelBw(ele.getUtilizationModelBw());
            result.setUtilizationModelCpu(ele.getUtilizationModelCpu());
            result.setUtilizationModelRam(ele.getUtilizationModelRam());
        }
        var nme = legacy.getNetworkModelEntity();
        if (nme != null) {
            result.setCommunicationProtocol(nme.getCommunicationProtocol());
            result.setNetworkType(nme.getNetworkType());
        }
        var mobility = legacy.getMobilityEntity();
        if (mobility != null) {
            result.setMovable(mobility.isMovable());
            result.setSignalRange(mobility.getSignalRange());
            result.setVelocity(mobility.getVelocity());
            var inner = mobility.getRange();
            if (inner != null) {
                result.beginX = inner.beginX;
                result.beginY = inner.beginY;
                result.endX = inner.endX;
                result.endY = inner.endY;
                result.hasMovingRange = true;
            } else {
                result.hasMovingRange = false;
            }
        }
        return result;
    }

    /**
     * This representation enables a more versatile tabular representation of the same data,
     * without requiring to adhere to a fixed nested data structure. In fact, nested data
     * is the same as unnested!
     * @return
     */
    public LegacyConfiguration.IotDeviceEntity asLegacyConfiguration() {
        LegacyConfiguration.IotDeviceEntity result = new LegacyConfiguration.IotDeviceEntity();

        if (((utilizationModelCpu != null) && (utilizationModelCpu.length() > 0)) ||
                ((utilizationModelRam != null) && (utilizationModelRam.length() > 0)) ||
                ((utilizationModelBw != null) && (utilizationModelBw.length() > 0))) {
            LegacyConfiguration.EdgeLetEntity ele = new LegacyConfiguration.EdgeLetEntity();
            ele.setCloudletId(cloudletId);
            ele.setCloudletLength(cloudletLength);
            ele.setCloudletFileSize(cloudletFileSize);
            ele.setPesNumber(pesNumber);
            ele.setCloudletOutputSize(cloudletOutputSize);
            ele.setUtilizationModelBw(utilizationModelBw);
            ele.setUtilizationModelCpu(utilizationModelCpu);
            ele.setUtilizationModelRam(utilizationModelRam);
            result.setDataTemplate(ele);
        } else {
            result.setDataTemplate(null);
        }

        if ((((networkType != null) && (networkType.length() > 0))) ||
                (((communicationProtocol != null) && (communicationProtocol.length() > 0)))) {
            LegacyConfiguration.NetworkModelEntity nme = new LegacyConfiguration.NetworkModelEntity();
            nme.setCommunicationProtocol(communicationProtocol);
            nme.setNetworkType(networkType);
            result.setNetworkModelEntity(nme);
        } else {
            result.setNetworkModelEntity(null);
        }

        LegacyConfiguration.MovingRangeEntity mre;
        if (hasMovingRange) {
            mre = new LegacyConfiguration.MovingRangeEntity();
            mre.beginX = beginX;
            mre.endX = endX;
            mre.beginY = beginY;
            mre.endY = endY;
        } else {
            mre = null;
        }

        LegacyConfiguration.MobilityEntity mobility = new LegacyConfiguration.MobilityEntity(new Mobility.Location(beginX, beginY, z));
        mobility.setMovable(movable);
        mobility.setRange(mre);
        mobility.setSignalRange(signalRange);
        mobility.setVelocity(velocity);

        result.ioTClassName = ioTClassName;
        result.setName(name);
        result.setData_frequency(data_frequency);
        result.setDataGenerationTime(dataGenerationTime);
        result.setComplexityOfDataPackage(complexityOfDataPackage);
        result.setDataSize(dataSize);
        result.setMax_battery_capacity(max_battery_capacity);
        result.setInitial_battery_capacity(initial_battery_capacity);
        result.setBattery_sensing_rate(battery_sensing_rate);
        result.setBattery_sending_rate(battery_sending_rate);
        result.setBattery_voltage(battery_voltage);
        result.setRes_powered(res_powered);
        result.setSolar_peak_power(solar_peak_power);
        result.setMax_charging_current(max_charging_current);
        result.setProcessingAbility(processingAbility);
        result.setBw(bw);
        result.setLatency(latency);

        result.setMobilityEntity(mobility);

        return result;
    }

    public boolean isHasMovingRange() {
        return hasMovingRange;
    }

    public void setHasMovingRange(boolean hasMovingRange) {
        this.hasMovingRange = hasMovingRange;
    }

    public String getAssociatedEdge() {
        return associatedEdge;
    }

    public void setAssociatedEdge(String associatedEdge) {
        this.associatedEdge = associatedEdge;
    }

    public double getTotalMovingDistance() {
        return totalMovingDistance;
    }

    public void setTotalMovingDistance(double totalMovingDistance) {
        this.totalMovingDistance = totalMovingDistance;
    }

    public boolean isMovable() {
        return movable;
    }

    public void setMovable(boolean movable) {
        this.movable = movable;
    }

    public double getVelocity() {
        return velocity;
    }

    public void setVelocity(double velocity) {
        this.velocity = velocity;
    }

    public int getBeginX() {
        return beginX;
    }

    public void setBeginX(int beginX) {
        this.beginX = beginX;
    }

    public int getEndX() {
        return endX;
    }

    public void setEndX(int endX) {
        this.endX = endX;
    }

    public int getBeginY() {
        return beginY;
    }

    public void setBeginY(int beginY) {
        this.beginY = beginY;
    }

    public int getEndY() {
        return endY;
    }

    public void setEndY(int endY) {
        this.endY = endY;
    }

    public double getSignalRange() {
        return signalRange;
    }

    public void setSignalRange(double signalRange) {
        this.signalRange = signalRange;
    }


    public double getZ() {
        return z;
    }

    public void setZ(double z) {
        this.z = z;
    }

    public String getIoTClassName() {
        return ioTClassName;
    }

    public void setIoTClassName(String ioTClassName) {
        this.ioTClassName = ioTClassName;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getData_frequency() {
        return data_frequency;
    }

    public void setData_frequency(double data_frequency) {
        this.data_frequency = data_frequency;
    }

    public double getDataGenerationTime() {
        return dataGenerationTime;
    }

    public void setDataGenerationTime(double dataGenerationTime) {
        this.dataGenerationTime = dataGenerationTime;
    }

    public int getComplexityOfDataPackage() {
        return complexityOfDataPackage;
    }

    public void setComplexityOfDataPackage(int complexityOfDataPackage) {
        this.complexityOfDataPackage = complexityOfDataPackage;
    }

    public int getDataSize() {
        return dataSize;
    }

    public void setDataSize(int dataSize) {
        this.dataSize = dataSize;
    }

    public String getNetworkType() {
        return networkType;
    }

    public void setNetworkType(String networkType) {
        this.networkType = networkType;
    }

    public String getCommunicationProtocol() {
        return communicationProtocol;
    }

    public void setCommunicationProtocol(String communicationProtocol) {
        this.communicationProtocol = communicationProtocol;
    }

    public double getMax_battery_capacity() {
        return max_battery_capacity;
    }

    public void setMax_battery_capacity(double max_battery_capacity) {
        this.max_battery_capacity = max_battery_capacity;
    }

    public double getInitial_battery_capacity() {
        return initial_battery_capacity;
    }

    public void setInitial_battery_capacity(double initial_battery_capacity) {
        this.initial_battery_capacity = initial_battery_capacity;
    }

    public double getBattery_sensing_rate() {
        return battery_sensing_rate;
    }

    public void setBattery_sensing_rate(double battery_sensing_rate) {
        this.battery_sensing_rate = battery_sensing_rate;
    }

    public double getBattery_sending_rate() {
        return battery_sending_rate;
    }

    public void setBattery_sending_rate(double battery_sending_rate) {
        this.battery_sending_rate = battery_sending_rate;
    }

    public double getBattery_voltage() {
        return battery_voltage;
    }

    public void setBattery_voltage(double battery_voltage) {
        this.battery_voltage = battery_voltage;
    }

    public boolean isRes_powered() {
        return res_powered;
    }

    public void setRes_powered(boolean res_powered) {
        this.res_powered = res_powered;
    }

    public double getSolar_peak_power() {
        return solar_peak_power;
    }

    public void setSolar_peak_power(double solar_peak_power) {
        this.solar_peak_power = solar_peak_power;
    }

    public double getMax_charging_current() {
        return max_charging_current;
    }

    public void setMax_charging_current(double max_charging_current) {
        this.max_charging_current = max_charging_current;
    }

    public double getProcessingAbility() {
        return processingAbility;
    }

    public void setProcessingAbility(double processingAbility) {
        this.processingAbility = processingAbility;
    }

    public int getCloudletId() {
        return cloudletId;
    }

    public void setCloudletId(int cloudletId) {
        this.cloudletId = cloudletId;
    }

    public long getCloudletLength() {
        return cloudletLength;
    }

    public void setCloudletLength(long cloudletLength) {
        this.cloudletLength = cloudletLength;
    }

    public int getPesNumber() {
        return pesNumber;
    }

    public void setPesNumber(int pesNumber) {
        this.pesNumber = pesNumber;
    }

    public long getCloudletFileSize() {
        return cloudletFileSize;
    }

    public void setCloudletFileSize(long cloudletFileSize) {
        this.cloudletFileSize = cloudletFileSize;
    }

    public long getCloudletOutputSize() {
        return cloudletOutputSize;
    }

    public void setCloudletOutputSize(long cloudletOutputSize) {
        this.cloudletOutputSize = cloudletOutputSize;
    }

    public String getUtilizationModelCpu() {
        return utilizationModelCpu;
    }

    public void setUtilizationModelCpu(String utilizationModelCpu) {
        this.utilizationModelCpu = utilizationModelCpu;
    }

    public String getUtilizationModelRam() {
        return utilizationModelRam;
    }

    public void setUtilizationModelRam(String utilizationModelRam) {
        this.utilizationModelRam = utilizationModelRam;
    }

    public String getUtilizationModelBw() {
        return utilizationModelBw;
    }

    public void setUtilizationModelBw(String utilizationModelBw) {
        this.utilizationModelBw = utilizationModelBw;
    }

    public double getBw() {
        return bw;
    }

    public void setBw(double bw) {
        this.bw = bw;
    }
}
