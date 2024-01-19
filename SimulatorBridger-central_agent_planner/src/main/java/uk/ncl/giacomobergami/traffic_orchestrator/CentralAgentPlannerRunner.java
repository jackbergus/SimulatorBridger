/*
 * CentralAgentPlannerRunner.java
 * This file is part of SimulatorBridger-central_agent_planner
 *
 * Copyright (C) 2022 - Giacomo Bergami
 *
 * SimulatorBridger-central_agent_planner is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * SimulatorBridger-central_agent_planner is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with SimulatorBridger-central_agent_planner. If not, see <http://www.gnu.org/licenses/>.
 */


package uk.ncl.giacomobergami.traffic_orchestrator;

import uk.ncl.giacomobergami.utils.data.YAML;
import uk.ncl.giacomobergami.utils.pipeline_confs.OrchestratorConfiguration;
import uk.ncl.giacomobergami.utils.pipeline_confs.TrafficConfiguration;

import java.io.File;
import java.sql.SQLException;
import java.util.Optional;

public class CentralAgentPlannerRunner {
    private static Class<?> clazz;
    private static PreSimulatorEstimator obj;

    public static PreSimulatorEstimator generateFacade(OrchestratorConfiguration conf,
                                                       TrafficConfiguration conf2) throws SQLException {
        if (obj == null) {
            obj = new PreSimulatorEstimator(conf, conf2);
        }
        return obj;
    }

    public static void orchestrate(String conf2,
                                   String conf1) {
        Optional<OrchestratorConfiguration> conf = YAML.parse(OrchestratorConfiguration.class, new File(conf2));
        Optional<TrafficConfiguration> conf3 = YAML.parse(TrafficConfiguration.class, new File(conf1));
        conf.ifPresent(x -> conf3.ifPresent(y -> {
            PreSimulatorEstimator conv = null;
            try {
                conv = generateFacade(x, y);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
            conv.run();
            try {
                conv.serializeAll();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }));
    }

    public static void main(String[] args) {
        String configuration = "orchestrator.yaml";
        String converter = "orchestrator.yaml";
        if (args.length >= 2) {
            configuration = args[0];
            converter = args[1];
        }
        orchestrate(configuration, converter);
    }
}
