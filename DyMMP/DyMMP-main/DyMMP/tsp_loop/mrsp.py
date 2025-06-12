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

INF = float("inf")

def max_reward_shortest_path(graph, start, end, nRecharges, pathCost, visited, vehicleChargeCapacity, vehicleMaxCapacity):
    # Default path if no solution was found
    from DyMMP.tsp_loop import pathWithCost
    cp = pathWithCost(list(), INF, nRecharges, vehicleChargeCapacity, vehicleMaxCapacity)
    if (start == end) and (vehicleChargeCapacity > 0):
        # If we reached destination with a vehicle which is still fully charged
        cp.appendToPath(start)
        cp.setCost(pathCost)
        return cp
    elif len(graph.out_edges(start)) == 0 or vehicleChargeCapacity <= 0 or (start in visited):
        # Otherwise, if either the current node is a deadlock or the vehicle is
        # out of battery, then no more viable path can be pursued
        return cp
    # If all the other test are passed, I am attempting at generating a novel solution
    cp.appendToPath(start)
    visited.add(start)
    # Still, at this stage, no further path was found, and so the minimum path has infinite cost
    shortest = pathWithCost(list(), INF, nRecharges, vehicleChargeCapacity, vehicleMaxCapacity)
    if not graph.is_charging_station(start):
        # If the current node start is not a charging station, then do the Dijkstra algorithm as usual
        for node in graph.out_edges(start):
            if (not node in visited):
                cost = graph.weight(start, node)
                newpath = max_reward_shortest_path(graph,
                                             node,
                                             end,
                                             copy.deepcopy(nRecharges),
                                             pathCost + cost,
                                             copy.deepcopy(visited),
                                             vehicleChargeCapacity - cost,
                                             vehicleMaxCapacity)
                npC = newpath.getCost()
                sC = shortest.getCost()
                nT = graph.countTotalPowerUsedInRecharge(newpath.getNRecharges())
                sT = graph.countTotalPowerUsedInRecharge(shortest.getNRecharges())
                # We are preferring the novel solution to the previously computed one if and only if the former
                # minimizes the overall cost with the recharge
                if ((npC < sC) or (npC == sC and nT < sT)) and (len(newpath.getPath()) > 0):
                    shortest = newpath
        # At the end, we can only extend the current path with the one that was computed before
        cp.extendWith(shortest)
        return cp
    else:
        # Otherwise, we are in a charging station, and we need to try the minimum amount of recharge required to visit
        # the path
        while True:
            # Computing the maximum recharge at this current iteration, in nRecharges[start]
            maxRecharge = nRecharges[start] * graph.get_charge_per_time_unit(start)
            # Then, running Dijkstra as usual, where now the currentCapacity considers the maxRecharge available at this
            # iteration
            for node in graph.out_edges(start):
                if not node in cp.getPath():
                    cost = graph.weight(start, node)
                    capacityAfterRecharge = min(vehicleMaxCapacity, vehicleChargeCapacity + maxRecharge)
                    actualRecharge = capacityAfterRecharge - vehicleChargeCapacity
                    currentCapacity = capacityAfterRecharge - cost
                    newpath = max_reward_shortest_path(graph,
                                                 node,
                                                 end,
                                                 copy.deepcopy(nRecharges),
                                                 pathCost + cost,
                                                 copy.deepcopy(visited),
                                                 currentCapacity,
                                                 vehicleMaxCapacity)
                    npC = newpath.getCost()
                    sC = shortest.getCost()
                    nT = graph.countTotalPowerUsedInRecharge(newpath.getNRecharges())
                    sT = graph.countTotalPowerUsedInRecharge(shortest.getNRecharges())
                    if ((npC < sC) or (npC == sC and nT < sT)) and (len(newpath.getPath()) > 0):
                        shortest = newpath
            if (shortest.getCost() < INF) and (len(shortest.getPath()) > 0):
                # A solution was found if the navigation cost is not INF and the length of the path is not-zero.
                # By previous definitions, this entails that
                cp.extendWith(shortest)
                return cp
            if min(vehicleChargeCapacity + maxRecharge, vehicleMaxCapacity) < vehicleMaxCapacity:
                # A solution was not found yet, but the recharge on the current charging station
                # has not reached yet the maximum car recharge
                nRecharges[start] = nRecharges[start] + 1
                shortest.setPath(list())
                shortest.setCost(INF)
            else:
                # Otherwise, we reached the maximum charge and, nevertheless, no viable path was found.
                # Returning the impossible path
                return pathWithCost(list(), INF, nRecharges)