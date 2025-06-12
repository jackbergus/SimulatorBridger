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
import collections
import copy
from collections import defaultdict
import heapq

from DyMMP.tsp_loop import pathWithCost

class MST(object):
    def __init__(self, root, mst=None, nodes=None):
        if mst is None:
            mst = defaultdict(set)
        if nodes is None:
            nodes = dict()
        self.root = root
        self.mst = mst
        self.nodes = nodes
        self.height = defaultdict(set)

    def __str__(self):
        from DyMMP.tsp_loop.Serializable import StringBuilder
        sb = StringBuilder()
        sb.Append("tree with root ")
        sb.Append(str(self.root))
        return sb.__str__()

    def getRoot(self):
        return self.root

    def maxHeight(self, start, count=0):
        self.height[count].add(start)
        val = count
        for x in self.mst[start]:
            tmp = self.maxHeight(x.getPath()[-1], count+1)
            if tmp > val:
                val = tmp
        if (count == 0):
            self.maxHeight = collections.OrderedDict(reversed(list(self.height.items())))
        return val

    def getLevel(self, level):
        if level in self.height:
            return self.height[level]
        else:
            return list()

    def set_init_node(self, src, src_info):
        self.nodes[src] = src_info

    def set_target_Ref(self, src, target_info, do_print=False):
        target = target_info.getPath()[-1]
        self.nodes[target] = target_info
        if not target_info in self.mst[src]:
            self.mst[src].add(target_info)
            if do_print:
                print(str(src)+"-->"+str(target))

    def out_size(self, x):
        if x not in self.mst:
            return 0
        else:
            return self.mst[x]

    def get_outgoing(self, x):
        if x not in self.mst:
            return dict()
        else:
            return self.mst[x]

    def get_outgoing_id(self,x):
        if x not in self.mst:
            return list()
        else:
            return map(lambda x : x.getPath()[-1], self.mst[x])

    def collect_paths(self, curr, coll):
        for out in self.get_outgoing(curr):
            p = tuple(out.getPath())
            next = out.getPath()[-1]
            coll.add(p)
            self.collect_paths(next, coll)

    def getReprise(self, x):
        if x in self.nodes:
            return self.nodes[x]
        else:
            return None
    def get_nodes(self, pred=lambda x: True):
        for x in self.mst:
            if pred(self.mst[x]):
                yield x


_end = '_end_'
def make_trie(collected, mst, do_print=False):
    final = MST(mst.root)
    final.set_init_node(mst.root, mst.getReprise(mst.root))
    for this in collected:
        start = this[0]
        nextt = None
        for i in range(1,len(this)):
            if this[i] in mst.nodes:
                nextt = this[i]
                final.set_target_Ref(start, mst.getReprise(nextt), do_print)
                start = nextt
    return final

def restructure_trie(trie, this, g, c, vmc):
    if trie.out_size(this) == 0:
        cp = copy.deepcopy(trie.getReprise(this))
        path = cp.getPath()
        path.pop()
        cp.setPath(path)
        return cp
    else:
        curr = trie.getReprise(this)
        path = curr.getPath()
        cn = curr.getNRecharges()
        for out in trie.get_outgoing_id(this):
            tmp = restructure_trie(trie, out, g, c, vmc)
            if all(map(lambda x: x[0] < x[1],zip([(k, v) for k, v in tmp.getNRecharges().items()], [(k, v) for k, v in cn.items()]))):
                cn = tmp.getNRecharges()
                curr.setNRecharges(cn)
        total, onlyCost = g.estimatePathCharge(curr.getNRecharges(), path, c, vmc)
        curr.setVehicleChargeCapacity(total)
        curr.setCost(onlyCost)
        curr.setPath(path)
        trie.nodes[this] = curr
        curr = copy.deepcopy(curr)
        ls = copy.deepcopy(curr.getPath())
        if len(ls) > 0:
            ls.pop()
        curr.setPath(ls)
        return curr


def create_spanning_tree(graph, starting_vertex, vehicleChargeCapacity=0, vehicleMaxCapacity=0, do_print=False):
    """
    This algorithm implements Prim's Spanning Trees by exploiting the minimum shortest path function for getting the
    distance between charging stations

    :param graph:                       Traffic networks, where charging stations and intermediate edges are given
    :param starting_vertex:             Vertex from which we want start the visit (e.g., the vehicle's location)
    :param vehicleChargeCapacity:       Current charge capacity of the vehicle
    :param vehicleMaxCapacity:          Maximum vehicle's charge when fully charged
    :return:                            The associated minimum spanning tree
    """
    from DyMMP.tsp_loop import max_reward_shortest_path
    from DyMMP.tsp_loop.mrsp import INF
    # nodes = set(graph.get_charging_stations())
    mst = MST(starting_vertex)
    mst.set_init_node(starting_vertex,
                                              pathWithCost([starting_vertex], 0, graph.initNRecharges(),
                                                           vehicleChargeCapacity, vehicleMaxCapacity))
    visited = {starting_vertex}

    edges = []
    for node in graph.get_charging_stations():
        if not (node == starting_vertex):
            reprise = max_reward_shortest_path(graph, starting_vertex, node, graph.initNRecharges(), 0, set(), vehicleChargeCapacity,
                                               vehicleMaxCapacity)
            cost = reprise.getCost()
            if (cost < INF):
                edges.append(tuple([cost, starting_vertex, node, reprise]))
    heapq.heapify(edges)

    while edges:
        cost, frm, to, reprise = heapq.heappop(edges)
        if to not in visited:
            visited.add(to)
            mst.set_target_Ref(frm, reprise)
            for to_next in graph.get_charging_stations():
                if to_next not in visited:
                    reprise2 = max_reward_shortest_path(graph, to, to_next, reprise.getNRecharges(),
                                                        reprise.getCost(),
                                                        set(),
                                                        reprise.getVehicleChargeCapacity(),
                                                        reprise.getVehicleMaxCapacity())
                    cost2 = reprise2.getCost()
                    if (cost2 < INF):
                        edges.append(tuple([cost2, to, to_next, reprise2]))

    s = set()
    mst.collect_paths(starting_vertex, s)
    trie = make_trie(s, mst, do_print)
    restructure_trie(trie, starting_vertex, graph, vehicleChargeCapacity, vehicleMaxCapacity)
    return trie

