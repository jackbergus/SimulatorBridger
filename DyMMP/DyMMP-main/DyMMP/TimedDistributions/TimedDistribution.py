import bisect
import sys
from datetime import datetime
from typing import List

import networkx
import dateutil
import pandas
from rasterio.crs import defaultdict

from DyMMP.TimedDistributions.RiskDistribution import RiskDistributionBuilder, Filter, RiskDistribution


def split_segment_in_k(x1, y1, x2, y2, k):
    """
    Splits a segmnt into k different parts
    :param x1:  X coordinate, first point
    :param y1:  Y coordinate, first point
    :param x2: X coordinate, second point
    :param y2: Y coordinate, second point
    :param k:
    :return:        The k splits of the given segment
    """
    dy = (y2 - y1)/k
    dx = (x2 - x1)/k
    x0 = float(x1)
    y0 = float(y1)
    results = []
    for i in range(k):
        cp = (x0+dx,y0+dy)
        results.append([(x0,y0),cp])
        x0, y0 = cp[0], cp[1]
    return results


def lowerBound(arr, target, return_idx=False):
    lo = 0
    N = len(arr)
    hi = N - 1
    res = N
    while lo <= hi:
        mid = lo + (hi - lo) // 2
        # If arr[mid] >= target, then mid can be the
        # lower bound, so update res to mid and
        # search in left half, i.e. [lo...mid-1]
        if arr[mid] >= target:
            res = mid
            hi = mid - 1
        # If arr[mid] < target, then lower bound
        # cannot lie in the range [lo...mid] so
        # search in right half, i.e. [mid+1...hi]
        else:
            lo = mid + 1
    idx = min(max(res,0), N-1)
    return arr[idx] if not return_idx else idx

# Function to find the upper bound of a number
def upperBound(arr, target, return_idx=False):
    N = len(arr)
    lo, hi = 0, N - 1
    res = len(arr)

    while lo <= hi:
        mid = lo + (hi - lo) // 2

        # If arr[mid] > target, then arr[mid] can be
        # the upper bound so store mid in result and
        # search in left half, i.e. arr[lo...mid-1]
        if arr[mid] > target:
            res = mid
            hi = mid - 1

        # If arr[mid] <= target, then upper bound
        # cannot lie in the range [lo...mid], so
        # search in right half, i.e. arr[mid+1...hi]
        else:
            lo = mid + 1

    idx = min(max(res,0), N-1)
    return arr[idx] if not return_idx else idx

class TimedRiskDistribution:
    def __init__(self, times, d):
        self.d: dict[object,RiskDistribution] = d # rd_map
        self.times = list(times)
        self.times.sort()
        self.alpha = self.times[0]
        self.omega = self.times[-1]

    def update_cost(self, lat, lon, time, new_cost, strategy):
        final_time = self.times[bisect.bisect_left(self.times, time)]
        return self.d[final_time].update_cost(lat, lon, new_cost, strategy)

    def start_value(self, lat, lon, time, scaled=True):
        final_time = self.times[bisect.bisect_left(self.times, time)]
        return self.d[final_time].value(lat, lon, scaled)

    def end_value(self, lat, lon, time, scaled=True):
        final_time = lowerBound(self.times, time)
        return self.d[final_time].value(lat, lon, scaled)

    def is_valid_lat(self, lat):
        return self.d[self.alpha].is_valid_lat(lat)

    def is_valid_lon(self, lat):
        return self.d[self.alpha].is_valid_lon(lat)

    def get_distribution_in_line(self, lat1, lon1, lat2, lon2, beginning_time, duration, aggregation='max', scaled=False):
        ending_time = upperBound(self.times, beginning_time+duration, return_idx=True)
        beginning_time = lowerBound(self.times, beginning_time, return_idx=True)
        assert beginning_time <= ending_time
        duration = ending_time-beginning_time+1
        idx = 0
        result = 0.0
        for segment in split_segment_in_k(lat1, lon1, lat2, lon2, duration):
            result += self.d[self.times[idx+beginning_time]].get_distribution_in_line(segment[0][0], segment[0][1], segment[1][0], segment[1][1], aggregation, scaled)
            idx += 1
        return result

    def start_time_plot(self, time):
        final_time = self.times[bisect.bisect_left(self.times, time)]
        self.d[final_time].plot()

    def end_time_plot(self, time):
        final_time = upperBound(self.times, time)
        self.d[final_time].plot()

    @staticmethod
    def from_gnn_prediction_csv(junction_position_map: dict[str, (float, float)],
                                scaling,
                                filter,
                                file:str,
                                w=None,
                                additional_traffic_positions : List[tuple[float, str, float]]=None)->'TimedRiskDistribution':
        if w is None and file is not None:
            w = pandas.read_csv(file, index_col=0)
        elif w is None:
            raise RuntimeError('w is None')
        if additional_traffic_positions is None:
            additional_traffic_positions = list()
        else:
            additional_traffic_positions.sort()
        rd_map = dict()
        ls = list()
        lsNext = list(zip(w.index, w.to_dict('records')))
        N = len(lsNext)
        records = defaultdict(float)
        for index, (timestamp, record) in enumerate(lsNext):
            ts = dateutil.parser.parse(timestamp)
            float_ts = ts.timestamp()
            float_ts_up = dateutil.parser.parse(lsNext[index+1][0]).timestamp() if N-1 != index else sys.float_info.max
            ls.append(ts)
            b = RiskDistributionBuilder()
            records.clear()
            while (len(additional_traffic_positions)>0) and (float_ts <= additional_traffic_positions[0][0] <= float_ts_up):
                timestamp_add, junction_id_add, val_add = additional_traffic_positions.pop(0)
                records[junction_id_add] += val_add
            for junction, val in record.items():
                val = max(val, 0.0)
                orig_value = records.get(junction, 0.0)
                val += orig_value
                assert junction in junction_position_map
                lat, lon = junction_position_map[junction]
                b.put(lat, lon, val)

            for remaining_junction in set(records.keys()).difference(set(record.keys())).intersection(set(junction_position_map.keys())):
                lat, lon = junction_position_map[remaining_junction]
                val = records[remaining_junction]
                b.put(lat, lon, val)
            rd_map[ts] = b.build(scaling, filter)
        return TimedRiskDistribution(ls, rd_map)


def load_junction_position_map_from_gexf_file(g, filename=None)->tuple[dict[str, (float, float)],dict[str,str],networkx.DiGraph] :
    if g is None and filename is not None:
        g = networkx.read_gexf(filename)
    elif g is None:
        raise Exception("No graph given")
    junction_id_to_position: dict[str, (float, float)] = dict()
    junction_name_to_id: dict[str,str] = dict()
    for node_id, node in dict(g.nodes.data()).items():
        node_label = node["label"]
        assert node["name"] not in junction_name_to_id
        junction_name_to_id[node["name"]] = node["label"]
        lat = node["lat"]
        lon = node["lon"]
        junction_id_to_position[node_label] = (lat, lon)
    return junction_id_to_position, junction_name_to_id, g

def estimation_error(csv_1, csv_2):
    p1 = pandas.read_csv(csv_1, index_col=0)
    lower_bound_heuristic = pandas.read_csv(csv_2, index_col=0)
    errors = dict(abs(p1-lower_bound_heuristic).max(axis='rows'))
    for k in errors:
        lower_bound_heuristic[k] = (lower_bound_heuristic[k] - errors[k]).apply(lambda x: max(x, 0.0))
    traffic_predictions = pandas.read_csv(csv_2, index_col=0)
    return traffic_predictions, lower_bound_heuristic

if __name__ == '__main__':
    scaling: float = 0.0001
    filter: Filter = None

    estimation_error("/media/giacomo/Biggus/bakuppo/data/Bologna/gnn_traffic/gnn_network_minlen_predictions/all_original.csv",
                     "/media/giacomo/Biggus/bakuppo/data/Bologna/gnn_traffic/gnn_network_minlen_predictions/all_predictions.csv")

    junction_id_to_position, name_to_id, g = load_junction_position_map_from_gexf_file("/media/giacomo/Biggus/bakuppo/data/Bologna/planner_graph.gexf")
    expected_qos = TimedRiskDistribution.from_gnn_prediction_csv(junction_id_to_position,
                                                  "/media/giacomo/Biggus/bakuppo/data/Bologna/gnn_traffic/gnn_network_minlen_predictions/original.csv")

    #
    # lat_lon_file = "/media/giacomo/Biggus/bakuppo/data/Bologna/traffic_stations.csv"
    # lldf = pandas.read_csv(lat_lon_file)

    ## TOFIX: latlon file, wrong element ids being provided


    file = "/media/giacomo/Biggus/bakuppo/data/Bologna/gnn_traffic/gnn_network_minlen_predictions/original.csv"
    w = pandas.read_csv(file,index_col=0)
    rd_map = dict()
    from dateutil import parser
    times = [parser.parse(x) for x in w.index]
    print(upperBound(times, parser.parse("2026")))
    print(lowerBound(times, parser.parse("2026")))
    print(upperBound(times, parser.parse("2017")))
    print(lowerBound(times, parser.parse("2017")))
    for timestamp, record in zip(w.index,w.to_dict('records')):
        b = RiskDistributionBuilder()
        for junction, val in record.items():
            assert junction in junction_id_to_position
            lat, lon = junction_id_to_position[junction]
            b.put(lat, lon, val)
        rd_map[timestamp] = b.build(scaling, filter)