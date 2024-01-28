/*
 * OsmoticRunner.java
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

package uk.ncl.giacomobergami.components;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.jooq.DSLContext;
import uk.ncl.giacomobergami.components.loader.GlobalConfigurationSettings;
import uk.ncl.giacomobergami.components.simulator.OsmoticConfiguration;
import uk.ncl.giacomobergami.components.simulator.OsmoticWrapper;
import uk.ncl.giacomobergami.utils.data.JSON;

import java.io.File;
import java.sql.Connection;
import java.util.List;

public class OsmoticRunner {

    static {
        File file = new File("log4j2.xml");
        LoggerContext context = (LoggerContext) LogManager.getContext(false);
        context.setConfigLocation(file.toURI());
    }

    private static OsmoticWrapper obj;

    public static OsmoticWrapper generateFacade() {
        if (obj == null) {
            obj = new OsmoticWrapper();
        }
        return obj;
    }

    @Deprecated
    public static void legacyOrchestrate(String configuration, Connection conn, DSLContext context) {
        List<OsmoticConfiguration> ls = JSON.stringToArray(new File(configuration), OsmoticConfiguration[].class);
        if (ls.isEmpty()) return;
        OsmoticWrapper conv = generateFacade();
        for (var y : ls) {
            conv.runConfiguration(y, conn, context);
        }
        conv.stop(conn, context);
        conv.legacy_log();
    }

    public static void runFromConfiguration(GlobalConfigurationSettings conf, Connection conn, DSLContext context) {
        var conv = new OsmoticWrapper(conf.asPreviousOsmoticConfiguration());
        conv.runConfiguration(conf, conn, context);
        conv.stop(conn, context);
        conv.log(conf, conn, context);
    }

    @Deprecated
    public static void runFromDump(String configuration, Connection conn, DSLContext context) {
        var conf = GlobalConfigurationSettings.readFromYAML(new File(configuration));
        runFromConfiguration(GlobalConfigurationSettings.readFromYAML(new File(configuration)), conn, context);
    }
}
