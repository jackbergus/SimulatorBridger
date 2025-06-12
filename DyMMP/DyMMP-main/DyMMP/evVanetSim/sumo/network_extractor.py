import math

import sumolib
import statistics
import dataclasses
from dataclasses import dataclass

@dataclass(frozen=True, eq=True, order=True)
class OriginalEdgeInfo:
    num_lanes: int
    avg_length: float
    avg_speed: float
    avg_time: float

@dataclass(frozen=True, eq=True, order=True)
class TSPEdgeInfo:
    internal_id: int
    sumo_id: str
    src: int
    dst: int
    time: float ## to be discretized via minimum float value

def least_diff(num):
    """
    https://stackoverflow.com/a/73012481/1376095
    :param num:     List of numbers
    :return:        Minimum difference between two numbers
    """
    num.sort()
    least_diff = None
    for i in range(len(num) - 1):
        diff = abs(num[i] - num[i + 1])
        if not least_diff:
            least_diff = diff
        if diff < least_diff:
            least_diff = diff
    return least_diff

class NetworkExtractor:

    def __init__(self, file):
        self.net = sumolib.net.readNet(file)
        self.edges = list()
        self.original_edge_info = list()
        self.sumo_edge_id = list()
        self.all_times = set()
        for x in self.net.getEdges():
            self.sumo_edge_id.append(x.getID())
            avg_length = statistics.mean([y.getLength() for y in x.getLanes()])
            avg_speed = statistics.mean([y.getSpeed() for y in x.getLanes()])
            avg_time = avg_length / avg_speed
            self.all_times.add(avg_time)
            self.original_edge_info.append(OriginalEdgeInfo(len(x.getLanes()), avg_length, avg_speed, avg_time))
            self.edges.append((x.getFromNode().getID(), x.getToNode().getID(), avg_time))
        translation = dict()
        translation_inv = dict()
        min_time = least_diff(list(self.all_times))
        id = 0
        for (src,dst,length) in self.edges:
            if src not in translation:
                translation[src] = id
                translation_inv[id] = src
                id += 1
            if dst not in translation:
                translation[dst] = id
                translation_inv[id] = dst
                id += 1
        self.edges = [TSPEdgeInfo(idx, self.original_edge_info[idx], translation[src], translation[dst], time) for idx, (src,dst,time) in enumerate(self.edges)]

    def get_edges(self):
        return self.edges

if __name__ == "__main__":
    ne = NetworkExtractor("/home/giacomo/Scaricati/SUMO-Example/network.net.xml")
    ne.get_edges()