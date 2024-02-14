/*
 * DataCenterWithController.java
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

package uk.ncl.giacomobergami.components.networking;

import org.cloudbus.cloudsim.DatacenterCharacteristics;
import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.edge.core.edge.EdgeDataCenter;
import org.cloudbus.cloudsim.edge.core.edge.LegacyConfiguration;
import org.cloudbus.osmosis.core.CloudSDNController;
import org.cloudbus.osmosis.core.EdgeSDNController;

import java.io.Serializable;
import java.util.List;

public class DataCenterWithController implements Serializable {
    public static String limiting;
    public static int communication_limit;
    public double scheduling_interval;
    public String datacenter_name;
    public String datacenter_type;
    public String datacenter_vmAllocationPolicy;
    public String datacenter_architecture;
    public String datacenter_os;
    public String datacenter_vmm;
    public double datacenter_timeZone;
    public double datacenter_costPerSec;
    public double datacenter_costPerMem;
    public double datacenter_costPerStorage;
    public double datacenter_costPerBw;
    public String controller_name;
    public String controller_trafficPolicy;
    public String controller_routingPolicy;

    public DataCenterWithController copy() {
        DataCenterWithController result = new DataCenterWithController();
        result.scheduling_interval = scheduling_interval;
        result.datacenter_name = datacenter_name;
        result.datacenter_type = datacenter_type;
        result.datacenter_vmAllocationPolicy = datacenter_vmAllocationPolicy;
        result.datacenter_architecture = datacenter_architecture;
        result.datacenter_os = datacenter_os;
        result.datacenter_vmm = datacenter_vmm;
        result.datacenter_timeZone = datacenter_timeZone;
        result.datacenter_costPerSec = datacenter_costPerSec;
        result.datacenter_costPerMem = datacenter_costPerMem;
        result.datacenter_costPerStorage = datacenter_costPerStorage;
        result.datacenter_costPerBw = datacenter_costPerBw;
        result.controller_name = controller_name;
        result.controller_trafficPolicy = controller_trafficPolicy;
        result.controller_routingPolicy = controller_routingPolicy;
        return result;
    }

    public DataCenterWithController() {}

    public DataCenterWithController(LegacyConfiguration.CloudDataCenterEntity cloud) {
        scheduling_interval = 0.0;
        datacenter_name = cloud.getName();
        datacenter_type = cloud.getType();
        datacenter_vmAllocationPolicy = cloud.getVmAllocationPolicy();
        datacenter_architecture = "x86";
        datacenter_os = "Linux";
        datacenter_vmm = "Xen";
        datacenter_timeZone = 10.0;
        datacenter_costPerSec = 3.0;
        datacenter_costPerMem = 0.05;
        datacenter_costPerStorage = 0.001;
        datacenter_costPerBw = 0.0;
        controller_name = cloud.getControllers().get(0).name;
        controller_trafficPolicy = cloud.getControllers().get(0).trafficPolicy;
        controller_routingPolicy = cloud.getControllers().get(0).routingPolicy;
    }

    public DataCenterWithController(LegacyConfiguration.EdgeDataCenterEntity cloud) {
        scheduling_interval = cloud.getSchedulingInterval();
        datacenter_name = cloud.getName();
        datacenter_type = cloud.getType();
        datacenter_vmAllocationPolicy = cloud.getVmAllocationPolicy().getClassName();
        var features = cloud.getCharacteristics();
        datacenter_architecture = features.getArchitecture();
        datacenter_os = features.getOs();
        datacenter_vmm = features.getVmm();
        datacenter_timeZone = features.getTimeZone();
        datacenter_costPerSec = features.getCostPerSec();
        datacenter_costPerMem = features.getCostPerMem();
        datacenter_costPerStorage = features.getCostPerStorage();
        datacenter_costPerBw = features.getCostPerBw();
        controller_name = cloud.getControllers().get(0).name;
        controller_trafficPolicy = cloud.getControllers().get(0).trafficPolicy;
        controller_routingPolicy = cloud.getControllers().get(0).routingPolicy;
    }

    public CloudSDNController asCloudController() {
        return new CloudSDNController(controller_name, controller_trafficPolicy, controller_routingPolicy);
    }

    public EdgeSDNController asEdgeSDNController(EdgeDataCenter datacenter) {
        return new EdgeSDNController(controller_name, controller_trafficPolicy, controller_routingPolicy, datacenter);
    }

    public DatacenterCharacteristics asDatacenterCharacteristics(List<? extends Host> hostList) {
        return new DatacenterCharacteristics(datacenter_architecture,
                datacenter_os,
                datacenter_vmm,
                hostList,
                datacenter_timeZone,
                datacenter_costPerSec,
                datacenter_costPerMem,
                datacenter_costPerStorage,
                datacenter_costPerBw);
    }

    public static String getLimiting() {
        return limiting;
    }

    public void setLimiting(String limiting) {
        this.limiting = limiting;
    }

    public static int getCommunication_limit() {
        return communication_limit;
    }

    public void setCommunication_limit(int communication_limit) {
        DataCenterWithController.communication_limit = communication_limit;
    }

    public double getScheduling_interval() {
        return scheduling_interval;
    }

    public void setScheduling_interval(double scheduling_interval) {
        this.scheduling_interval = scheduling_interval;
    }

    public String getDatacenter_name() {
        return datacenter_name;
    }

    public void setDatacenter_name(String datacenter_name) {
        this.datacenter_name = datacenter_name;
    }

    public String getDatacenter_type() {
        return datacenter_type;
    }

    public void setDatacenter_type(String datacenter_type) {
        this.datacenter_type = datacenter_type;
    }

    public String getDatacenter_vmAllocationPolicy() {
        return datacenter_vmAllocationPolicy;
    }

    public void setDatacenter_vmAllocationPolicy(String datacenter_vmAllocationPolicy) {
        this.datacenter_vmAllocationPolicy = datacenter_vmAllocationPolicy;
    }

    public String getDatacenter_architecture() {
        return datacenter_architecture;
    }

    public void setDatacenter_architecture(String datacenter_architecture) {
        this.datacenter_architecture = datacenter_architecture;
    }

    public String getDatacenter_os() {
        return datacenter_os;
    }

    public void setDatacenter_os(String datacenter_os) {
        this.datacenter_os = datacenter_os;
    }

    public String getDatacenter_vmm() {
        return datacenter_vmm;
    }

    public void setDatacenter_vmm(String datacenter_vmm) {
        this.datacenter_vmm = datacenter_vmm;
    }

    public double getDatacenter_timeZone() {
        return datacenter_timeZone;
    }

    public void setDatacenter_timeZone(double datacenter_timeZone) {
        this.datacenter_timeZone = datacenter_timeZone;
    }

    public double getDatacenter_costPerSec() {
        return datacenter_costPerSec;
    }

    public void setDatacenter_costPerSec(double datacenter_costPerSec) {
        this.datacenter_costPerSec = datacenter_costPerSec;
    }

    public double getDatacenter_costPerMem() {
        return datacenter_costPerMem;
    }

    public void setDatacenter_costPerMem(double datacenter_costPerMem) {
        this.datacenter_costPerMem = datacenter_costPerMem;
    }

    public double getDatacenter_costPerStorage() {
        return datacenter_costPerStorage;
    }

    public void setDatacenter_costPerStorage(double datacenter_costPerStorage) {
        this.datacenter_costPerStorage = datacenter_costPerStorage;
    }

    public double getDatacenter_costPerBw() {
        return datacenter_costPerBw;
    }

    public void setDatacenter_costPerBw(double datacenter_costPerBw) {
        this.datacenter_costPerBw = datacenter_costPerBw;
    }

    public String getController_name() {
        return controller_name;
    }

    public void setController_name(String controller_name) {
        this.controller_name = controller_name;
    }

    public String getController_trafficPolicy() {
        return controller_trafficPolicy;
    }

    public void setController_trafficPolicy(String controller_trafficPolicy) {
        this.controller_trafficPolicy = controller_trafficPolicy;
    }

    public String getController_routingPolicy() {
        return controller_routingPolicy;
    }

    public void setController_routingPolicy(String controller_routingPolicy) {
        this.controller_routingPolicy = controller_routingPolicy;
    }
}
