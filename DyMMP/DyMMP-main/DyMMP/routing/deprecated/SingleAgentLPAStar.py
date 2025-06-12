from DyMMP.routing.MultiDimensionalCostGraph import MultiDimensionalCostGraph
from DyMMP.routing.utils import PriorityQueue


class SingleAgentLPAStar:
    def __init__(self, g:MultiDimensionalCostGraph, start:object, end:object, forward:bool=True):
        self.graph = g
        self.U = PriorityQueue()
        self.MAX_COST = g.inf
        self.ZERO = g.zero
        self._rhs = dict()
        self._g = dict()
        self._rhs[start] = self.ZERO
        self.source = start
        self.destination = end
        self.U.insert(start, (self.graph.heuristic_cost(self.source, self.destination), self.ZERO))

    def _calculate_key(self, s):
        right_component = min(self.g(s), self.rhs(s))
        return (right_component+self.graph.heuristic_cost(s, self.destination), right_component)

    def _update_vertex(self, s):
        if self.g(s) != self.rhs(s):
            if s in self.U:
                self.U.update(s, self._calculate_key(s))
            else:
                self.U.insert(s, self._calculate_key(s))
        elif s in self.U:
            self.U.remove(s)

    def compute_shortest_paths(self, isDirectionOut=True):
        maxmax = (self.MAX_COST, self.MAX_COST)
        while (self.U.top_key(maxmax) < self._calculate_key(self.destination)) or (self.rhs(self.destination) != self.g(self.destination)):
            u = self.U.get()
            rhs_u = self.rhs(u)
            g_u = self.g(u)
            if (g_u > rhs_u):
                self._g[u] = rhs_u
                self.U.remove(u)
                for node, edge_cost in self.graph.edges(u, isDirectionOut):
                    if (node  != self.source):
                        # final_costs = [self.rhs(node), self.g(u)+edge_cost]
                        # for predNode, predEdgeCost in self.graph.edges(node, not isDirectionOut):
                        #     l.append(self.g(predNode) + predEdgeCost)
                        # mask = is_pareto_efficient(l, True)
                        # final_costs = list(itertools.compress(l, mask))
                        self._rhs[node] = min(self.rhs(node), self.g(u)+edge_cost)
                        self._update_vertex(node)
            else:
                g_old = self.g(u)
                self._g.pop(u)
                l = list()
                for predNode, predEdgeCost in self.graph.edges(node, not isDirectionOut):
                    l.append(self.g(predNode) + predEdgeCost)
                l.append()


    def rhs(self, x):
        return self._rhs.get(x, self.MAX_COST)

    def g(self, x):
        return self._g.get(x, self.MAX_COST)