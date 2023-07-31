/*
 * TopologyLink.java
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

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import org.cloudbus.cloudsim.edge.core.edge.LegacyConfiguration;
import uk.ncl.giacomobergami.utils.data.CSVMediator;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

@JsonPropertyOrder({"network", "source", "destination", "bandwidth"})
public class TopologyLink {
    public String network;
    public String source;
    public String destination;
    public double bandwidth;

    public TopologyLink() {}

    public TopologyLink(String network, String source, String destination, double bandwidth) {
        this.network = network;
        this.source = source;
        this.destination = destination;
        this.bandwidth = bandwidth;
    }

    public TopologyLink(String network, LegacyConfiguration.LinkEntity onta) {
        this.network = network;
        this.source = onta.getSource();
        this.destination = onta.getDestination();
        this.bandwidth = onta.getBw();
    }

    public String leftProjection() {
        return network;
    }

    private static CSVMediator<TopologyLink> readerWriter = null;
    public static CSVMediator<TopologyLink> csvReader() {
        if (readerWriter == null)
            readerWriter = new CSVMediator<>(TopologyLink.class);
        return readerWriter;
    }

    public LegacyConfiguration.LinkEntity rightProjection() {
        var a = new LegacyConfiguration.LinkEntity();
        a.setBw(bandwidth);
        a.setSource(source);
        a.setDestination(destination);
        return a;
    }

    public static Map<String, Collection<LegacyConfiguration.LinkEntity>> asNetworkedLinks(File csv) {
        Map<String, Collection<LegacyConfiguration.LinkEntity>> result = new HashMap<>();
        var reader = csvReader().beginCSVRead(csv);
        while (reader.hasNext()) {
            var x = reader.next();
            result.computeIfAbsent(x.leftProjection(), s -> new HashSet<>()).add(x.rightProjection());
        }
        return result;
    }

    public static Map<String, Collection<LegacyConfiguration.LinkEntity>> asNetworkedLinks(Collection<TopologyLink> links) {
        Map<String, Collection<LegacyConfiguration.LinkEntity>> result = new HashMap<>();
        if (links != null) for (var x : links) {
            result.computeIfAbsent(x.leftProjection(), s -> new HashSet<>()).add(x.rightProjection());
        }
        return result;
    }

    public String getNetwork() {
        return network;
    }
    public void setNetwork(String network) {
        this.network = network;
    }
    public String getSource() {
        return source;
    }
    public void setSource(String source) {
        this.source = source;
    }
    public String getDestination() {
        return destination;
    }
    public void setDestination(String destination) {
        this.destination = destination;
    }
    public double getBandwidth() {
        return bandwidth;
    }
    public void setBandwidth(double bandwidth) {
        this.bandwidth = bandwidth;
    }
}
