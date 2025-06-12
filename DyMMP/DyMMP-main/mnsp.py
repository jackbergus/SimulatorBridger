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
import copy
from collections import defaultdict

from DyMMP.tsp_loop import MyGraph, max_reward_shortest_path, pathWithCost
from DyMMP.tsp_loop import decode
from DyMMP.tsp_loop import create_spanning_tree

def appending_generated_paths(begin, current_start, all_paths, current_pi):
    for path in filter(lambda x : (x.getPath()[0] == current_start), all_paths):
        p = path.getPath()
        found = False
        if not p[-1] == begin:
            for continuation in filter(lambda y : p[-1] == y.getPath()[0], all_paths):
                q = continuation.getPath()
                found = True
                if (q[-1] == current_start):
                    result = pathWithCost(copy.deepcopy(current_pi), continuation.cost, continuation.nRecharges, continuation.vehicleChargeCapacity, continuation.vehicleMaxCapacity)
                    result.getPath().extend(p[1:])
                    result.getPath().extend(q[1:])
                    yield result
                else:
                    result = copy.deepcopy(current_pi)
                    result.extend(p[1:])
                    result.extend(q[1:])
                    appending_generated_paths(begin, q[-1], all_paths, result)
        if not found:
            result = pathWithCost(copy.deepcopy(current_pi), path.cost, path.nRecharges, path.vehicleChargeCapacity, path.vehicleMaxCapacity)
            result.getPath().extend(p[1:])
            yield result

def load_graph(stations, connections):
    g = MyGraph()
    g.load_edges_csv(connections)
    g.load_charging_power(stations)
    return g

def visit_nodes(current_start, mst, g, do_print, odd_nodes, extendReachingPaths = None):
    while odd_nodes:
        v_nRecharge = None
        v_nCost = 0
        if (extendReachingPaths is None) or len(extendReachingPaths) == 0:
            v = odd_nodes.pop()
            v_reprise = mst.getReprise(v)
            v_nRecharge = copy.deepcopy(v_reprise.getNRecharges())
            v_nCost = v_reprise.getCost()
        else:
            v = extendReachingPaths[-1].getPath()[-1]
            v_reprise = mst.getReprise(v)
            if v in odd_nodes:
                if v == current_start:
                    odd_nodes.remove(v)
                    v_reprise = mst.getReprise(v)
                    v_nRecharge = copy.deepcopy(v_reprise.getNRecharges())
                    v_nCost = v_reprise.getCost()
                else:
                    odd_nodes.remove(v)
                    v_nRecharge = copy.deepcopy(extendReachingPaths[-1].getNRecharges())
                    v_nCost = extendReachingPaths[-1].getCost()
            else:
                return
        length = float("inf")
        closest = 0
        closest_reprise = None
        found = False
        if v_reprise is None:
            continue
        for u in odd_nodes[::-1]:
            u_reprise = max_reward_shortest_path(g, v, u, v_nRecharge, v_nCost,
                                                set(),
                                                v_reprise.getVehicleChargeCapacity(),
                                                 v_reprise.getVehicleMaxCapacity())
            if v != u and u_reprise.getCost() < length:
                length = u_reprise.getCost()
                closest = u
                closest_reprise = u_reprise
                found = True
        if found:
            if (closest_reprise is None) and (closest == mst.root):
                closest_reprise = mst.nodes[closest]
            if extendReachingPaths is None:
                mst.set_target_Ref(v, closest_reprise, do_print)
            else:
                extendReachingPaths.append(closest_reprise)

def minimum_weight_matching(mst, g, do_print=False):
    import random
    odd_nodes = list(mst.get_nodes(lambda x: (len(x) % 2) == 1))
    random.shuffle(odd_nodes)
    visit_nodes(mst.root, mst, g, do_print, odd_nodes)



class Algorithm:
    def __init__(self, graph_charging_station_power_file, graph_file, start_node=1, vehicle_initial_capacity=5, vehicle_max_capacity=5):
        self.vehicle_max_capacity = vehicle_max_capacity
        self.vehicle_initial_capacity = vehicle_initial_capacity
        self.g = load_graph(graph_charging_station_power_file, graph_file)
        self.start = start_node
        # Printing the solution
        self.visiting_queue = list()
        self.visited = set()
        self.solution = defaultdict(list)
        self.discarded = set()

    def _extract_alternatives(self, current_start, maximumPathCandidates, solution, mst):
        for node in maximumPathCandidates:
            result = list()
            reachingPath = mst.getReprise(node)
            result.append(reachingPath)
            nodes = list(self.g.get_charging_stations(lambda x: x not in set(reachingPath.getPath())))
            nodes.append(node)
            nodes.insert(0, current_start)
            visit_nodes(current_start, mst, self.g, True, nodes, result)

            for finalPath in appending_generated_paths(current_start, current_start, result, [current_start]):
                if len(finalPath.getPath()) > 1:
                    for x in finalPath.getPath():
                        self.visited.add(x)
                    solution[mst].append(finalPath)
                else:
                    self.discarded.add(current_start)

    def run(self, verbose=False):
        self.visiting_queue.clear()
        self.visiting_queue.append(self.start)
        self.solution.clear()
        self.discarded.clear()
        while len(self.visiting_queue) > 0:
            current_start = self.visiting_queue.pop()
            mst = create_spanning_tree(self.g, current_start, self.vehicle_initial_capacity, self.vehicle_max_capacity, verbose)
            maxHeight = mst.maxHeight(current_start)
            minimum_weight_matching(mst, self.g, True)
            maximumPathCandidates = list(mst.getLevel(maxHeight))
            maximumPathCandidates.sort(key=lambda x: mst.getReprise(x).getCost())
            self._extract_alternatives(current_start, maximumPathCandidates, self.solution, mst)
            for remaining in self.g.get_charging_stations(lambda x: (x not in self.visited) and (x not in self.discarded)):
                self.visiting_queue.append(remaining)

        for mst in self.solution:
            maximumPathCandidates = list()
            for level in mst.height:
                for node in mst.height[level]:
                    if not node in self.visited:
                        maximumPathCandidates.append(node)
            maximumPathCandidates.sort(key=lambda x: mst.getReprise(x).getCost())
            self._extract_alternatives(mst.root, maximumPathCandidates, self.solution, mst)

        if verbose:
            print(decode(self.solution))
        return self.solution

if __name__ == '__main__':
    a = Algorithm("/home/giacomo/PycharmProjects/pythonProject1/data/example/charging_power.csv",
              "/home/giacomo/PycharmProjects/pythonProject1/data/example/graph.txt")
    a.run(True)