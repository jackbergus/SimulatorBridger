/*
 * Title:        IoTSim-Osmosis-RES 1.0
 * Description:  IoTSim-Osmosis-RES enables the testing and validation of osmotic computing applications
 * 			     over heterogeneous edge-cloud SDN-aware environments powered by the Renewable Energy Sources.
 *
 * Licence:      GPL - http://www.gnu.org/copyleft/gpl.html
 *
 * Copyright (c) 2021, Newcastle University (UK) and Saudi Electronic University (Saudi Arabia) and
 *                     AGH University of Science and Technology (Poland)
 *
 */

package org.cloudbus.cloudsim.osmesis.examples;

import org.cloudbus.agent.AgentBroker;
import org.cloudbus.agent.config.AgentConfigLoader;
import org.cloudbus.agent.config.AgentConfigProvider;
import org.cloudbus.agent.config.TopologyLink;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.MainEventManager;
import org.cloudbus.cloudsim.edge.core.edge.LegacyConfiguration;
import org.cloudbus.cloudsim.edge.utils.LogUtil;
import org.cloudbus.cloudsim.osmesis.examples.uti.LogPrinter;
import org.cloudbus.cloudsim.osmesis.examples.uti.PrintResults;
import org.cloudbus.cloudsim.osmesis.examples.uti.RESPrinter;
import org.cloudbus.cloudsim.sdn.Switch;
import org.cloudbus.osmosis.core.*;
import org.cloudbus.res.EnergyController;
import org.cloudbus.res.config.AppConfig;
import org.cloudbus.res.dataproviders.res.RESResponse;
import uk.ncl.giacomobergami.components.mel_routing.MELRoutingPolicyGeneratorFacade;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * A complex example that use Osmotic Agents. MEL's are available on two Edges.
 *
 */

public class RES_example6 {
    //Workload and infrastructure configuration are the same as in the example 2.
    public static final String configurationFile = "inputFiles/res/RES_example6_infrastructure_2edges.json";
    public static final String osmesisAppFile =  "inputFiles/res/RES_example2_workload_single_day.csv";
    //RES configuration is the same as in the example 1.
    public static final String RES_CONFIG_FILE =  "inputFiles/res/RES_example6_energy_config.json";
    public static final String AGENT_CONFIG_FILE="inputFiles/agent/RES_example6_agent_config.json";

    LegacyTopologyBuilder topologyBuilder;
    OsmoticBroker osmesisBroker;
    EdgeSDNController edgeSDNController;

    public static void main(String[] args) throws Exception {
        RES_example6 osmosis = new RES_example6();
        osmosis.start();
    }

    public void start() throws Exception{

        int num_user = 1; // number of users
        Calendar calendar = Calendar.getInstance();
        boolean trace_flag = false; // mean trace events

        // Set Agent and Message classes
        AgentBroker agentBroker = AgentBroker.getInstance();

        // Getting configuration from json and entering classes to Agent Broker
        AgentConfigProvider provider = new AgentConfigProvider(AgentConfigLoader.getFromFile(AGENT_CONFIG_FILE));

        // In this example, the Central Agent is not used
        agentBroker.setDcAgentClass(provider.getDCAgentClass());
        agentBroker.setDeviceAgentClass(provider.getDeviceAgentClass());
        agentBroker.setAgentMessageClass(provider.getAgentMessageClass());

        //Simulation is not started yet thus there is not any MELs.
        //Links for Agents between infrastructure elements.
        for (TopologyLink link : provider.getTopologyLinks()) {
            agentBroker.addAgentLink(link.AgentA, link.AgentB);
        }

        //Osmotic Agents time interval
        agentBroker.setMAPEInterval(provider.getMAPEInterval());

        //Create Energy Controllers
        Map<String, EnergyController> energyControllers = getEnergyControllers();
        System.out.println(energyControllers);

        agentBroker.setEnergyControllers(energyControllers);

        //Set the simulation start time
        //String simulationStartTime="20160101:0000";
        //String simulationStartTime="20160501:0000";
        String simulationStartTime="20160901:0000";

        agentBroker.setSimulationStartTime(simulationStartTime);

        // Initialize the CloudSim library
        MainEventManager.init(num_user, calendar, trace_flag);
        osmesisBroker  = new OsmoticBroker("OsmesisBroker", edgeLetId);
        osmesisBroker.setMelRouting(MELRoutingPolicyGeneratorFacade.generateFacade(null));
        topologyBuilder = new LegacyTopologyBuilder(osmesisBroker);

        LegacyConfiguration config = buildTopologyFromFile(configurationFile);
        //
        if(config !=  null) {
            topologyBuilder.buildTopology(config);
        }

        OsmosisOrchestrator maestro = new OsmosisOrchestrator();

        OsmoticAppsParser.startParsingCSVAppFile(osmesisAppFile);
        List<SDNController> controllers = new ArrayList<>();
        for(OsmoticDatacenter osmesisDC : topologyBuilder.getOsmesisDatacentres()){
            osmesisBroker.submitVmList(osmesisDC.getVmList(), osmesisDC.getId());
            controllers.add(osmesisDC.getSdnController());
            osmesisDC.getSdnController().setWanOorchestrator(maestro);
        }
        controllers.add(topologyBuilder.getSdWanController());
        maestro.setSdnControllers(controllers);
        osmesisBroker.submitOsmesisApps(OsmoticAppsParser.appList);
        osmesisBroker.setDatacenters(topologyBuilder.getOsmesisDatacentres());

        double startTime = MainEventManager.startSimulation();

        LogUtil.simulationFinished();
        PrintResults pr = new PrintResults();
        pr.printOsmesisNetwork();

        Log.printLine();

        for(OsmoticDatacenter osmesisDC : topologyBuilder.getOsmesisDatacentres()){
            List<Switch> switchList = osmesisDC.getSdnController().getSwitchList();
            LogPrinter.printEnergyConsumption(osmesisDC.getName(), osmesisDC.getSdnhosts(), switchList, startTime);
            Log.printLine();
        }

        Log.printLine();
        LogPrinter.printEnergyConsumption(topologyBuilder.getSdWanController().getName(), null, topologyBuilder.getSdWanController().getSwitchList(), startTime);
        Log.printLine();
        Log.printLine("Simulation Finished!");

        Log.printLine();
        Log.printLine("Post-mortem RES energy analysis!");
        RESPrinter res_printer = new RESPrinter();
        res_printer.postMortemAnalysis(energyControllers,simulationStartTime, true,1);
        //res_printer.postMortemAnalysis(energyControllers,simulationStartTime, false, 36);
        //res_printer.postMortemAnalysis(energyControllers,"20160901:0000", false, 36);
        Log.printLine("End of RES analysis!");
    }

    private Map<String, EnergyController> getEnergyControllers() throws IOException {
        RESResponse resResponse = AppConfig.RES_PARSER.parse(RES_CONFIG_FILE);
        return resResponse.getDatacenters()
                .stream()
                .map(EnergyController::fromDatacenter)
                .collect(Collectors.toMap(EnergyController::getEdgeDatacenterId, Function.identity()));
    }

    private LegacyConfiguration buildTopologyFromFile(String filePath) {
        System.out.println("Creating topology from file " + filePath);
        LegacyConfiguration conf  = null;
        try {
            conf = topologyBuilder.buildTopology(new File(filePath));
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

        System.out.println("Topology built:");
        return conf;
    }

    public void setEdgeSDNController(EdgeSDNController edc) {
        this.edgeSDNController = edc;
    }
}
