import dataclasses
import datetime
import os.path
from collections import defaultdict
from enum import Enum
from typing import List

import networkx
import numpy

import sys
sys.path.append("C:\\Users\\rohin\\DyMMP\\DyMMP-main")

from DyMMP.TimedDistributions.TimedDistribution import TimedRiskDistribution
from DyMMP.dataintergration.DataIntegration import latlon_to_meters
from DyMMP.evVanetSim.EVBattery.EVMatlab.conf import VehicleEntryPoint
from DyMMP.routing.MultiObjectiveCost.MultiDimensionalScore import MultidimensionalScore


@dataclasses.dataclass
class Dimensions:
    QoS: float              ## Number of cars-dependant
    BatteryCost: float      ## Cost for traversing the edge
    RiskCost: float         ## Desirability, from probability of risk, expressed as a cost via -log(p)
    TraversalTime: float    ## The time it takes to traverse the edge

class MultiDimensionalCostGraph:
    def __init__(self, veh: VehicleEntryPoint, NDIM, time_dimension, aggregation='max', scaled=False):
        self._time_dimension = time_dimension
        self.veh = veh
        self.scaled = scaled
        self.aggregation = aggregation
        self.adjacency_list = dict()
        self.inverse_adjacency_list = dict()
        self.NDIM = NDIM
        self.nedges = 0
        self.edge_coord_to_info = list()
        self.compact_adj_list = defaultdict(list)
        self.compact_inv_adj_list = defaultdict(list)
        self.edge_data= list()
        self.zero = MultidimensionalScore.generate_ndim_zero_array(self.NDIM)
        self.inf = MultidimensionalScore.generate_ndim_max_array(self.NDIM)
        self.other_dimensions = dict()
        self.alpha = None
        self.omega = None
        self.vertices_data = dict()

    @property
    def timeDimension(self):
        return self._time_dimension

    def nodes(self):
        return self.adjacency_list.keys()

    def add_edge(self, src, dst, multi_cost, data=None):
        assert len(multi_cost) == self.NDIM
        if dst not in self.inverse_adjacency_list:
            self.inverse_adjacency_list[dst] = dict()
        if src not in self.adjacency_list:
            self.adjacency_list[src] = dict()
        if dst not in self.adjacency_list[src]:
            self.adjacency_list[src][dst] = list()
        if src not in self.inverse_adjacency_list[dst]:
            self.inverse_adjacency_list[dst][src] = list()
        cost = MultidimensionalScore(multi_cost)
        self.adjacency_list[src][dst].append(cost)
        self.inverse_adjacency_list[dst][src].append(cost)
        self.edge_coord_to_info.append((src, dst, len(self.adjacency_list[src][dst])-1))
        N = len(self.edge_coord_to_info)
        self.compact_adj_list[src].append(N-1)
        self.compact_inv_adj_list[dst].append(N-1)
        self.nedges += 1
        self.edge_data.append(data)

    def edges(self, src, out:bool=True):
        L = list()
        for target, ls in (self.adjacency_list[src] if out else self.inverse_adjacency_list[src]).items():
            for x in ls:
                L.append((target, x))
        return L

    def neighbors(self, node_id):
        return self.compact_adj_list[node_id]

    def neighbors_at_step(self, node_id, step=1, verbose=False):
        if step <= 0:
            return {node_id}
        else:
            result = set()
            for dst in self.adjacency_list.get(node_id, dict()).keys():
                if verbose:
                    print(f"{node_id}->{dst}")
                result.update(self.neighbors_at_step(dst, step-1, verbose))
            return result


    def getTargetNode(self, edgeId):
        if edgeId >= self.nedges:
            return None
        return self.edge_coord_to_info[edgeId][1]

    def heuristic_cost(self, node, targetNode, time=None):
        if self.heuristic is not None:
            return self.heuristic(self, node, targetNode, time)
        else:
            return self.zero

    def getOutgoingEdgesIDs(self, node):
        return self.compact_adj_list.get(node, list())

    def getEdgeInformation(self, edgeId):
        src, dst, offset = self.edge_coord_to_info[edgeId]
        return src, dst, self.adjacency_list[src][dst][offset]

    def heuristic_distance(self, node, goal, time=None)->MultidimensionalScore:
        ## TODO: GENERALIZE
        assert time >= self.alpha
        assert time <= self.omega
        if node == goal:
            return MultidimensionalScore.generate_ndim_zero_array(self.NDIM)
        for_heuristic_qos:TimedRiskDistribution = self.other_dimensions["hQoS"]
        lat1, lon1 = self.vertices_data[node]["lat"], self.vertices_data[node]["lon"]
        lat2, lon2 = self.vertices_data[goal]["lat"], self.vertices_data[goal]["lon"]
        distance_heur = latlon_to_meters(lat1, lon1, lat2, lon2)
        from DyMMP.evVanetSim.EVBattery.UnitsOfMeasure import Distance, VelocityUnit, Velocity
        distance = Distance(distance_heur)
        velocity = Velocity(50, VelocityUnit.m_per_s)
        battery_cons_heur, time_travel = self.veh.estimate_road_consumption(distance, velocity, 0.0001)
        time_heur = time_travel.seconds
        heurQoS = for_heuristic_qos.get_distribution_in_line(lat1, lon1, lat2, lon2, time, datetime.timedelta(seconds=time_heur), self.aggregation, self.scaled)
        return MultidimensionalScore(numpy.array([heurQoS, battery_cons_heur, 0.0, time_heur, distance_heur]))

    def add_cars_to_counting(self, edgeId, time, value):
        """TODO: we do not only consider cars being added in time, but also the number of cars changing with time"""
        src, dst, offset = self.edge_coord_to_info[edgeId]
        c: MultidimensionalScore = self.adjacency_list[src][dst][offset]
        final_time = time + datetime.timedelta(seconds=c[3])
        lat1, lon1 = self.vertices_data[src]["lat"], self.vertices_data[src]["lon"]
        lat2, lon2 = self.vertices_data[dst]["lat"], self.vertices_data[dst]["lon"]
        expected_qos: TimedRiskDistribution = self.other_dimensions["QoS"]
        h_expected_qos: TimedRiskDistribution = self.other_dimensions["hQoS"]
        from DyMMP.TimedDistributions.RiskDistribution import CostUpdateStrategy
        expected_qos.update_cost(lat1, lon1, time, value, CostUpdateStrategy.SumToPreviousCost)
        expected_qos.update_cost(lat2, lon2, final_time, value, CostUpdateStrategy.SumToPreviousCost)
        h_expected_qos.update_cost(lat1, lon1, time, value, CostUpdateStrategy.SumToPreviousCost)
        h_expected_qos.update_cost(lat2, lon2, final_time, value, CostUpdateStrategy.SumToPreviousCost)

    def cost(self, node, edgeId, time=None)->MultidimensionalScore:
        ## TODO: GENERALIZE
        assert time >= self.alpha
        assert time <= self.omega
        assert edgeId in self.compact_adj_list[node]
        src, dst, offset = self.edge_coord_to_info[edgeId]
        assert src == node
        allCost = self.adjacency_list[src][dst][offset].list()
        expected_qos:TimedRiskDistribution = self.other_dimensions["QoS"]
        lat1, lon1 = self.vertices_data[src]["lat"], self.vertices_data[src]["lon"]
        lat2, lon2 = self.vertices_data[dst]["lat"], self.vertices_data[dst]["lon"]
        duration_as_seconds = datetime.timedelta(seconds=allCost[3])
        allCost[0] = expected_qos.get_distribution_in_line(lat1, lon1, lat2, lon2, time, duration_as_seconds, self.aggregation, self.scaled)
        return MultidimensionalScore(numpy.array(allCost))

    @staticmethod
    def load_from_planner_graph(filename, vehicle)->tuple['MultiDimensionalCostGraph', networkx.MultiDiGraph]:
        car_configuration = VehicleEntryPoint(vehicle)
        final_g = MultiDimensionalCostGraph(car_configuration, 5, 3)
        g = networkx.read_gexf(filename)
        final_g.vertices_data = dict()
        local_graph_id_to_sumo_name = dict()
        for local_id, dd in dict(g.nodes.data()).items():
            local_graph_id_to_sumo_name[local_id] = dd["name"]
            final_g.vertices_data[dd["name"]] = dd
        for (src, dst, data) in g.edges.data():
            src = local_graph_id_to_sumo_name[src]
            dst = local_graph_id_to_sumo_name[dst]
            sumo_id = data["sumo_id"]  # Data required for reconstructing the Sumo edge, if required
            grade = data["relu_grade"]  # Data information for the edge:
            haul_time = data["time_s"]
            battery_cons = data["battery_cons"]
            distance = data["distance_m"]
            desirability = data['desirability']
            cost = (None, battery_cons, desirability, haul_time, distance)
            final_g.add_edge(src, dst, cost, data)
        return final_g, g



class GNNConfCases(Enum):
    HOP = "hop"
    MINLEN = "minlen"
    MINTIME = "mintime"

def load_from_datadump(vehicle_file,
                       base_folder,
                       dataset,
                       case: GNNConfCases,
                       additional_traffic_positions : List[tuple[float, str, float]]=None,
                       scaling:float=0.0001,
                       topologyPreserve:bool=False):
    ## Loading the basic graph, containing costs not varying in time
    graph_file = os.path.join(base_folder, dataset, "planner_graph.gexf")
    g, old_g = MultiDimensionalCostGraph.load_from_planner_graph(graph_file, vehicle_file)

    ## Loading some other conversion information from the graph, mainly the junction information
    from DyMMP.TimedDistributions.TimedDistribution import load_junction_position_map_from_gexf_file
    from DyMMP.TimedDistributions.TimedDistribution import TimedRiskDistribution
    from DyMMP.TimedDistributions.TimedDistribution import estimation_error

    junction_id_to_position, junction_name_to_id, old_g = load_junction_position_map_from_gexf_file(old_g)

    top = "_top_preserve_" if topologyPreserve else "_"
    folder = f"gnn_network_{case.value}{top}predictions"
    graph_file = os.path.join(base_folder, dataset, "gnn_traffic", folder)
    assert os.path.exists(graph_file) and os.path.isdir(graph_file), f"{graph_file} does not exist"
    all_original = os.path.join(graph_file, "all_original.csv")
    all_predictions = os.path.join(graph_file, "all_predictions.csv")

    final_result_id = None
    if additional_traffic_positions is not None:
        final_result_id = [None] * len(additional_traffic_positions)
        for idx, x in enumerate(additional_traffic_positions):
            assert x[1] in junction_name_to_id
            final_result_id[idx] = (x[0], junction_name_to_id[x[1]], x[2])

    traffic_predictions, lower_bound_heuristic = estimation_error(all_original, all_predictions)
    expected_qos = TimedRiskDistribution.from_gnn_prediction_csv(junction_id_to_position,
                                                  scaling, filter, None, traffic_predictions, final_result_id)
    for_heuristic_qos = TimedRiskDistribution.from_gnn_prediction_csv(junction_id_to_position,
                                                  scaling, filter, None, lower_bound_heuristic, final_result_id)
    assert expected_qos.alpha == for_heuristic_qos.alpha
    assert expected_qos.omega == for_heuristic_qos.omega
    g.alpha = expected_qos.alpha
    g.omega = expected_qos.omega
    g.other_dimensions["QoS"] = expected_qos
    g.other_dimensions["hQoS"] = for_heuristic_qos
    return g




#if __name__ == "__main__":
def output(vehNum, imported_city, src, x):
    car_model = "C:\\Users\\rohin\\SimulatorBridger\\SimulatorBridger\\DyMMP\\ChevyVolt.yaml"
    data_folder = "C:\\Users\\rohin\\SimulatorBridger\\SimulatorBridger\\DyMMP\\"
    city = imported_city
    prediction_car_distr_type = GNNConfCases.MINTIME
    topologyPreserve = True
    scaling = 0.0001
    g = load_from_datadump(car_model,
                           data_folder,
                           city,
                           prediction_car_distr_type)
    h = 10
    print("Loading g: done")
    #src = list(g.nodes())[0]
    edge = g.getOutgoingEdgesIDs(src)[0]
    original_data = g.getEdgeInformation(edge)
    result = g.cost(src, edge, g.alpha)
    S = g.neighbors_at_step(src, h, True)
    print(f"{h} nearest nodes: done: {S}")
    #for x in S:
    print(f"Timed A* from {src} to {x}")
    from DyMMP.routing.TimedAStar import MultiAgentTimedAStar
    Algorithm = MultiAgentTimedAStar(src, x, car_model, data_folder, city,
                prediction_car_distr_type, topologyPreserve, scaling)
    result = Algorithm(vehNum)
    return result
        # print(result)
        # exit(1)
        # from DyMMP.routing.TimedAStar import astar_loopfree_structured_maximum_cost_result
        # src_x_path = astar_loopfree_structured_maximum_cost_result(g, src, g.alpha, x, True, True)
        # list(src_x_path)