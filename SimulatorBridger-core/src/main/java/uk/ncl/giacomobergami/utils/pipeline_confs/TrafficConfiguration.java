package uk.ncl.giacomobergami.utils.pipeline_confs;

public class TrafficConfiguration {
    public String clazzPath;
    public String YAMLConverterConfiguration;
    public String RSUCsvFile;
    public String VehicleCsvFile;
    public boolean outputRSUCsvFile;
    public boolean outputVehicleCsvFile;
    public long begin, end;
    public double step;

    public long getBegin() {
        return begin;
    }

    public void setBegin(long begin) {
        this.begin = begin;
    }

    public long getEnd() {
        return end;
    }

    public void setEnd(long end) {
        this.end = end;
    }

    public double getStep() {
        return step;
    }

    public void setStep(double step) {
        this.step = step;
    }

    public String getClazzPath() {
        return clazzPath;
    }

    public void setClazzPath(String clazzPath) {
        this.clazzPath = clazzPath;
    }

    public String getYAMLConverterConfiguration() {
        return YAMLConverterConfiguration;
    }

    public void setYAMLConverterConfiguration(String YAMLConverterConfiguration) {
        this.YAMLConverterConfiguration = YAMLConverterConfiguration;
    }

    public String getRSUCsvFile() {
        return RSUCsvFile;
    }

    public void setRSUCsvFile(String RSUCsvFile) {
        this.RSUCsvFile = RSUCsvFile;
    }

    public String getVehicleCsvFile() {
        return VehicleCsvFile;
    }

    public void setVehicleCsvFile(String vehicleCsvFile) {
        VehicleCsvFile = vehicleCsvFile;
    }

    public void setOutputRSUCsvFile(boolean outputRSUCsvFile) {
        this.outputRSUCsvFile = outputRSUCsvFile;
    }

    public boolean isOutputRSUCsvFile() {
        return outputRSUCsvFile;
    }

    public void setOutputVehicleCsvFile(boolean outputVehicleCsvFile) {
        this.outputVehicleCsvFile = outputVehicleCsvFile;
    }

    public boolean isOutputVehicleCsvFile() {
        return outputVehicleCsvFile;
    }
}