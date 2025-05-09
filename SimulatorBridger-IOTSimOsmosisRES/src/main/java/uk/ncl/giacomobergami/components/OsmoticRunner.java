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
import uk.ncl.giacomobergami.components.iot.IoTDeviceTabularConfiguration;
import uk.ncl.giacomobergami.components.loader.GlobalConfigurationSettings;
import uk.ncl.giacomobergami.components.simulator.OsmoticConfiguration;
import uk.ncl.giacomobergami.components.simulator.OsmoticWrapper;
import uk.ncl.giacomobergami.utils.data.JSON;
import uk.ncl.giacomobergami.utils.database.JavaPostGres;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class OsmoticRunner {

    public static AtomicInteger linkID = new AtomicInteger(0);
    public static double deltaTime;
    public static boolean toUpdate = true;
    public static HashSet<Integer> updatedLinks = new HashSet<>();
    static OsmoticWrapper conv;

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

    public static HashMap<String, Double[]> edgeNodeTelemetry() {
        return conv.edgeNodeTelemetry();
    }

    public static void addIoTDevices(GlobalConfigurationSettings globalConfigurationSettings, List<IoTDeviceTabularConfiguration> deviceList) {
        conv.addIoTDevices(globalConfigurationSettings, deviceList);
        System.out.println("Global Device List Updated");
    }

    public static HashMap<String, Integer> numberOfActiveCommsPerEdge(){
        return conv.numberOfActiveCommsPerEdge();
    }

    public static HashMap<String, Integer> numberOfDevicesPerEdge() {
        return conv.numberOfDevicesPerEdge();
    }

    public static HashMap<String, Double>  currentEnergyConsumption() {
        return conv.currentEnergyConsumption();
    }

    @Deprecated
    public static void legacyOrchestrate(String configuration, Connection conn, DSLContext context, double loopEnd, double deltaTime) {
        List<OsmoticConfiguration> ls = JSON.stringToArray(new File(configuration), OsmoticConfiguration[].class);
        if (ls.isEmpty()) return;
        OsmoticWrapper conv = generateFacade();
        for (var y : ls) {
            conv.runConfiguration(y, conn, context, loopEnd, deltaTime);
        }
        conv.stop(conn, context, deltaTime);
        conv.legacy_log();
    }

    public static void runFromConfiguration(GlobalConfigurationSettings conf, Connection conn, DSLContext context, double loopEnd, double deltaTime) {
        conv = new OsmoticWrapper(conf.asPreviousOsmoticConfiguration());
        conv.runConfiguration(conf, conn, context, loopEnd, deltaTime);
        try {
            JavaPostGres.indexLINKSDATA(conn);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        //conv.stop(conn, context);
        //conv.log(conf, conn, context);
    }

        public static void LogOutput(GlobalConfigurationSettings conf, Connection conn, DSLContext context, Double endTime) {
        conv.log(conf, conn, context, endTime);
    }

    @Deprecated
    public static void runFromDump(String configuration, Connection conn, DSLContext context, double loopEnd, double deltaTime) {
        var conf = GlobalConfigurationSettings.readFromYAML(new File(configuration));
        runFromConfiguration(GlobalConfigurationSettings.readFromYAML(new File(configuration)), conn, context, loopEnd, deltaTime);
    }
}
