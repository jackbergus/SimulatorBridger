#!/usr/bin/env python3
# -*- coding: utf-8 -*-
#
# Copyright 2022 Giacomo Bergami
#
# This file is part of MaxCDijkstra
#
# This program is free software; you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation; either version 2 of the License, or
# (at your option) any later version.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with this program; if not, see <http://www.gnu.org/licenses/>.
import pandas

class MyGraph(object):
    def __init__(self):
        self.outgoing_edges = dict()
        self.edge_weight = dict()
        self.charge_power = dict()

    def add_edge(self, src:int, dst:int, weight:float):
        if src not in self.outgoing_edges:
            self.outgoing_edges[src] = set()
        self.outgoing_edges[src].add(dst)
        self.edge_weight[(src, dst)] = weight

    def out_edges(self, src)->set:
        if not src in self.outgoing_edges:
            return set()
        else:
            return self.outgoing_edges[src]

    def weight(self, src, dst)->float:
        cp = (src, dst)
        if not cp in self.edge_weight:
            return 0
        else:
            return self.edge_weight[cp]

    def load_edges_csv(self, filename):
        for _, row in pandas.read_csv(filename, names=["src", "dst", "weight"]).iterrows():
            self.add_edge(row["src"], row["dst"], row["weight"])

    def load_charging_power(self, filename):
        for _, row in pandas.read_csv(filename, names=["node", "power"]).iterrows():
            self.charge_power[row["node"]] = row["power"]

    def get_charging_stations(self, fun=lambda x: True):
        for node in self.charge_power:
            if (self.charge_power[node] > 0.0) and (fun(node)):
                yield node

    def get_charge_per_time_unit(self, node):
        if node in self.charge_power:
            return self.charge_power[node]
        else:
            return 0.0

    def is_charging_station(self, node):
        if node in self.charge_power:
            return self.charge_power[node] > 0.0
        else:
            return False

    def initNRecharges(self):
        d = dict()
        for node in self.charge_power:
            d[node] = 0
        return d

    def infNRecharges(self):
        d = dict()
        for node in self.charge_power:
            d[node] = float('inf')
        return d

    def countTotalPowerUsedInRecharge(self, nRecharges):
        total = 0.0
        for x in nRecharges:
            total = total + self.get_charge_per_time_unit(x) * nRecharges[x]
        return total

    def estimatePathCharge(self, nRecharges, path, initCharge, maxRecharge):
        i = 0
        total = initCharge
        onlyCost = 0
        for curr in path:
            if i == 0:
                if curr in nRecharges:
                    total = min(total + self.get_charge_per_time_unit(curr) * nRecharges[curr], maxRecharge)
            else:
                w = self.weight(path[i-1], curr)
                total = total - w
                onlyCost = onlyCost + w
                if curr in nRecharges:
                    total = min(total + self.get_charge_per_time_unit(curr) * nRecharges[curr], maxRecharge)
            i = i+1
        return (total, onlyCost)

