import dataclasses
import enum
import math
import sys
from collections import defaultdict
from typing import Union

import numpy
import pandas
from DyMMP.TimedDistributions.sgolay2 import SGolayFilter2


@dataclasses.dataclass
class GolavFilter:
    window_size: int = 5
    poly_order: int = 5

@dataclasses.dataclass
class MedianFilter:
    size: int = 20
    mode: str = 'nearest'
    cval: float = 0.0

@dataclasses.dataclass
class Interpolate:
    method: str = 'linear'
    fill_value:float = math.nan
    tol:float = 1e-06
    maxiter:int = 400

Filter = Union[GolavFilter, MedianFilter,Interpolate]

class CostUpdateStrategy(enum.Enum):
    AddCost = 0
    SingleCost = 1
    SumToPreviousCost = 2

class RiskDistribution:
    def __init__(self, resolution_lat, resolution_lon, max_lat, min_lat, max_lon, min_lon, ddd, filter: Filter = None):
        self.min_lon = min_lon
        self.max_lon = max_lon
        self.min_lat = min_lat
        self.max_lat = max_lat
        self.resolution_lon = resolution_lon
        self.resolution_lat = resolution_lat
        self.arr = numpy.zeros((int(round(self.resolution_lat)), int(round(self.resolution_lon))))
        self.x_orig , self.y_orig = numpy.mgrid[0:int(round(self.resolution_lat)),0:int(round(self.resolution_lon))]
        self.x = self.x_orig/round(self.resolution_lat)*(self.max_lat-self.min_lat)+self.min_lat
        self.y = self.y_orig / round(self.resolution_lon) * (self.max_lon - self.min_lon) + self.min_lon
        self.d = defaultdict(list)
        for (lat, lon), val in ddd.items():
            n_lat = (lat - self.min_lat) / (self.max_lat - self.min_lat) * self.resolution_lat
            n_lon = (lon - self.min_lon) / (self.max_lon - self.min_lon) * self.resolution_lon
            self.d[(round(n_lat), round(n_lon))].append(val)
        self.filter = filter
        self.initialized = False
        self.index()

    def update_cost(self, lat, lon, new_cost, strategy:CostUpdateStrategy):
        self.initialized = False
        assert self.min_lat <= lat <= self.max_lat
        assert self.min_lon <= lon <= self.max_lon
        n_lat = round((lat - self.min_lat) / (self.max_lat - self.min_lat) * self.resolution_lat)
        n_lon = round((lon - self.min_lon) / (self.max_lon - self.min_lon) * self.resolution_lon)
        if strategy == CostUpdateStrategy.AddCost:
            self.d[(n_lat, n_lon)].append(new_cost)
        elif strategy == CostUpdateStrategy.SumToPreviousCost:
            self.d[(n_lat, n_lon)] = [max(self.d[(n_lat, n_lon)]) + new_cost]
        else:
            self.d[(n_lat, n_lon)] = [new_cost]

    def index(self):
        """
        This function re-applies the filter over the data when the cost is updated.
        This shall be called at initialization time, and after each time the costs have been changed.
        """
        if self.initialized:
            return
        for (x,y) in self.d:
            val = self.d[(x,y)]
            self.arr[int(x)-1,int(y)-1] = numpy.max(val)
        if self.filter is None:
            self.filter = GolavFilter()
        if isinstance(self.filter, GolavFilter):
            # del self.d
            self.scaled_array = SGolayFilter2(window_size=self.filter.window_size, poly_order=self.filter.poly_order)(
                self.arr)
        elif isinstance(self.filter, MedianFilter):
            # del d
            from scipy.ndimage import median_filter
            self.scaled_array = median_filter(self.arr, self.filter.size, mode=self.filter.mode, cval=self.filter.cval)
        elif isinstance(self.filter, Interpolate):
            arra = numpy.array(list([(int(x) - 1, int(y) - 1) for (x, y) in self.d]))
            vals = numpy.array([self.arr[k, v] for (k, v) in arra])
            if self.filter.method == 'CloughTocher':
                from scipy.interpolate import CloughTocher2DInterpolator
                interp = CloughTocher2DInterpolator(arra, vals, fill_value=self.filter.fill_value, tol=self.filter.tol,
                                                    maxiter=self.filter.maxiter)
            else:
                from scipy.interpolate import LinearNDInterpolator
                interp = LinearNDInterpolator(arra, vals, fill_value=self.filter.fill_value)
            self.scaled_array = interp(self.x_orig, self.y_orig)
        self.initialized = True

    def raw_value(self, la, lo, scaled=True):
        self.index()
        val = self.arr[la-1,lo-1] if not scaled else self.scaled_array[la-1,lo-1]
        return val if val >= 0.0 else sys.float_info.epsilon

    def value(self, lat, lon, scaled=True):
        la = int(round(self.lat(lat)))
        lo = int(round(self.lon(lon)))
        return self.raw_value(la, lo, scaled)

    def is_valid_lat(self, lat):
        return self.min_lat <= lat <= self.max_lat

    def is_valid_lon(self, lon):
        return self.min_lon <= lon <= self.max_lon

    def get_line(self, lat1, lon1, lat2, lon2):
        x1 = int(round(self.lat(lat1)))
        y1 = int(round(self.lon(lon1)))
        x2 = int(round(self.lat(lat2)))
        y2 = int(round(self.lon(lon2)))

        # Bresenham's line algorithm (https://en.wikipedia.org/wiki/Bresenham's_line_algorithm): https://dl.acm.org/doi/10.1145/359423.359432
        from bresenham import bresenham
        return bresenham(x1, y1, x2, y2)

    def get_distribution_in_line(self, lat1, lon1, lat2, lon2, aggregation='max', scaled=False):
        # Bresenham's line algorithm (https://en.wikipedia.org/wiki/Bresenham's_line_algorithm): https://dl.acm.org/doi/10.1145/359423.359432
        values = list()
        for pt in self.get_line(lat1, lon1, lat2, lon2):
            values.append(self.raw_value(pt[0], pt[1], scaled=scaled))
        if aggregation == 'max':
            return max(values)
        elif aggregation == 'mean' or aggregation == 'avg':
            return sum(values)/float(len(values))
        elif aggregation == 'sum':
            return sum(values)
        raise Exception(f"Unknown aggregation: {aggregation}")

    def clamp(self, n, min, max):
        if n < min:
            return min
        elif n > max:
            return max
        else:
            return n

    def lat(self, value):
        value = self.clamp(value, 1, self.max_lat)
        return (value - self.min_lat) / (self.max_lat - self.min_lat) * math.floor(self.resolution_lat)

    def lon(self, value):
        value = self.clamp(value, 1, self.max_lon)
        return (value - self.min_lon) / (self.max_lon - self.min_lon) * self.resolution_lon

    def plot(self):
            import matplotlib.pyplot as plt
            fig = plt.figure()
            ax = fig.add_subplot(111, projection='3d')
            # ax.plot_wireframe(self.x, self.y, self.arr, linewidths=0.5, color='r')
            # ax.scatter(self.x, self.y, self.arr, s=5, c='r')
            # ax.plot_surface(self.x, self.y, zs, linewidth=0)
            ax.plot_surface(self.x, self.y, self.scaled_array, color='y', linewidth=0, alpha=0.4)
            plt.show()

class RiskDistributionBuilder:
    def __init__(self):
        self.d = dict()
        self.max_lat = -sys.float_info.max
        self.max_lon = -sys.float_info.max
        self.min_lat =  sys.float_info.max
        self.min_lon =  sys.float_info.max
        self.lats = set()
        self.lons = set()

    def put(self, lat, lon, value):
        self.max_lat = max(self.max_lat, lat)
        self.min_lat = min(self.min_lat, lat)
        self.max_lon = max(self.max_lon, lon)
        self.min_lon = min(self.min_lon, lon)
        self.d[(lat, lon)] = value
        self.lats.add(lat)
        self.lons.add(lon)

    def put_from_lat_lon_value_file(self, filename, lat_n="lat", lon_n="lon", val_n="risk_score"):
        pandas_geoloc = pandas.read_csv(filename)
        for record in pandas_geoloc.to_dict('records'):
            lat = record[lat_n]
            lon = record[lon_n]
            val = record[val_n]
            self.put(lat, lon, val)

    def build(self, scaling: float = 0.0001, filter: Filter = None):
        resolution_lat =  (self.max_lat - self.min_lat) / min(numpy.diff(numpy.array(sorted(list(self.lats))))) * scaling
        resolution_lon = (self.max_lon - self.min_lon) / min(
            numpy.diff(numpy.array(sorted(list(self.lons))))) * scaling
        return RiskDistribution(resolution_lat, resolution_lon, self.max_lat, self.min_lat, self.max_lon, self.min_lon, self.d, filter)

if __name__ == '__main__':
    b1 = RiskDistributionBuilder()
    b1.put_from_lat_lon_value_file("../../data/bolo/max_frail_distribution.csv")
    r = b1.build(0.0001, Interpolate())
    r.plot()
    b1 = RiskDistributionBuilder()
    b1.put_from_lat_lon_value_file("../../data/bolo/max_frail_distribution.csv")
    r = b1.build(0.0001)
    r.plot()