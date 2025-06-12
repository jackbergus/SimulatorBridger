import datetime
from typing import Union, Iterator, List

import numpy

from DyMMP.routing.MultiDimensionalCostGraph import MultiDimensionalCostGraph, load_from_datadump, GNNConfCases
from DyMMP.routing.MultiObjectiveCost.MultiDimensionalScore import MultidimensionalScore, \
    MultidimensionalScoresFromSoruce, prune_with_pareto_optimality
from DyMMP.routing.StaticDijkstra import path_merge
from DyMMP.routing.utils import PriorityQueue

from DyMMP.routing.ResultUtils import ResultWithCost, sum_triplet_costs


def astar_loopfree_maximum_cost_search(graph:MultiDimensionalCostGraph,
                                       start: object,
                                       start_time:Union[datetime.datetime|float],
                                       target: object = None,
                                       verbose:bool = False):
    """
    Version of the A* algorithm, where actually we consider the timestamp associated to the vertex node, so to progress with the visit,
    and where edges have also traversal costs. In the frontier, we consider vertices as represented by pairs <current_navigation_time,vertex_id>
    :param graph:
    :param start:
    :param ndim:
    :param obj_append_f:
    :param start_time:
    :return:
    """
    fun = lambda x, y: path_merge(x, y)
    ndim = graph.NDIM
    if isinstance(start_time, datetime.datetime):
        start_time = start_time.timestamp()
    frontier = PriorityQueue(True)
    frontier.put((start_time, start), MultidimensionalScore.generate_ndim_zero_array(ndim))
    # came_from: dict[object, Optional[object]] = {}
    cost_so_far: dict[object, MultidimensionalScoresFromSoruce] = {}
    # came_from[start] = None
    tmp = MultidimensionalScoresFromSoruce()
    tmp.add_cost((start,), MultidimensionalScore.generate_ndim_zero_array(ndim))
    cost_so_far[start] = tmp
    openSet = {start}
    dim_time = graph.timeDimension
    firstIteration = True
    # alradyVisited = set()
    while not frontier.empty():
        current_visit_time, current = frontier.get()
        if verbose:
            print(f"Visiting: {current} at time {current_visit_time}")
        # if current in alradyVisited:
        #     sortd_list = list(sorted(frontier.elements))
        #     test = sortd_list[1][0] > sortd_list[0][0]
        #     print(f"Already visited: {current}")
        # alradyVisited.add(current)
        if target is not None and target == current:
            return cost_so_far[target]
        curr_timestep = datetime.datetime.fromtimestamp(current_visit_time)
        openSet.remove(current)
        old_cost_so_far = cost_so_far[current]
        for next_edge in graph.neighbors(current):
            target_node = graph.getTargetNode(next_edge)
            if target_node == current:
                continue
            if (firstIteration) or (not old_cost_so_far.do_objects_contain(target_node)):
                edge_cost = graph.cost(current, next_edge, curr_timestep)
                delta_time = edge_cost.array[dim_time]
                final_time = current_visit_time + delta_time
                new_cost = cost_so_far[current].append_with_extension((next_edge, edge_cost, final_time, target_node), edge_cost, fun)
                if target_node not in cost_so_far:
                    cost_so_far[target_node] = new_cost
                    priority = new_cost.pick_lex_dimension()
                    priority = priority.incrementWith(dim_time, current_visit_time)
                    if target is not None:
                        priority = priority + graph.heuristic_distance(target_node, target, curr_timestep)
                    frontier.put((final_time, target_node), priority)
                    openSet.add(target_node)
                else:
                    old_cost = cost_so_far[target_node]
                    neu_costs, doUpdate = prune_with_pareto_optimality(old_cost, new_cost)
                    if doUpdate:
                        cost_so_far[target_node] = neu_costs
                        if (not target_node in openSet) and neu_costs != old_cost:
                            priority = neu_costs.pick_lex_dimension()
                            priority = priority.incrementWith(dim_time, current_visit_time)
                            if target is not None:
                                priority = priority + graph.heuristic_distance(target_node, target, curr_timestep)
                            frontier.put((current_visit_time+delta_time, target_node), priority)
                            openSet.add(target_node)
                    # came_from[next] = current
            # else:
            # print(f"{new_cost.objects} contains {target_node}")
        firstIteration = False
    return cost_so_far if target is None else None

def astar_loopfree_structured_maximum_cost_result(graph,
                                                 start,
                                                 start_time,
                                                 target,
                                                 forSumo=False,
                                                  pickMinimumPath=True) -> List[ResultWithCost]:
    """This function provides some structured information from the path.
       Please observe this refers to the local graph, and not to the
       SUMO network (forSumo = False). Otherwise, these provide all the information for the
       node and edge information for the graph. """
    from DyMMP.routing.ResultUtils import split_edge_information
    from operator import itemgetter
    src_x_path = astar_loopfree_maximum_cost_search(graph, start, start_time, target)
    if src_x_path is not None:
        N = min(len(src_x_path.costs), len(src_x_path.objects))
        if pickMinimumPath and N>1:
            index, total_cost = max(enumerate(src_x_path.costs), key=itemgetter(1))
            final_path = tuple(split_edge_information(src_x_path.objects[index]))
            result = ResultWithCost(total_cost, final_path)
            if not forSumo:
                return [result]
            else:
                return [result.forSumo(graph)]
        else:
            result = [None] * N
            for idx, (total_cost, path) in enumerate(zip(src_x_path.costs, src_x_path.objects)):
                final_path = tuple(split_edge_information(path))
                rec = ResultWithCost(total_cost, final_path)
                if not forSumo:
                    result[idx] = rec
                else:
                    result[idx] = rec.forSumo(graph)
            return result
    return []

class MultiAgentTimedAStar:
    def __init__(self, src, target, car_model_file, data_folder, city, prediction_car_distr_type:GNNConfCases,topologyPreserve:bool, scaling:float=0.0001):
        """
        Initialization parameters for the multi-agent variant of the algorithm
        :param src:                         Source node from where all the cars will depart
        :param target:                      Target node to be reached by all cars
        :param car_model_file:              Digital Twin configuration file for the car
        :param data_folder:                 Main folder containing all the city configuration files
        :param city:                        Name of the city of interest
        :param prediction_car_distr_type:   Type of prediction car model of interest
        :param topologyPreserve:            Type of prediction car model of interest: whether the former parameter has nodes preserving the orginal topology, or whether we just consider no nodes at all
        :param scaling:                     Scaling for fitting all the nodes to a grid-size view of the map
        """
        self.target = target
        self.src = src
        self.topologyPreserve = topologyPreserve
        self.scaling = scaling
        self.prediction_car_distr_type = prediction_car_distr_type
        self.city = city
        self.data_folder = data_folder
        self.car_model_file = car_model_file
        self.result = []
        self.additional_traffic_positions: List[tuple[float, str, float]] = list()

    def __call__(self, ncars):
        """
        Calls multiple times the algorithm
        :param ncars: Number of cars running the same shortest-path algorithm
        :return:      Returning each single path computed per car
        """
        if ncars <= 0:
            return self.result
        print(f"Running for car #{ncars}")
        g = load_from_datadump(self.car_model_file,
                              self.data_folder,
                               self.city,
                               self.prediction_car_distr_type,
                               self.additional_traffic_positions,
                               self.scaling,
                               self.topologyPreserve)
        src_x_path = astar_loopfree_structured_maximum_cost_result(g, self.src, g.alpha, self.target, True, True)
        assert len(src_x_path) == 1
        self.result.append(src_x_path[0])
        result = src_x_path[0].as_list_of_triplets(g.alpha.timestamp())
        if len(self.additional_traffic_positions) == 0:
            self.additional_traffic_positions = list(result)
        else:
            self.additional_traffic_positions = list(sum_triplet_costs(self.additional_traffic_positions, result))
        return self(ncars-1)

