package uk.ncl.giacomobergami.utils.pipeline_confs;

public class TrafficConfiguration {
        public String clazzPath;
        public String YAMLConverterConfiguration;
        public String RSUCsvFile;
        public String VehicleCsvFile;
        public long begin, end, step;

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

        public long getStep() {
            return step;
        }

        public void setStep(long step) {
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
    }