package org.cloudbus.agent;

import uk.ncl.giacomobergami.components.iot.IoTDevice;
import org.cloudbus.osmosis.core.OsmoticDatacenter;
import org.cloudbus.res.EnergyController;
import org.cloudbus.res.config.AppConfig;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class AgentBroker {
    //Singleton pattern
    private static AgentBroker singleton = new AgentBroker();
    private Map<String, EnergyController> energyControllers;

    public static AgentBroker getInstance() {
        return singleton;
    }

    //Communication topology for Osmotic Agents
    private Map<String, Collection<String>> linksTopology = new HashMap<>();

    //Agents Names2Agents
    //Osmotic Agents are uniquely recognized by name
    private Map<String,DCAgent> agentsDC = new HashMap<>();
    private Map<String,DeviceAgent> agentsDevices = new HashMap<>();
    private Map<String, DeviceAgent> updateAgentDevices = new HashMap<>();
    private CentralAgent ca;

    public Stream<OsmoticDatacenter> getOsmoticDataCentersStream() {
        return agentsDC.values().stream().map(x-> x.osmesisDatacenter);
    }

    //Agents classes
    private Class dcAgentClass;
    private Class deviceAgentClass;
    private Class centralAgentClass;
    private Class agentMessageClass;

    private LocalDateTime simulationStartTime;
    private LocalDateTime simulationCurrentTime;

    private boolean agentsAvailable = false;

    private void updateEnergyControllersTime(){
        if (energyControllers != null)
        for (EnergyController controller: energyControllers.values()){
            controller.setCurrentTime(simulationCurrentTime);
        }
    }

    private double lastBatteryUpdate=-1;
    private double batteryDeltaTime;


    private void updateDeviceBatteries(double clock){
        if (lastBatteryUpdate < 0){
            lastBatteryUpdate = clock;
            return;
        } else {
            //battery delta time is in seconds
            batteryDeltaTime = clock-lastBatteryUpdate;
            lastBatteryUpdate = clock;
        }

        for(DeviceAgent devAgent:updateAgentDevices.values()){
            IoTDevice iotDevice = devAgent.getIoTDevice();
            if (iotDevice.getBattery().isResPowered()){

                String associatedEdge = devAgent.getIoTDevice().getAssociatedEdge();
                EnergyController ec = energyControllers.get(associatedEdge);
                double actualPowerEdge = ec.getRESCurrentPower();
                double maxPowerEdge = ec.getRESMaximumPower();
                double batteryPeakSolar = iotDevice.getBattery().getPeakSolarPower();
                double batteryVoltage = devAgent.getIoTDevice().getBattery().getBatteryVoltage();

                double max_charging_current = devAgent.getIoTDevice().getBattery().getMaxChargingCurrent();

                double actualBatteryPower = actualPowerEdge * batteryPeakSolar / maxPowerEdge /1000.0;

                double current = actualBatteryPower / batteryVoltage * 1000.0;

                if (current>max_charging_current){
                    current = max_charging_current;
                }

//                double mah = batteryDeltaTime/3600.0 * current;

                //charging is in mAh
//                iotDevice.getBattery().chargeBattery(mah, current);

                iotDevice.getBattery().setCharging(false);
//                if (mah>0.0){
//                    iotDevice.getBattery().setCharging(true);
//                } else {
//                    iotDevice.getBattery().setCharging(false);
//                }

            }
        }
    }

    public void setSimulationStartTime(String time_s){
        simulationStartTime = LocalDateTime.parse(time_s, AppConfig.FORMATTER);
        simulationCurrentTime = simulationStartTime;
        updateEnergyControllersTime();
    }

    public void updateTime(double clock, List<String> toUpdate) {
        if (!agentsAvailable) return;
        simulationCurrentTime = simulationStartTime.plusNanos((long) (clock*1000000000));
        updateEnergyControllersTime();
        updateAgentDevices.clear();
        Set<String> intersection = agentsDevices.keySet().stream().filter(toUpdate::contains).collect(Collectors.toSet());
        for(String entry:intersection) {
            updateAgentDevices.put(entry, agentsDevices.get(entry));
        }
        updateDeviceBatteries(clock);
    }

    public void setDcAgentClass(Class dcAgentClass) {
        this.dcAgentClass = dcAgentClass;
        agentsAvailable = true;
    }

    public void setDeviceAgentClass(Class deviceAgentClass) {
        this.deviceAgentClass = deviceAgentClass;
        agentsAvailable = true;
    }

    public void setCentralAgentClass(Class centralAgentClass) {
        this.centralAgentClass = centralAgentClass;
        agentsAvailable = true;
    }

    public void setAgentMessageClass(Class agentMessageClass) {
        this.agentMessageClass = agentMessageClass;
    }

    //Agents creation
    public void createDCAgent(String dcName, OsmoticDatacenter dc){
        if (dcAgentClass==null) return;
        try {
            DCAgent dcAgent = (DCAgent) dcAgentClass.newInstance();
            dcAgent.setOsmesisDatacenter(dc);
            dcAgent.setName(dcName);

            if ((energyControllers != null) && energyControllers.containsKey(dcName)){
                dcAgent.setEnergyController(energyControllers.get(dcName));
            } else {
                dcAgent.setEnergyController(null);
            }

            agentsDC.put(dcName,dcAgent);
        } catch (InstantiationException | IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    public void createDeviceAgent(String deviceName, IoTDevice device){
        if (deviceAgentClass==null) return;
        try {
            DeviceAgent deviceAgent = (DeviceAgent) deviceAgentClass.newInstance();
            deviceAgent.setIoTDevice(device);
            deviceAgent.setName(deviceName);
            agentsDevices.put(deviceName, deviceAgent);
        } catch (InstantiationException | IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    public AgentMessage createEmptyMessage(){
        AgentMessage message = null;
        try {
            message = (AgentMessage) agentMessageClass.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            e.printStackTrace();
        }
        return message;
    }

    //Osmotic Agents link topology
    private void addLink2Topology(String source, String destination){
        if (!linksTopology.containsKey(source)){
            linksTopology.put(source, new HashSet<String>());
        }

        if (!linksTopology.get(source).contains(destination)){
            linksTopology.get(source).add(destination);
        }
    }

    public void addAgentLink(String source, String destination){
        //links are bidirectional
        addLink2Topology(source,destination);
        addLink2Topology(destination,source);
    }

    public AbstractAgent getAgentByName(String name){
        if (agentsDC.containsKey(name)){
            return agentsDC.get(name);
        } else if (agentsDevices.containsKey(name)){
            return agentsDevices.get(name);
        } else if (name.equals(CentralAgent.CENTRAL_AGENT_NAME)) {
            if (ca == null)
                initializeCentralAgentIfRequired();
            return ca;
        } else
            return null;
    }

    public void distributeMessage(AgentMessage message){
        Collection<String> dst;
        if (message.getDESTINATION() == null){
            if (message.getSOURCE().equals(CentralAgent.CENTRAL_AGENT_NAME)){
                dst = new ArrayList<>();
                dst.addAll(agentsDC.keySet());
                dst.addAll(agentsDevices.keySet());
            } else {
                dst = linksTopology.get(message.getSOURCE());
            }
        } else {
            dst = message.getDESTINATION();
        }

        if (dst!=null){
            dst.forEach(target->getAgentByName(target).receiveMessage(message));
        }
    }

    private double timeIntervalMAPE;

    //MAPE loop interval
    public void setMAPEInterval(double time_interval){
        this.timeIntervalMAPE = time_interval;
    }

    private double lastMAPEloop=-1;

    public void executeMAPE(double clock){
        if (lastMAPEloop < 0){
            executeMAPE();
            lastMAPEloop = clock;
        } else if (lastMAPEloop + timeIntervalMAPE < clock){
            executeMAPE();
            lastMAPEloop = clock;
        }
    }

    public void executeMAPE(){
        //Monitor & Analyze
        for(Agent agent: agentsDC.values()){
            agent.setCurrentTime(lastMAPEloop);
            agent.monitor();
            agent.analyze();
        }
        for(Agent agent: updateAgentDevices.values()){
            agent.setCurrentTime(lastMAPEloop);
            agent.monitor();
            agent.analyze();
        }
        if (ca != null) {
            ca.setCurrentTime(lastMAPEloop);
            ca.monitor();
            ca.analyze();
        }

        //Message passing between agents

        //Plan & Execute
        if (ca != null) {
            ca.plan();
            ca.execute();
        }
        for(Agent agent: agentsDC.values()){
            agent.plan();
            agent.execute();
        }
        for(Agent agent: updateAgentDevices.values()){
            agent.plan();
            agent.execute();
        }
    }

    public void setEnergyControllers(Map<String, EnergyController> energyControllers) {
        this.energyControllers = energyControllers;
    }

    public void initializeCentralAgentIfRequired() {
        if ((this.centralAgentClass != null) && (ca == null)) {
            try {
                ca = (CentralAgent) centralAgentClass.newInstance();
            } catch (InstantiationException | IllegalAccessException e) {
                e.printStackTrace();
            }
        }
    }
}
