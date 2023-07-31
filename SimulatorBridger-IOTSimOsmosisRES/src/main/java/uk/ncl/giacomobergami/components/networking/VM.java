/*
 * VM.java
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

import org.cloudbus.cloudsim.edge.core.edge.LegacyConfiguration;
import uk.ncl.giacomobergami.utils.data.CSVMediator;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class VM {
    public String name;
    public double bw;
    public double mips;
    public int ram;
    public int pes;
    public String cloudletPolicy;
    public long storage;

    private static CSVMediator<VM> readerWriter = null;
    public static CSVMediator<VM> csvReader() {
        if (readerWriter == null)
            readerWriter = new CSVMediator<>(VM.class);
        return readerWriter;
    }

    public VM() {}

    public VM(String name, double bw, double mips, int ram, int pes, String cloudletPolicy, long storage) {
        this.name = name;
        this.bw = bw;
        this.mips = mips;
        this.ram = ram;
        this.pes = pes;
        this.cloudletPolicy = cloudletPolicy;
        this.storage = storage;
    }

    public VM(LegacyConfiguration.MELEntities x) {
        name = x.getName();
        bw = x.getBw();
        mips = x.getMips();
        ram = x.getRam();
        pes = x.getPesNumber();
        cloudletPolicy = x.getCloudletSchedulerClassName();
        storage = Integer.MAX_VALUE;
    }

    public VM(LegacyConfiguration.VMEntity x) {
        name = x.getName();
        bw = x.getBw();
        mips = x.getMips();
        ram = x.getRam();
        pes = x.getPes();
        cloudletPolicy = x.getCloudletPolicy();
        storage = (int)x.getStorage();
    }

    public LegacyConfiguration.MELEntities asLegacyMELEntity() {
        var result = new LegacyConfiguration.MELEntities();
        result.setName(name);
        result.setBw(bw);
        result.setMips((int)mips);
        result.setRam(ram);
        result.setPesNumber(pes);
        result.setCloudletSchedulerClassName(cloudletPolicy);
        result.setVmm("xxx");
        return result;
    }

    public LegacyConfiguration.VMEntity asLegacyVMEntity() {
        var result = new LegacyConfiguration.VMEntity();
        result.setName(name);
        result.setBw(bw);
        result.setMips(mips);
        result.setRam(ram);
        result.setPes(pes);
        result.setCloudletPolicy(cloudletPolicy);
        result.setStorage(storage);
        return result;
    }

    public static List<LegacyConfiguration.MELEntities> asLegacyMELEntity(File name) {
        var reader = csvReader().beginCSVRead(name);
        ArrayList<LegacyConfiguration.MELEntities> ls = new ArrayList<>();
        while (reader.hasNext()) {
            ls.add(reader.next().asLegacyMELEntity());
        }
        return ls;
    }

    public static List<LegacyConfiguration.VMEntity> asLegacyVMEntity(File name) {
        var reader = csvReader().beginCSVRead(name);
        ArrayList<LegacyConfiguration.VMEntity> ls = new ArrayList<>();
        while (reader.hasNext()) {
            ls.add(reader.next().asLegacyVMEntity());
        }
        return ls;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getBw() {
        return bw;
    }

    public void setBw(double bw) {
        this.bw = bw;
    }

    public double getMips() {
        return mips;
    }

    public void setMips(double mips) {
        this.mips = mips;
    }

    public int getRam() {
        return ram;
    }

    public void setRam(int ram) {
        this.ram = ram;
    }

    public int getPes() {
        return pes;
    }

    public void setPes(int pes) {
        this.pes = pes;
    }

    public String getCloudletPolicy() {
        return cloudletPolicy;
    }

    public void setCloudletPolicy(String cloudletPolicy) {
        this.cloudletPolicy = cloudletPolicy;
    }

    public long getStorage() {
        return storage;
    }

    public void setStorage(long storage) {
        this.storage = storage;
    }
}
