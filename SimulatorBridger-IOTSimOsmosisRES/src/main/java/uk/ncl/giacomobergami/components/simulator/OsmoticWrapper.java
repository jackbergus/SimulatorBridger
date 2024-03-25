/*
 * OsmoticWrapper.java
 * This file is part of SimulatorBridger-IOTSimOsmosisRES
 *
 * Copyright (C) 2022 - Giacomo Bergami
 *
 * SimulatorBridger-IOTSimOsmosisRES is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * SimulatorBridger-IOTSimOsmosisRES is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with SimulatorBridger-IOTSimOsmosisRES. If not, see <http://www.gnu.org/licenses/>.
 */

package uk.ncl.giacomobergami.components.simulator;

import org.cloudbus.agent.AgentBroker;
import org.cloudbus.agent.config.AgentConfigLoader;
import org.cloudbus.agent.config.AgentConfigProvider;
import org.cloudbus.agent.config.TopologyLink;
import org.cloudbus.cloudsim.core.MainEventManager;
import org.cloudbus.cloudsim.core.SimEntity;
import org.cloudbus.cloudsim.edge.core.edge.LegacyConfiguration;
import org.cloudbus.cloudsim.edge.utils.LogUtil;
import org.cloudbus.cloudsim.osmesis.examples.uti.PrintResults;
import org.cloudbus.cloudsim.osmesis.examples.uti.RESPrinter;
import org.cloudbus.osmosis.core.*;
import org.cloudbus.res.EnergyController;
import org.cloudbus.res.config.AppConfig;
import org.cloudbus.res.dataproviders.res.RESResponse;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import uk.ncl.giacomobergami.components.iot.IoTEntityGenerator;
import uk.ncl.giacomobergami.components.loader.GlobalConfigurationSettings;
import uk.ncl.giacomobergami.components.mel_routing.MELRoutingPolicyGeneratorFacade;
import uk.ncl.giacomobergami.components.mel_routing.MELSwitchPolicy;
import uk.ncl.giacomobergami.components.network_type.NetworkTypingGeneratorFactory;
import uk.ncl.giacomobergami.components.networking.DataCenterWithController;
import uk.ncl.giacomobergami.components.routing_algorithm.RoutingAlgorithmGeneratorFactory;
import uk.ncl.giacomobergami.components.routing_algorithm.routing_algorithms;
import uk.ncl.giacomobergami.utils.data.YAML;
import uk.ncl.giacomobergami.utils.pipeline_confs.TrafficConfiguration;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * This class provides an abstraction over the possible settings of running OSMOSIS
 */
public class OsmoticWrapper {
    private OsmoticConfiguration conf;
    LegacyTopologyBuilder topologyBuilder;
    OsmoticBroker osmoticBroker;
    AgentBroker agentBroker;
    Map<String, EnergyController> energyControllers;
    private boolean init;
    private boolean started;
    private boolean finished;
    private double runTime;
    List<OsmoticAppDescription> appList;
    List<PrintResults.BandwidthInfo> bandwidthInfoList;
    private static final File converter_file = new File("clean_example/converter.yaml");
    private static final Optional<TrafficConfiguration> time_conf = YAML.parse(TrafficConfiguration.class, converter_file);
    private static final String RA = time_conf.get().getRoutingAlgorithm();



    public OsmoticWrapper() {
        this(null);
    }

    public OsmoticWrapper(OsmoticConfiguration conf) {
        this.conf = conf;
        init = false;
        started = false;
        finished = false;
        energyControllers = null;
        runTime = 0.0;
    }

    private static File fileExists(String path) {
        if (path != null) {
            File n = new File(path).getAbsoluteFile();
            if (n.exists() && n.isFile()) {
                return n;
            }
        }
        return null;
    }

    public void stop(Connection conn, DSLContext context) {
        if (started) {
            MainEventManager.novel_stop(conn, context);
//            OsmoticAppsParser.appList.clear();
            OsmoticBroker.workflowTag.clear();
//            osmoticBroker = null;
            topologyBuilder = null;
            agentBroker = null;
            started = false;
            init = false;
            energyControllers = null;
            runTime = 0.0;
            started = false;
            finished = false;
        }
    }

    public boolean runConfiguration(OsmoticConfiguration newConfiguration, Connection conn, DSLContext context) {
        stop(conn, context);
        init = false;
        this.conf = newConfiguration;
        if (!init(conn, context)) {
            stop(conn, context);
            return false;
        }
        start(conn, context);
        return true;
    }

    private boolean init(Connection conn, DSLContext context) {
        stop(conn, context); // ensuring that the previous simulation was stopped
        if (init) return init;
        Calendar calendar = Calendar.getInstance();

        // Getting configuration from json and entering classes to Agent Broker
        if (agentBrokerageInitFails(RA)) return init;

        allocateOrClearDataStructures(calendar);

        osmoticBroker = LegacyTopologyBuilder.newBroker(); // TODO: new OsmoticBroker(conf.OsmesisBroker, edgeLetId);
        MELSwitchPolicy melSwitchPolicy = MELRoutingPolicyGeneratorFacade.generateFacade(conf.mel_switch_policy);
        osmoticBroker.setMelRouting(melSwitchPolicy);

        topologyBuilder = new LegacyTopologyBuilder(osmoticBroker);
        {
            var confFile = fileExists(conf.configurationFile);
            if (confFile != null) {
                LegacyConfiguration config = LegacyConfiguration.fromFile(confFile);
                if(config !=  null) {
                    try {
                        topologyBuilder.buildTopology(config);
                    } catch (Exception e) {
                        e.printStackTrace();
                        return init;
                    }
                }
            }
        }


        OsmosisOrchestrator conductor = new OsmosisOrchestrator();
        appList = OsmoticAppsParser.legacyAppParser(conf.osmesisAppFile);
        List<SDNController> controllers = new ArrayList<>();
        for(OsmoticDatacenter osmesisDC : topologyBuilder.getOsmesisDatacentres()){
            osmoticBroker.submitVmList(osmesisDC.getVmList(), osmesisDC.getId());
            controllers.add(osmesisDC.getSdnController());
            osmesisDC.getSdnController().setWanOorchestrator(conductor);
        }
        controllers.add(topologyBuilder.getSdWanController());
        conductor.setSdnControllers(controllers);
        osmoticBroker.submitOsmesisApps(appList);
        osmoticBroker.setDatacenters(topologyBuilder.getOsmesisDatacentres());

        init = true;
        return init;
    }

    private void allocateOrClearDataStructures(Calendar calendar) {
        MainEventManager.init(conf.num_user, calendar, conf.trace_flag);
        if (conf.terminate_simulation_at > 0)
            MainEventManager.terminateSimulation(conf.terminate_simulation_at);
    }

    /**
     * This part only initializes the AgentBrokering architecture, and not the actual simulator.
     * @return
     */
    private boolean agentBrokerageInitFails(String RA) {
        if (fileExists(conf.AGENT_CONFIG_FILE) != null) {
            // Set Agent and Message classes
            AgentBroker agentBroker = AgentBroker.getInstance();

            // Getting configuration from json and entering classes to Agent Broker
            AgentConfigProvider provider = null;
            try {
                provider = new AgentConfigProvider(AgentConfigLoader.getFromFile(conf.AGENT_CONFIG_FILE));
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                return true;
            }

            // In this example, the Central Agent is not used
            try {
                agentBroker.setDcAgentClass(provider.getDCAgentClass());
                agentBroker.setAgentMessageClass(provider.getAgentMessageClass());
                if (Objects.equals(RoutingAlgorithmGeneratorFactory.generateFacade(RA).getName(), "custom")) {
                    agentBroker.setDeviceAgentClass(provider.getDeviceAgentClass());
                    agentBroker.setCentralAgentClass(provider.getCentralAgentClass());
                } else {
                    agentBroker.setDeviceAgentClass(Class.forName(RoutingAlgorithmGeneratorFactory.generateFacade(RA).getDevice_agent_class()));
                    agentBroker.setCentralAgentClass(Class.forName(RoutingAlgorithmGeneratorFactory.generateFacade(RA).getCentral_agent_class()));
                }
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
                return true;
            }

            //Simulation is not started yet thus there is not any MELs.
            //Links for Agents between infrastructure elements.
            for (TopologyLink link : provider.getTopologyLinks()) {
                agentBroker.addAgentLink(link.AgentA, link.AgentB);
            }

            //Osmotic Agents time interval
            agentBroker.setMAPEInterval(provider.getMAPEInterval());

            if (fileExists(conf.RES_CONFIG_FILE) != null) {
                RESResponse resResponse = null;
                try {
                    resResponse = AppConfig.RES_PARSER.parse(conf.RES_CONFIG_FILE);
                } catch (IOException e) {
                    e.printStackTrace();
                    return true;
                }
                energyControllers = resResponse
                        .getDatacenters()
                        .stream()
                        .map(EnergyController::fromDatacenter)
                        .collect(Collectors.toMap(EnergyController::getEdgeDatacenterId, Function.identity()));
                agentBroker.setEnergyControllers(energyControllers);
            }

            if (conf.simulationStartTime != null && (!conf.simulationStartTime.isEmpty())) {
                agentBroker.setSimulationStartTime(conf.simulationStartTime);
            }

            agentBroker.initializeCentralAgentIfRequired();
        }
        return false;
    }

    private void start(Connection conn, DSLContext context) {
        init(conn, context); // Ensuring that the simulation is started
        runTime = MainEventManager.startSimulation(conn, context);
        finished = true;
    }

    public void legacy_log() {
        if (finished) {
            LogUtil.logger.trace("Simulation finished...");
            PrintResults pr = new PrintResults();
            pr.collectTrustworthyBatteryData(osmoticBroker.getDevices());
            pr.collectNetworkData(appList, osmoticBroker);

            for(OsmoticDatacenter osmesisDC : topologyBuilder.getOsmesisDatacentres()){
                pr.collectDataCenterData(osmesisDC.getName(),
                        osmesisDC.getSdnhosts(),
                        osmesisDC.getSdnController().getSwitchList(),
                        runTime);
            }

            pr.collectDataCenterData(topologyBuilder.getSdWanController().getName(), null, topologyBuilder.getSdWanController().getSwitchList(), runTime);

            if (energyControllers != null) {
                RESPrinter res_printer = new RESPrinter();
                res_printer.postMortemAnalysis(energyControllers,
                                                conf.simulationStartTime,
                                   true,
                                       1,
                                                appList);
            }
            LogUtil.logger.trace("End of RES analysis!");
        }
    }


    public void log(GlobalConfigurationSettings conf, Connection conn, DSLContext context) {
        if (finished) {
            LogUtil.logger.trace("Simulation finished...");
            bandwidthInfoList = OsmosisOrchestrator.getBandwidthShareInfo();
            PrintResults pr = new PrintResults();
            pr.collectTrustworthyBatteryData(osmoticBroker.getDevices());
            pr.collectNetworkData(appList, osmoticBroker);
            pr.collectBandwidthInfo(bandwidthInfoList);

            for(OsmoticDatacenter osmesisDC : conf.conf.osmesisDatacentres){
                pr.collectDataCenterData(osmesisDC.getName(),
                        osmesisDC.getSdnhosts(),
                        osmesisDC.getSdnController().getSwitchList(),
                        runTime);
            }

            pr.collectDataCenterData(conf.sdWanController.getName(), null, conf.sdWanController.getSwitchList(), runTime);

            if (energyControllers != null) {
                RESPrinter res_printer = new RESPrinter();
                res_printer.postMortemAnalysis(energyControllers, conf.simulationStartTime, true,1, appList);
            }

            if (conf.output_simulation_file != null)
                pr.dumpCSV(new File(conf.output_simulation_file));

            try {
                pr.write_to_SQL(conn);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public boolean runConfiguration(GlobalConfigurationSettings conf, Connection conn, DSLContext context) {
        stop(conn, context);
        init = false;
        this.conf = conf.asPreviousOsmoticConfiguration();
        if (!init(conf, conn, context)) {
            stop(conn, context);
            return false;
        }
        start(conn, context);
        return true;
    }

    private boolean init(GlobalConfigurationSettings conf, Connection conn, DSLContext context) {
        stop(conn, context); // ensuring that the previous simulation was stopped
        if (init) return init;
        Calendar calendar = Calendar.getInstance();
        // Getting configuration from json and entering classes to Agent Broker
        if (agentBrokerageInitFails(RA)) return init;
        allocateOrClearDataStructures(calendar);

        String name = null, entitiesList;
        List<SimEntity> newEntities = new ArrayList<>();
        if(time_conf.get().getIsBatch() && !time_conf.get().getIsFirstBatch()) {
            name = time_conf.get().getQueueFilePath();
            entitiesList = "entities.ser";
            newEntities = MainEventManager.deserializeEntities(name + entitiesList);
        }
        osmoticBroker = (time_conf.get().getIsBatch() && !time_conf.get().getIsFirstBatch()) ? (OsmoticBroker) newEntities.get(2) :  conf.newBroker();
        if(time_conf.get().getIsBatch() && !time_conf.get().getIsFirstBatch()) {
            MainEventManager.addEntity(osmoticBroker);
            osmoticBroker.setIsWakeupStartSet(false);
            osmoticBroker.setFullInterval(time_conf.get().getBatchStart(), time_conf.get().getBatchEnd());
        }
        MELSwitchPolicy melSwitchPolicy = MELRoutingPolicyGeneratorFacade.generateFacade(conf.mel_switch_policy);
        osmoticBroker.setMelRouting(melSwitchPolicy);
        conf.buildTopologyForSimulator(osmoticBroker, RA);

        OsmosisOrchestrator conductor = new OsmosisOrchestrator();
        List<SDNController> controllers = new ArrayList<>();
        for(OsmoticDatacenter osmesisDC : conf.conf.osmesisDatacentres) {
            osmoticBroker.submitVmList(osmesisDC.getVmList(), osmesisDC.getId());
            controllers.add(osmesisDC.getSdnController());
            osmesisDC.getSdnController().setWanOorchestrator(conductor);
        }

        controllers.add(conf.sdWanController);
        conductor.setSdnControllers(controllers);
        appList = (time_conf.get().getIsBatch() && !time_conf.get().getIsFirstBatch()) ?  MainEventManager.deserializeAppList(name +"appList.ser") : osmoticBroker.submitWorkloadCSVApps(conf.apps);
        osmoticBroker.setAppList(appList);
        osmoticBroker.setDatacenters(conf.conf.osmesisDatacentres);
        osmoticBroker.setDeltaVehUpdate(conf.simulation_step);
        osmoticBroker.setIoTTraces(new IoTEntityGenerator(new File(conf.iot_traces), null, conn, context));

        init = true;
        return init;
    }
}
