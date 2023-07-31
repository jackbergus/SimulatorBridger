package uk.ncl.giacomobergami.SumoOsmosisBridger.traffic_converter;

public class SUMOConfiguration {
    public String trace_file;
    public String logger_file;
    public String sumo_program;
    public String sumo_configuration_file_path;
    public String sumo_detectors_file_path;
    public String sumo_vTypes_file_path;
    public String python_filepath;
    public String generateRSUAdjacencyList;
    public String updateRSUFields;
    public double default_rsu_communication_radius;
    public int default_max_vehicle_communication;


    public String getGenerateRSUAdjacencyList() {
        return generateRSUAdjacencyList;
    }
    public void setGenerateRSUAdjacencyList(String generateRSUAdjacencyList) {
        this.generateRSUAdjacencyList = generateRSUAdjacencyList;
    }
    public String getUpdateRSUFields() {
        return updateRSUFields;
    }
    public void setUpdateRSUFields(String updateRSUFields) {
        this.updateRSUFields = updateRSUFields;
    }
    public double getDefault_rsu_communication_radius() {
        return default_rsu_communication_radius;
    }
    public void setDefault_rsu_communication_radius(double default_rsu_communication_radius) {
        this.default_rsu_communication_radius = default_rsu_communication_radius;
    }
    public int getDefault_max_vehicle_communication() {
        return default_max_vehicle_communication;
    }
    public void setDefault_max_vehicle_communication(int default_max_vehicle_communication) {
        this.default_max_vehicle_communication = default_max_vehicle_communication;
    }
    public String getTrace_file() {
        return trace_file;
    }
    public void setTrace_file(String trace_file) {
        this.trace_file = trace_file;
    }
    public String getLogger_file() {
        return logger_file;
    }
    public void setLogger_file(String logger_file) {
        this.logger_file = logger_file;
    }
    public String getSumo_program() {
        return sumo_program;
    }
    public void setSumo_program(String sumo_program) {
        this.sumo_program = sumo_program;
    }
    public String getSumo_configuration_file_path() {
        return sumo_configuration_file_path;
    }
    public void setSumo_configuration_file_path(String sumo_configuration_file_path) {
        this.sumo_configuration_file_path = sumo_configuration_file_path;
    }
    public String getSumo_detectors_file_path() {
        return sumo_detectors_file_path;
    }
    public void setSumo_detectors_file_path(String sumo_detectors_file_path) {
        this.sumo_detectors_file_path = sumo_detectors_file_path;
    }
    public String getSumo_vTypes_file_path() {
        return sumo_vTypes_file_path;
    }
    public void setSumo_vTypes_file_path(String sumo_vTypes_file_path) {
        this.sumo_vTypes_file_path = sumo_vTypes_file_path;
    }
    public String getPython_filepath() { return python_filepath; }
    public void setPython_filepath(String python_filepath) {
        this.python_filepath = python_filepath;
    }
}
