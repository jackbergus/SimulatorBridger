import itertools
from collections import defaultdict

from DyMMP.routing.MultiObjectiveCost.MultiDimensionalScore import MultidimensionalScore, MultidimensionalScoresFromSoruce, \
    prune_with_pareto_optimality
from DyMMP.routing.utils import PriorityQueue


# def heuristic(a: GridLocation, b: GridLocation) -> float:
#     (x1, y1) = a
#     (x2, y2) = b
#     return abs(x1 - x2) + abs(y1 - y2)


# Faster than is_pareto_efficient_simple, but less readable.

def dijkstra_loopfree_maximum_cost_search(graph, start: object, ndim, obj_append_f):
    frontier = PriorityQueue()
    frontier.put(start, MultidimensionalScore.generate_ndim_zero_array(ndim))
    # came_from: dict[object, Optional[object]] = {}
    cost_so_far: dict[object, MultidimensionalScoresFromSoruce] = {}
    # came_from[start] = None
    tmp = MultidimensionalScoresFromSoruce()
    tmp.add_cost(start, MultidimensionalScore.generate_ndim_zero_array(ndim))
    cost_so_far[start] = tmp
    in_frontier = {start}
    while not frontier.empty():
        current = frontier.get()
        in_frontier.remove(current)
        for next_edge in graph.neighbors(current):
            target_node = graph.getTargetNode(next_edge)
            edge_cost = graph.cost(current, next_edge)
            new_cost = cost_so_far[current].append_with_extension((next_edge, target_node), edge_cost, obj_append_f)
            if target_node not in cost_so_far:
                cost_so_far[target_node] = new_cost
                priority = new_cost.pick_lex_dimension()
                frontier.put(target_node, priority)
                in_frontier.add(target_node)
            else:
                old_cost = cost_so_far[target_node]
                neu_costs, doUpdate = prune_with_pareto_optimality(old_cost, new_cost)
                if doUpdate:
                    cost_so_far[target_node] = neu_costs
                    if (not target_node in in_frontier) and neu_costs != old_cost:
                        priority = neu_costs.pick_lex_dimension()
                        frontier.put(target_node, priority)
                        in_frontier.add(target_node)
                # came_from[next] = current
    return cost_so_far

class StaticGraph:
    def __init__(self, NDIM):
        self.adjacency_list = dict()
        self.NDIM = NDIM
        self.nedges = 0
        self.edge_coord_to_info = list()
        self.compact_adj_list = defaultdict(list)

    def add_edge(self, src, dst, multi_cost):
        assert len(multi_cost) == self.NDIM
        if src not in self.adjacency_list:
            self.adjacency_list[src] = dict()
        if dst not in self.adjacency_list[src]:
            self.adjacency_list[src][dst] = list()
        self.adjacency_list[src][dst].append(MultidimensionalScore(multi_cost))
        self.edge_coord_to_info.append((src, dst, len(self.adjacency_list[src][dst])-1))
        self.compact_adj_list[src].append(len(self.edge_coord_to_info)-1)
        self.nedges += 1

    def neighbors(self, node_id):
        return self.compact_adj_list[node_id]

    def getTargetNode(self, edgeId):
        if edgeId >= self.nedges:
            return None
        return self.edge_coord_to_info[edgeId][1]

    def cost(self, node, edgeId, time=None):
        assert edgeId in self.compact_adj_list[node]
        src, dst, offset = self.edge_coord_to_info[edgeId]
        assert src == node
        return self.adjacency_list[src][dst][offset]

def path_merge(x,y):
    return list(itertools.chain(x,y))

if __name__ == "__main__":
    sg = StaticGraph(3)
    sg.add_edge("0", "1", (4,5,6))
    sg.add_edge("0", "2", (1,2,3))
    sg.add_edge("0", "3", (0,0,0))
    sg.add_edge("1", "3", (0,0,0))
    sg.add_edge("2", "3", (3,1,7))
    sg.add_edge("3", "0", (1,1,1))
    dijkstra_loopfree_maximum_cost_search(sg, "0", 3, lambda x, y: path_merge(x, y))
