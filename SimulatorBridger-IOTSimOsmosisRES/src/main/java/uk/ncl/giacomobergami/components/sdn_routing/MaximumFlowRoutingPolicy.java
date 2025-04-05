/*
 * MaximumFlowRoutingPolicy.java
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

package uk.ncl.giacomobergami.components.sdn_routing;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import org.cloudbus.cloudsim.sdn.Link;
import org.cloudbus.cloudsim.sdn.NetworkNIC;
import org.cloudbus.cloudsim.sdn.SDNHost;
import org.cloudbus.osmosis.core.Flow;
import org.jooq.DSLContext;
import org.jooq.Result;
import uk.ncl.giacomobergami.components.OsmoticRunner;
import uk.ncl.giacomobergami.utils.database.jooq.tables.Sourcetodestlinks;
import uk.ncl.giacomobergami.utils.database.jooq.tables.records.SourcetodestlinksRecord;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 *
 * @author Giacomo Bergami
 * @contact bergamigiacomo@gmail.com
 * @since SimulatorBridger
 */
public class MaximumFlowRoutingPolicy extends SDNRoutingPolicy {
    @Override @Deprecated
    public void updateSDNNetworkGraph(Connection conn, DSLContext context, NetworkNIC src, NetworkNIC dest) { throw new RuntimeException("Unexpected method call!"); }


    Table<Integer, Integer, List<NetworkNIC>> table = HashBasedTable.create();
    Table<Integer, Integer, List<Link>> linkTable = HashBasedTable.create();
    Table<String, String, List<NetworkNIC>> table2 = HashBasedTable.create();
    Table<String, String, List<Link>> linkTable2 = HashBasedTable.create();

    @Override
    public List<NetworkNIC> buildRoute(NetworkNIC srcHost,
                                       NetworkNIC destHost,
                                       Flow pkt, Connection conn, DSLContext context) {
        var attempt = table.get(pkt.getOrigin(), pkt.getDestination());
        if (attempt != null) {
            var mostUpdated = table2.get(srcHost.getName(), destHost.getName());
            if ((mostUpdated != null) && !mostUpdated.equals(attempt)) {
                table.put(pkt.getOrigin(), pkt.getDestination(), mostUpdated);
                linkTable.put(pkt.getOrigin(), pkt.getDestination(), linkTable2.get(srcHost.getName(), destHost.getName()));
                return mostUpdated;
            } else
                return attempt;
        }
        attempt = table2.get(srcHost.getName(), destHost.getName());
        if ((attempt == null) || (attempt.isEmpty()))
            throw new RuntimeException("ERROR: path was unexpectedly missing! "+srcHost+"-->"+destHost+" @"+pkt);
        table.put(pkt.getOrigin(), pkt.getDestination(), attempt);
        linkTable.put(pkt.getOrigin(), pkt.getDestination(), linkTable2.get(srcHost.getName(), destHost.getName()));
        return attempt;
    }

    @Override
    public List<NetworkNIC> getRoute(int source, int dest) { return table.get(source, dest); }

    @Override
    public List<Link> getLinks(int source, int dest) { return linkTable.get(source, dest); }

    public void setNewPaths(Collection<List<String>> value,
                            SDNRoutingPolicy actualPolicy, Connection conn, DSLContext context) throws SQLException {
        for (var path : value) {
            List<Link> linkList = new ArrayList<>();
            var ls = path.stream().map(actualPolicy::inefficientNodeByName).collect(Collectors.toList());
            for (int i = 0, N = ls.size()-1; i<N; i++) {
                var srcNode = ls.get(i);
                var destNode = ls.get(i + 1);
                Link linkWithHighestBW = null;
                if(OsmoticRunner.deltaTime > 1) {
                    if (actualPolicy.topology.numLinks.get(srcNode.getAddress(), destNode.getAddress()) == 1 && actualPolicy.topology.numLinks.get(destNode.getAddress(), srcNode.getAddress()) == 1) {
                        linkWithHighestBW = topology.getLink(srcNode.getAddress(), destNode.getAddress());
                    } else {
                        List<Link> links = actualPolicy.topology.getNodeToNodeLinks(srcNode, destNode);
                        if ((links == null) || (links.isEmpty())) {
                            throw new RuntimeException("ERROR: expected link between " + ls.get(i) + " and " + ls.get(i + 1));
                        }
                        /*
                         * From LoadBalancing code:
                         * Sometimes two nodes are connected via two links; therefore, find the max BW among the links!
                         */
                        double bw = 0;
                        for (Link l : links) {
                            int numberChannel = l.getChannelCount();
                            if (numberChannel == 0 || srcNode instanceof SDNHost || destNode instanceof SDNHost) { // i think you may need to look the logic again!
                                numberChannel = 1; // we cannot divide by 0
                            } else {
                                numberChannel++; // 1 for exisiting one , and one for this one
                            }
                            double currentBw = l.getBw() / numberChannel;
                            if (currentBw > bw) {
                                // link bw does not change, instead you need to get the bw and number of channel on the link
                                bw = currentBw;
                                linkWithHighestBW = l;
                            }
                        }
                    }
                } else {
                    int bestID = linkWithHighestBW(context.select().from(Sourcetodestlinks.SOURCETODESTLINKS)
                            .where("from_ID in ( " + srcNode.getAddress() + "," + destNode.getAddress() + ")"
                                    + " AND to_ID in (" + destNode.getAddress() + "," + srcNode.getAddress() + ")")
                            .fetchInto(Sourcetodestlinks.SOURCETODESTLINKS).sortAsc(Sourcetodestlinks.SOURCETODESTLINKS.LINK_ID));
                    linkWithHighestBW = this.getTopology().getLinkfromMap(bestID);
                }
                linkList.add(linkWithHighestBW);
            }
            Collections.reverse(ls);
            Collections.reverse(linkList);
            linkTable2.put(path.get(0), path.get(path.size()-1), linkList);
            table2.put(path.get(0), path.get(path.size()-1), ls);
        }
    }

    private int linkWithHighestBW(Result<SourcetodestlinksRecord> linksData) {
        double bestLink = 0;
        int j = 0;
        for (int i = 0; i < linksData.size(); i++) {
             SourcetodestlinksRecord link = linksData.get(i);
            int noChannels = link.get(Sourcetodestlinks.SOURCETODESTLINKS.NOCHANNELS)+1;// == 0 ? 1 : link.get(Sourcetodestlinks.SOURCETODESTLINKS.NOCHANNELS);
            double bwPerChannel = link.get(Sourcetodestlinks.SOURCETODESTLINKS.BW) / noChannels;
            if (bwPerChannel > bestLink) {
                bestLink = bwPerChannel;
                j = i;
            }
        }
        return linksData.get(j).get(Sourcetodestlinks.SOURCETODESTLINKS.LINK_ID);
    }

    public void setNewPaths(Collection<List<String>> value, Connection conn, DSLContext context) throws SQLException {
        setNewPaths(value, this, conn, context);
    }
}
