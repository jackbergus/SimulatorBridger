CREATE TABLE vehInformation
(
    dI_entry_ID      serial PRIMARY KEY,
    vehicle_ID       VARCHAR(100) NOT NULL,
    x                float        NOT NULL,
    y                float        NOT NULL,
    angle            float        NOT NULL,
    vehicle_type     VARCHAR(50)  NOT NULL,
    speed            float        NOT NULL,
    pos              float        NOT NULL,
    lane             VARCHAR(50)  NOT NULL,
    slope            float        NOT NULL,
    simtime          float        NOT NULL,
    injected         VARCHAR(5)   NOT NULL,
    batteryDepletion float        NOT NULL,
    useBattery       VARCHAR(5)   NOT NULL,
    packetSize       float        NOT NULL,
    usePacketInfo    VARCHAR(5)   NOT NULL
);

SELECT * FROM vehInformation;

CREATE INDEX idx_vehInfo_simtime ON vehInformation(simtime);
CREATE INDEX idx_vehInfo_vehIDs ON vehInformation(vehicle_id);
CREATE INDEX mysearchIndex ON vehInformation(simtime, vehicle_id, x, y);
CREATE INDEX ambulanceIndex ON ambulanceInformation(simtime, vehicle_id, x, y);

CREATE TABLE vehInformation_import
(
    vehicle_ID       VARCHAR(100) NOT NULL,
    x                float        NOT NULL,
    y                float        NOT NULL,
    angle            float        NOT NULL,
    vehicle_type     VARCHAR(50)  NOT NULL,
    speed            float        NOT NULL,
    pos              float        NOT NULL,
    lane             VARCHAR(50)  NOT NULL,
    slope            float        NOT NULL,
    simtime          float        NOT NULL,
    injected         VARCHAR(5)   NOT NULL,
    batteryDepletion float        NOT NULL,
    useBattery       VARCHAR(5)   NOT NULL,
    packetSize       float        NOT NULL,
    usePacketInfo    VARCHAR(5)   NOT NULL
);

SELECT * FROM vehInformation_import;

CREATE TABLE ambulanceInformation
(
    dI_entry_ID      serial PRIMARY KEY,
    vehicle_ID       VARCHAR(100) NOT NULL,
    x                float        NOT NULL,
    y                float        NOT NULL,
    angle            float        NOT NULL,
    vehicle_type     VARCHAR(50)  NOT NULL,
    speed            float        NOT NULL,
    pos              float        NOT NULL,
    lane             VARCHAR(50)  NOT NULL,
    slope            float        NOT NULL,
    simtime          float        NOT NULL,
    injected         VARCHAR(5)   NOT NULL,
    batteryDepletion float        NOT NULL,
    useBattery       VARCHAR(5)   NOT NULL,
    packetSize       float        NOT NULL,
    usePacketInfo    VARCHAR(5)   NOT NULL
);

SELECT * FROM ambulanceInformation;

CREATE TABLE ambulanceInformation_import
(
    vehicle_ID       VARCHAR(100) NOT NULL,
    x                float        NOT NULL,
    y                float        NOT NULL,
    angle            float        NOT NULL,
    vehicle_type     VARCHAR(50)  NOT NULL,
    speed            float        NOT NULL,
    pos              float        NOT NULL,
    lane             VARCHAR(50)  NOT NULL,
    slope            float        NOT NULL,
    simtime          float        NOT NULL,
    injected         VARCHAR(5)   NOT NULL,
    batteryDepletion float        NOT NULL,
    useBattery       VARCHAR(5)   NOT NULL,
    packetSize       float        NOT NULL,
    usePacketInfo    VARCHAR(5)   NOT NULL
);

SELECT * FROM ambulanceInformation_import;

CREATE TABLE rsuInformation
(
    unique_entry_ID           serial PRIMARY KEY,
    rsu_ID                    VARCHAR(100) NOT NULL,
    x                         float        NOT NULL,
    y                         float        NOT NULL,
    simtime                   float        NOT NULL,
    communication_radius      float        NOT NULL,
    max_vehicle_communication float        NOT NULL
);

SELECT * FROM rsuInformation;

CREATE TABLE rsuInformation_import
(
    x                         float        NOT NULL,
    y                         float        NOT NULL,
    simtime                   float        NOT NULL,
    communication_radius      float        NOT NULL,
    max_vehicle_communication float        NOT NULL,
    rsu_ID                    VARCHAR(100) NOT NULL
);

SELECT * FROM rsuInformation_import;


CREATE TABLE neighboursChange
(
    unique_entry_ID serial PRIMARY KEY,
    rsu_ID          VARCHAR(100) NOT NULL,
    time_of_update  float        NOT NULL,
    Neighbour1      VARCHAR(100),
    Neighbour2      VARCHAR(100),
    Neighbour3      VARCHAR(100),
    isChange        VARCHAR(50),
    change1         VARCHAR(100),
    change2         VARCHAR(100),
    change3         VARCHAR(100),
    change4         VARCHAR(100)
);

SELECT * FROM neighboursChange;

CREATE TABLE timed_scc
(
    unique_entry_ID    serial PRIMARY KEY,
    time_of_update     float NOT NULL,
    networkNeighbours1 VARCHAR(100),
    networkNeighbours2 VARCHAR(100),
    networkNeighbours3 VARCHAR(100),
    networkNeighbours4 VARCHAR(100)
);

SELECT * FROM timed_scc;

CREATE TABLE accurateBatteryInfo
(
    unique_entry_ID serial PRIMARY KEY,
    IoTDeviceName   VARCHAR(100) NOT NULL,
    consumption     float        NOT NULL,
    flowID          INTEGER      NOT NULL,
    noPackets       float        NOT NULL,
    time            float        NOT NULL

);

SELECT * FROM accurateBatteryInfo;

CREATE TABLE accurateBatteryInfo_import
(
    IoTDeviceName VARCHAR(100) NOT NULL,
    consumption   float        NOT NULL,
    flowID        INTEGER      NOT NULL,
    noPackets     float        NOT NULL,
    time          float        NOT NULL

);

SELECT * FROM accurateBatteryInfo_import;

CREATE TABLE appList
(
    unique_entry_ID             serial PRIMARY KEY,
    appID                       INTEGER      NOT NULL,
    appName                     VARCHAR(100) NOT NULL,
    appStartTime                float        NOT NULL,
    cloudDatacenterName         VARCHAR(100) NOT NULL,
    cloudDcId                   INTEGER      NOT NULL,
    dataRate                    float        NOT NULL,
    edgeDatacenterName          VARCHAR(100),
    edgeDcId                    INTEGER      NOT NULL,
    endTime                     float        NOT NULL,
    IoTDeviceBatteryConsumption float        NOT NULL,
    IoTDeviceBatteryStatus      VARCHAR(50)  NOT NULL,
    IoTDeviceId                 INTEGER      NOT NULL,
    IoTDeviceName               VARCHAR(100) NOT NULL,
    IoTDeviceOutputSize         INTEGER      NOT NULL,
    isIoTDeviceDied             VARCHAR(50)  NOT NULL,
    melID                       INTEGER      NOT NULL,
    melname                     VARCHAR(100) NOT NULL,
    meloutputSize               INTEGER      NOT NULL,
    osmesisCloudletSize         INTEGER      NOT NULL,
    osmesisEdgeletSize          INTEGER      NOT NULL,
    startDataGenerationTime     float        NOT NULL,
    stopDataGenerationTime      float        NOT NULL,
    vmCloudId                   INTEGER      NOT NULL,
    vmName                      VARCHAR(100) NOT NULL,
    workflowId                  INTEGER      NOT NULL
);

SELECT * FROM appList;

CREATE TABLE appList_import
(
    appID                       INTEGER      NOT NULL,
    appName                     VARCHAR(100) NOT NULL,
    appStartTime                float        NOT NULL,
    cloudDatacenterName         VARCHAR(100) NOT NULL,
    cloudDcId                   INTEGER      NOT NULL,
    dataRate                    float        NOT NULL,
    edgeDatacenterName          VARCHAR(100),
    edgeDcId                    INTEGER      NOT NULL,
    endTime                     float        NOT NULL,
    IoTDeviceBatteryConsumption float        NOT NULL,
    IoTDeviceBatteryStatus      VARCHAR(50)  NOT NULL,
    IoTDeviceId                 INTEGER      NOT NULL,
    IoTDeviceName               VARCHAR(100) NOT NULL,
    IoTDeviceOutputSize         INTEGER      NOT NULL,
    isIoTDeviceDied             VARCHAR(50)  NOT NULL,
    melID                       INTEGER      NOT NULL,
    melname                     VARCHAR(100) NOT NULL,
    meloutputSize               INTEGER      NOT NULL,
    osmesisCloudletSize         INTEGER      NOT NULL,
    osmesisEdgeletSize          INTEGER      NOT NULL,
    startDataGenerationTime     float        NOT NULL,
    stopDataGenerationTime      float        NOT NULL,
    vmCloudId                   INTEGER      NOT NULL,
    vmName                      VARCHAR(100) NOT NULL,
    workflowId                  INTEGER      NOT NULL
);

SELECT * FROM appList_import;

CREATE TABLE osmoticAppsStats
(
    unique_entry_ID                serial PRIMARY KEY,
    appID                          INTEGER      NOT NULL,
    appName                        VARCHAR(100) NOT NULL,
    CloudLetMISize                 float        NOT NULL,
    CloudLetProcessingTimeByVM     float        NOT NULL,
    DataSizeIoTDeviceToMEL_Mb      INTEGER      NOT NULL,
    DataSizeMELToVM_Mb             INTEGER      NOT NULL,
    DestinationVmName              VARCHAR(100) NOT NULL,
    EdgeLetMISize                  float        NOT NULL,
    EdgeLetProcessingTimeByMEL     float        NOT NULL,
    EdgeLet_MEL_FinishTime         float        NOT NULL,
    EdgeLet_MEL_StartTime          float        NOT NULL,
    FinishTime                     float        NOT NULL,
    IoTDeviceName                  VARCHAR(100) NOT NULL,
    MELName                        VARCHAR(200) NOT NULL,
    MelEndTransmissionTime         float        NOT NULL,
    MelStartTransmissionTime       float NULL,
    StartTime                      float        NOT NULL,
    OAS_Transaction                INTEGER      NOT NULL,
    TransactionTotalTime           float        NOT NULL,
    TransmissionTimeIoTDeviceToMEL float        NOT NULL,
    TransmissionTimeMELToVM        float        NOT NULL,
    flowIoTMelAppId                INTEGER      NOT NULL,
    flowMELCloudAppId              INTEGER      NOT NULL,
    path_dst                       VARCHAR(200) NOT NULL,
    path_src                       VARCHAR(200) NOT NULL,
    EdgeToWANBW                    float        NOT NULL
);

SELECT * FROM osmoticAppsStats;

CREATE TABLE osmoticAppsStats_import
(
    appID                          INTEGER      NOT NULL,
    appName                        VARCHAR(100) NOT NULL,
    CloudLetMISize                 float        NOT NULL,
    CloudLetProcessingTimeByVM     float        NOT NULL,
    DataSizeIoTDeviceToMEL_Mb      INTEGER      NOT NULL,
    DataSizeMELToVM_Mb             INTEGER      NOT NULL,
    DestinationVmName              VARCHAR(100) NOT NULL,
    EdgeLetMISize                  float        NOT NULL,
    EdgeLetProcessingTimeByMEL     float        NOT NULL,
    EdgeLet_MEL_FinishTime         float        NOT NULL,
    EdgeLet_MEL_StartTime          float        NOT NULL,
    FinishTime                     float        NOT NULL,
    IoTDeviceName                  VARCHAR(100) NOT NULL,
    MELName                        VARCHAR(200) NOT NULL,
    MelEndTransmissionTime         float        NOT NULL,
    MelStartTransmissionTime       float NULL,
    StartTime                      float        NOT NULL,
    OAS_Transaction                INTEGER      NOT NULL,
    TransactionTotalTime           float        NOT NULL,
    TransmissionTimeIoTDeviceToMEL float        NOT NULL,
    TransmissionTimeMELToVM        float        NOT NULL,
    flowIoTMelAppId                INTEGER      NOT NULL,
    flowMELCloudAppId              INTEGER      NOT NULL,
    path_dst                       VARCHAR(200) NOT NULL,
    path_src                       VARCHAR(200) NOT NULL,
    EdgeToWANBW                    float        NOT NULL
);

SELECT * FROM osmoticAppsStats_import;

CREATE TABLE overallAppResults
(
    unique_entry_ID             serial PRIMARY KEY,
    appname                     VARCHAR(100) NOT NULL,
    endtime                     float        NOT NULL,
    IoTDeviceBatteryConsumption float        NOT NULL,
    IoTDeviceDrained            VARCHAR(50)  NOT NULL,
    SimulationTime              float        NOT NULL,
    StartTime                   float        NOT NULL,
    TotalCloudLetSizes          INTEGER      NOT NULL,
    TotalEdgeLetSizes           INTEGER      NOT NULL,
    TotalIoTGeneratedData       INTEGER      NOT NULL,
    TotalMELGeneratedData       INTEGER      NOT NULL,
    appTotalRunningTime         float        NOT NULL
);

SELECT * FROM overallAppResults;

CREATE TABLE overallAppResults_import
(
    appname                     VARCHAR(100) NOT NULL,
    endtime                     float        NOT NULL,
    IoTDeviceBatteryConsumption float        NOT NULL,
    IoTDeviceDrained            VARCHAR(50)  NOT NULL,
    SimulationTime              float        NOT NULL,
    StartTime                   float        NOT NULL,
    TotalCloudLetSizes          INTEGER      NOT NULL,
    TotalEdgeLetSizes           INTEGER      NOT NULL,
    TotalIoTGeneratedData       INTEGER      NOT NULL,
    TotalMELGeneratedData       INTEGER      NOT NULL,
    appTotalRunningTime         float        NOT NULL
);

SELECT * FROM overallAppResults_import;

CREATE TABLE dataCenterEnergyConsumption
(
    unique_entry_ID      serial PRIMARY KEY,
    HostEnergyConsumed   float        NOT NULL,
    SwitchEnergyConsumed float        NOT NULL,
    TotalEnergyConsumed  float        NOT NULL,
    dcName               VARCHAR(100) NOT NULL,
    finishTime           float        NOT NULL
);

SELECT * FROM dataCenterEnergyConsumption;

CREATE TABLE dataCenterEnergyConsumption_import
(
    HostEnergyConsumed   float        NOT NULL,
    SwitchEnergyConsumed float        NOT NULL,
    TotalEnergyConsumed  float        NOT NULL,
    dcName               VARCHAR(100) NOT NULL,
    finishTime           float        NOT NULL
);

SELECT * FROM dataCenterEnergyConsumption_import;

CREATE TABLE HostPowerConsumption
(
    unique_entry_ID serial PRIMARY KEY,
    dcname          VARCHAR(100) NOT NULL,
    energy          float        NOT NULL,
    hpc_name        VARCHAR(100) NOT NULL
);

SELECT * FROM HostPowerConsumption;

CREATE TABLE HostPowerConsumption_import
(
    dcname   VARCHAR(100) NOT NULL,
    energy   float        NOT NULL,
    hpc_name VARCHAR(100) NOT NULL
);

SELECT * FROM HostPowerConsumption_import;

CREATE TABLE SwitchPowerConsumption
(
    unique_entry_ID serial PRIMARY KEY,
    dcname          VARCHAR(100) NOT NULL,
    energy          float        NOT NULL,
    spc_name        VARCHAR(100) NOT NULL
);

SELECT * FROM SwitchPowerConsumption;

CREATE TABLE SwitchPowerConsumption_import
(
    dcname   VARCHAR(100) NOT NULL,
    energy   float        NOT NULL,
    spc_name VARCHAR(100) NOT NULL
);

SELECT * FROM SwitchPowerConsumption_import;

CREATE TABLE PowerUtilisationHistory
(
    unique_entry_ID serial PRIMARY KEY,
    dcname          VARCHAR(100) NOT NULL,
    puh_name        VARCHAR(100) NOT NULL,
    starttime       float        NOT NULL,
    usedmips        float        NOT NULL
);

SELECT * FROM PowerUtilisationHistory;

CREATE TABLE PowerUtilisationHistory_import
(
    dcname    VARCHAR(100) NOT NULL,
    puh_name  VARCHAR(100) NOT NULL,
    starttime float        NOT NULL,
    usedmips  float        NOT NULL
);

SELECT * FROM PowerUtilisationHistory_import;

CREATE TABLE HistoryEntry
(
    unique_entry_ID serial PRIMARY KEY,
    numactiveports  INTEGER,
    starttime       float
);

SELECT * FROM HistoryEntry;

CREATE TABLE HistoryEntry_import
(
    numactiveports INTEGER,
    starttime      float
);

SELECT * FROM HistoryEntry_import;

CREATE TABLE ConnectionPerSimTime
(
    unique_entry_ID serial PRIMARY KEY,
    edgehost        VARCHAR(100) NOT NULL,
    iotdevices      float        NOT NULL,
    cps_time        float        NOT NULL
);

SELECT * FROM ConnectionPerSimTime;

CREATE TABLE ConnectionPerSimTime_import
(
    edgehost   VARCHAR(100) NOT NULL,
    iotdevices float        NOT NULL,
    cps_time   float        NOT NULL
);

SELECT * FROM ConnectionPerSimTime_import;

CREATE TABLE bandwidthShareInfo
(
    unique_entry_ID serial PRIMARY KEY,
    bandwidthshare  float        NOT NULL,
    channelid       VARCHAR(100) NOT NULL,
    edgename        VARCHAR(100) NOT NULL,
    melname         VARCHAR(100) NOT NULL,
    timestamp       float        NOT NULL
);

SELECT * FROM bandwidthShareInfo;

CREATE TABLE bandwidthShareInfo_import
(
    bandwidthshare float        NOT NULL,
    channelid      VARCHAR(100) NOT NULL,
    edgename       VARCHAR(100) NOT NULL,
    melname        VARCHAR(100) NOT NULL,
    timestamp      float        NOT NULL
);

SELECT * FROM bandwidthShareInfo_import;

CREATE TABLE sourceToDestLinks
(
    unique_entry_ID serial PRIMARY KEY,
    topology_ID     INTEGER NOT NULL,
    link_ID         INTEGER NOT NULL,
    from_ID         INTEGER NOT NULL,
    to_ID           INTEGER NOT NULL,
    bw              float   NOT NULL,
    noChannels      INTEGER NOT NULL
);

SELECT * FROM sourceToDestLinks;