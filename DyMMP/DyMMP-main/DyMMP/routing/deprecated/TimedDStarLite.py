from DyMMP.routing.MultiObjectiveCost.MultiDimensionalScore import MultidimensionalScore
import heapq

## Osserva: con un grafo temporale, devo anche conoscere preventivamente il tempo di arrivo.
## Quindi, l'unico modo che posso fare è iniziare la visita dall'inizio, con lo stesso algoritmo, e poi ottenere il tempo di arrivo, per poi andare a ritroso

class DStarLite:
    def __init__(self, graph, start, goal, MAX=None, ZERO=None, DIM=None):
        self.graph = graph
        self.DIM = DIM
        self._g = dict()
        self._rhs = dict()
        self.max_vector = MultidimensionalScore.generate_ndim_max_array(DIM) if MAX is None else MAX
        self.zero_vector = MultidimensionalScore.generate_ndim_zero_array(DIM) if ZERO is None else ZERO
        self.k_m = self.zero_vector
        self._rhs[goal] = self.zero_vector
        self.goal = goal
        self.start = start
        self.queue = []
        heapq.heappush(self.queue, self.calculateKey(goal, start) + (self.goal,))

    def topKey(self)->tuple[MultidimensionalScore,MultidimensionalScore]:
        self.queue.sort()
        if len(self.queue) > 0:
            return self.queue[0][:2]
        else:
            return (self.max_vector, self.max_vector)

    def g(self, node)->MultidimensionalScore:
        return self._g.get(node, self.max_vector)

    def rhs(self, node)->MultidimensionalScore:
        return self._rhs.get(node, self.max_vector)

    def calculateKey(self, id, s_current)->tuple[MultidimensionalScore,MultidimensionalScore]:
        min_shared = min(self.g(id), self.rhs(id))
        return (min_shared + heuristic_from_s(graph, id, s_current) + self.k_m,
                min_shared)

    def computeShortestPath(self, s_start):
        while (self.rhs(s_start) != self.g(s_start)) or (
                self.topKey() < self.calculateKey(s_start, s_start)):
            k_old = self.topKey()
            u = heapq.heappop(self.queue)[2]
            if k_old < self.calculateKey(u, s_start):
                heapq.heappush(self.queue, self.calculateKey(u, s_start) + (u,))
            elif self.g(u) > self.rhs(u):
                self._g[u] = self.rhs(u)
                for i in graph.graph[u].parents:
                    self.updateVertex(i, s_start)
            else:
                self._g[u] = self.max_vector
                self.updateVertex(u, s_start)
                for i in graph.graph[u].parents:
                    self.updateVertex(i, s_start)
            # graph.printGValues()

    def updateVertex(self, id, s_current):
        s_goal = self.goal
        if id != s_goal:
            min_rhs = self.max_vector
            for i in graph.graph[id].children:
                min_rhs = min(min_rhs, self.g(i) + graph.graph[id].children[i])
            self._rhs[id] = min_rhs
        id_in_queue = [item for item in self.queue if id in item]
        if id_in_queue != []:
            if len(id_in_queue) != 1:
                raise ValueError('more than one ' + id + ' in the queue!')
            self.queue.remove(id_in_queue[0])
        if self.rhs(id) != self.g(id):
            heapq.heappush(self.queue, self.calculateKey(id, s_current) + (id,))
