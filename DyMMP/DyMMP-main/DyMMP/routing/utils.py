import heapq

import numpy

from DyMMP.routing.MultiObjectiveCost.MultiDimensionalScore import is_pareto_efficient


def heap_top(ls):
    if len(ls)>0:
        return ls[1]
    else:
        return None

class PriorityQueue:
    def __init__(self, isParetoQueue=False):
        self.isParetoQueue = isParetoQueue
        self.isParetoOrdered = False
        if self.isParetoQueue:
            self.elements = dict()
        else:
            self.elements = list()
        self.blacklist = set()
        self.objects = set()
        self.ordered_elements = None

    def empty(self) -> bool:
        return not self.elements

    def remove(self, item):
        self.isParetoOrdered = False
        self.blacklist.add(item)
        self.objects.remove(item)

    def __contains__(self, item):
        return item in self.objects and item not in self.blacklist

    def put(self, item, priority): # (obj x ordered object) -> void
        if self.isParetoQueue:
            self.isParetoOrdered = False
            self.ordered_elements = None
            self.elements[item] = priority
        else:
            heapq.heappush(self.elements, (priority, item))
        self.objects.add(item)
        if item in self.blacklist:
            self.blacklist.remove(item)

    def insert(self, item, priority):
        self.put(item, priority)
        self.objects.add(item)
        if item in self.blacklist:
            self.blacklist.remove(item)

    def update(self, item, priority):
        if self.isParetoQueue:
            self.isParetoOrdered = False
            self.ordered_elements = None
            self.elements[item] = priority
        else:
            heapq.heapreplace(self.elements, (priority, item))
        self.objects.add(item)
        if item in self.blacklist:
            self.blacklist.remove(item)

    def get(self): # void -> (cost,obj)
        while len(self.elements) > 0:
            if not self.isParetoQueue:
                cost, obj = heapq.heappop(self.elements)
                self.objects.remove(obj)
                if obj not in self.blacklist:
                    return obj
            else:
                if not self.isParetoOrdered or self.ordered_elements is None:
                    self.ordered_elements = None
                    self.isParetoOrdered = True
                    items = [(x,y) for x,y in self.elements.items() if x not in self.blacklist]
                    self.blacklist.clear()
                    self.ordered_elements = self.sortParetoFrontLexicographically(items)
                while len(self.ordered_elements) > 0:
                    obj, cost = self.ordered_elements.pop(0)
                    del self.elements[obj]
                    return obj

        return None

    def sortParetoFrontLexicographically(self, items):
        priority_values = [x[1].array for x in items]
        priority_values = numpy.array(priority_values)
        mask = is_pareto_efficient(priority_values, True)
        front = sorted([idx for idx, val in enumerate(mask) if val], key=lambda x: items[x][1])
        filtered_items = [(x, y) for idx, (x, y) in enumerate(items) if not mask[idx]]
        if len(filtered_items) > 0:
            ls = self.sortParetoFrontLexicographically(filtered_items)
            return [items[x] for x in front] + ls
        else:
            return [items[x] for x in front]


    def top(self, object=True, default=None):
        assert not self.isParetoQueue
        if len(self.elements) > 0:
            if self.elements[0][1] not in self.blacklist:
                return self.elements[0][1] if object else self.elements[0][0]
            else:
                ls = [x for x in self.elements if x[1] not in self.blacklist]
                if len(ls)>0:
                    return min(ls)[1] if object else min(ls)[0]
                else:
                    return default
        else:
            return default

    def top_key(self, default):
        assert not self.isParetoQueue
        return self.top(False, default)
