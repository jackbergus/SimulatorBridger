import dataclasses
import itertools
from dataclasses import dataclass
from typing import Iterator

from DyMMP.routing.MultiObjectiveCost.MultiDimensionalScore import MultidimensionalScore


@dataclass(eq=True, frozen=True)
class EdgeFromPath:
    src_node: int|str
    edge_id_for_src: int|str
    edge_cost_in_time: MultidimensionalScore
    final_time: float
    target_node: int|str

    def forSumo(self, graph):
        src_node = graph.vertices_data[self.src_node]['name']
        target_node = graph.vertices_data[self.src_node]['name']
        edge_id_for_src = graph.edge_data[self.edge_id_for_src]['sumo_id']
        return EdgeFromPath(src_node, edge_id_for_src, self.edge_cost_in_time, self.final_time, target_node)

    def as_list_of_triplets(self, src_time):
        return [(src_time, self.src_node, 1.0), (self.final_time, self.target_node, 1.0)]

    def as_first_triplet(self, src_time):
        return (src_time, self.src_node, 1.0)

    def as_triplet(self):
        return (self.final_time, self.target_node, 1.0)




@dataclass(eq=True, frozen=True)
class ResultWithCost:
    total_cost: MultidimensionalScore
    path: tuple[EdgeFromPath,...]

    def forSumo(self, graph):
        return ResultWithCost(self.total_cost, tuple(x.forSumo(graph) for x in self.path))

    def as_list_of_triplets(self, src_time):
        isFirst = True
        for x in self.path:
            if isFirst:
                yield x.as_first_triplet(src_time)
                isFirst = False
            else:
                yield x.as_triplet()

def split_edge_information(ls:tuple|list)->Iterator[EdgeFromPath]:
    """
    This function splits the path information returned by astar_loopfree_maximum_cost_search into a list of edge information
    :param ls:  Path information returned by astar_loopfree_maximum_cost_search
    :return:    Collection of Result information
    """
    N = len(ls)
    for idx in range(0,N,4):
        lst = ls[idx:idx+5]
        if len(lst) == 5:
            yield EdgeFromPath(lst[0], lst[1], lst[2], lst[3], lst[4])

def sum_triplet_costs(x, y):
    for key, group in itertools.groupby(itertools.chain(x,y), lambda x: (x[0], x[1])):
        for item in group:
            print(item)
        yield (key[0], key[1], sum(group))

# if __name__ == '__main__':
#     ls = [1, 2, 3.0, 4, 5, 6.0, 7]
#     print(list(split_edge_information(ls)))