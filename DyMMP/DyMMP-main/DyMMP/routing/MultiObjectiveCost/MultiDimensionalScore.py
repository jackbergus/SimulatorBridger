import itertools
import os
import sys

import numpy
import numpy as np


class MultidimensionalScore:
    def __init__(self, array : np.ndarray):
        assert isinstance(array, int) or isinstance(array, float) or isinstance(array, np.ndarray) or isinstance(array, list) or isinstance(array, tuple)
        if isinstance(array, int) or isinstance(array, float):
            array = [array]
        if isinstance(array, list) or isinstance(array, tuple):
            array = np.array(array)
        self.array = array

    def __getitem__(self, item):
        return self.array[item]

    def __eq__(self, other):
        return isinstance(other, MultidimensionalScore) and all(self.array == other.array)

    def __hash__(self):
        return hash(str(self.array))

    def list(self):
        return list(self.array)

    def set(self, idx, val):
        self.array[idx] = val

    def incrementWith(self, idx, val):
        array = numpy.array(self.array)
        array[idx] += val
        return MultidimensionalScore(array)

    def __repr__(self):
        return self.array.__repr__()

    def __str__(self):
        return str(self.array)

    def __add__(self, other):
        return MultidimensionalScore(self.array + other.array)

    def __iadd__(self, other):
        self.array += other.array
        return self

    def ltany(self, other):
        assert len(self.array) == len(other.array)
        for (val1, val2) in zip(self.array, other.array):
            if val1<val2:
                return True
        return False

    def __lt__(self, other):
        assert len(self.array) == len(other.array)
        for (val1, val2) in zip(self.array, other.array):
            if val1>=val2:
                return False
            elif val1<val2:
                return True
        return False

    def __le__(self, other):
        assert len(self.array) == len(other.array)
        for (val1, val2) in zip(self.array, other.array):
            if val1>val2:
                return False
            if val1<val2:
                return True
        return True

    @staticmethod
    def generate_ndim_zero_array(ndim):
        return MultidimensionalScore(np.zeros(ndim))

    @staticmethod
    def generate_ndim_max_array(ndim):
        return MultidimensionalScore(np.array([sys.float_info.max for _ in range(ndim)]))


class MultidimensionalScoresFromSoruce:
    def __init__(self, object:list[object]=None, costs:list[MultidimensionalScore]=None):
        if costs is None:
            costs = []
        if object is None:
            object = []
        self.objects = object
        self.costs = costs

    def do_objects_contain(self, x):
        return any(map(lambda y: x in y, self.objects))

    def __str__(self):
        string_ls = ["Resulting paths: "]
        for x, y in zip(self.objects, self.costs):
            string_ls.append(" - path:" + str(x)+" with cost: "+str(y))
        return os.linesep.join(string_ls)

    def __getitem__(self, item):
        return self.objects[item]

    def __repr__(self):
        return self.__str__()

    def __eq__(self, other):
        return sorted(self.costs) == sorted(other.costs)

    def add_cost(self, obj, cost):
        self.objects.append(obj)
        self.costs.append(cost)

    def append_with_extension(self, obj, cost, obj_append_f)->'MultidimensionalScoresFromSoruce':
        final_objects = [obj_append_f(x, obj) for x in self.objects]
        final_costs = [x + cost for x in self.costs]
        return MultidimensionalScoresFromSoruce(final_objects, final_costs)

    def sort_by_lex(self):
        lex_order = [i[0] for i in sorted(enumerate(self.objects), key=lambda x: x[1])]
        return lex_order

    def pick_lex_dimension(self):
        return self.costs[[i[0] for i in sorted(enumerate(self.objects), key=lambda x: x[1])][0]]


def is_pareto_efficient(costs, return_mask = True):
    """
    Find the pareto-efficient points
    :param costs: An (n_points, n_costs) array
    :param return_mask: True to return a mask
    :return: An array of indices of pareto-efficient points.
        If return_mask is True, this will be an (n_points, ) boolean array
        Otherwise it will be a (n_efficient_points, ) integer array of indices.
    """
    if isinstance(costs, list):
        costs = np.array(costs)
    is_efficient = np.arange(costs.shape[0])
    n_points = costs.shape[0]
    next_point_index = 0  # Next index in the is_efficient array to search for
    while next_point_index<len(costs):
        nondominated_point_mask = np.any(costs<costs[next_point_index], axis=1)
        nondominated_point_mask[next_point_index] = True
        is_efficient = is_efficient[nondominated_point_mask]  # Remove dominated points
        costs = costs[nondominated_point_mask]
        next_point_index = np.sum(nondominated_point_mask[:next_point_index])+1
    if return_mask:
        is_efficient_mask = np.zeros(n_points, dtype = bool)
        is_efficient_mask[is_efficient] = True
        return is_efficient_mask
    else:
        return is_efficient


def prune_with_pareto_optimality(orig: MultidimensionalScoresFromSoruce, neu: MultidimensionalScoresFromSoruce):
        costs = np.array([x.array for x in itertools.chain(orig.costs,neu.costs)])
        M = len(orig.objects)
        orig.objects += neu.objects
        N = len(orig.objects)
        mask = is_pareto_efficient(costs, True)
        final_objects = list(itertools.compress(orig.objects, mask))
        final_costs = list(itertools.compress(itertools.chain(orig.costs, neu.costs), mask))
        result = MultidimensionalScoresFromSoruce(final_objects, final_costs)
        return result, ((not np.all(mask)) or (max(np.arange(N)[mask]) >= M))