import json
import math
import operator
import random
import statistics
import sys
from collections import defaultdict
from pathlib import Path
import dataclasses
import os.path
from typing import Optional

import networkx
import numpy
from lxml import etree
from networkx import DiGraph, MultiDiGraph
import sumolib
import os
import pandas
import osmnx as ox
import subprocess
import xarray
from scipy.spatial import KDTree

from DyMMP.dataintergration.JavaClasses import GNNEdge
from DyMMP.dataintergration.OpenChargerData import OpenChargeMapRequest, send_request
from DyMMP.dataintergration.OpenStreetMap import Sumo
from DyMMP.dataintergration.UrbanObservatoryFiles import dowload_and_extract
from DyMMP.dataintergration.utils import dataclasses_to_csv, urlify, get_proxies, get_distance_in_meters
from DyMMP.evVanetSim.EVBattery.CSSim import ChargingStationConf
from DyMMP.evVanetSim.EVBattery.UnitsOfMeasure import VelocityUnit, Velocity, Distance, DistanceUnit
from DyMMP.TimedDistributions.RiskDistribution import Filter, RiskDistribution, RiskDistributionBuilder


@dataclasses.dataclass
class SensorInformation:
    sensor_id: str
    sensor_name: str
    lon: str
    lat: str
    time: str
    count: str

    def list(self):
        return [self.sensor_id, self.sensor_name, self.lon, self.lat, self.time, self.count]

@dataclasses.dataclass
class ForRiskDistribution:
    file_name: str
    scaling: float
    filter: Filter
    aggregation: str
    useScaled: bool

@dataclasses.dataclass
class DataIntegrationConfiguration:
    walking: str
    vehicles: str
    netconvert_binary: str
    place_name: str
    velocity_unit: str
    distance_unit: str
    # pedestrian_fields: SensorInformation
    vehicular_fields: SensorInformation
    sumo:Sumo
    gdal_translate:str
    aggregate_edges_to_nodes :bool
    chargers: Optional[OpenChargeMapRequest]
    car_configuration: Optional[str]
    desirability_distribution: Optional[ForRiskDistribution]

def prune_graph_node_with_outdegree(G:DiGraph, node, outdegree):
    edges = list(G.edges(node, data="weight"))
    edges = sorted(edges, key=lambda x:x[2])[outdegree:]
    for (src, dst, _) in edges:
        assert src == node
        G.remove_edge(src, dst)
    return G

def latlon_to_meters(lat1, lon1, lat2, lon2):#https://en.wikipedia.org/wiki/Haversine_formula
    R = 6378.137 # Radius of earth in KM
    dLat = lat2 * math.pi / 180 - lat1 * math.pi / 180
    dLon = lon2 * math.pi / 180 - lon1 * math.pi / 180
    a = math.sin(dLat/2) * math.sin(dLat/2) + math.cos(lat1 * math.pi / 180) * math.cos(lat2 * math.pi / 180) * math.sin(dLon/2) * math.sin(dLon/2)
    c = 2 * math.atan2(math.sqrt(a), math.sqrt(1-a))
    d = R * c
    return d * 1000 #// meters

class DataIntegration:
    def __init__(self, conf:DataIntegrationConfiguration):

        ## Empty Initializations
        self.edges = set()
        self.edges_latlon = set()
        self.connections = set()
        self.net = None
        self.chargers = None
        self.charger = set()
        self.node_id_dict = dict()
        self.node_id_to_name = list()
        self.coord_list = []
        self.coord_dict = dict()
        self.node_to_coord = dict()
        self.geotopo_coord_order = list()
        self.count_external_calls = 0
        self.proxy_counter = 0
        self.networkG = MultiDiGraph()
        self.kdTree_altitude = None
        self.kdTree_graphnodes = None
        self.sensor_name_to_id = dict()
        self.ts_sensor_name_to_time_to_value = defaultdict(lambda: defaultdict(int))
        self.datetime_conf = dict()
        self.visited_ids = set()
        self.junction_to_nearest_sensors = defaultdict(set)
        self.useSumoData = False
        self.trace_file = None

        ## Some configuration-based initializations
        self.conf = conf
        # self.proxies = list(get_proxies())
        self.network_base_name = urlify(self.conf.place_name)
        self.velocity_unit = VelocityUnit[self.conf.velocity_unit]
        self.distance_unit = DistanceUnit[self.conf.distance_unit]

        ## Setting up the vehicle information
        self.car_configuration = None
        if conf.car_configuration is not None:
            from DyMMP.evVanetSim.EVBattery.EVMatlab.conf import VehicleEntryPoint
            self.car_configuration = VehicleEntryPoint(conf.car_configuration)

        ## Setting up the desirability distribution
        self.desirability_distribution = None
        self.des_aggr = None
        self.useScale = False
        if conf.desirability_distribution is not None:
            self.des_aggr = conf.desirability_distribution.aggregation
            self.useScale = conf.desirability_distribution.useScaled
            b = RiskDistributionBuilder()
            b.put_from_lat_lon_value_file(conf.desirability_distribution.file_name)
            self.desirability_distribution = b.build(conf.desirability_distribution.scaling,
                                                              conf.desirability_distribution.filter)
        ## Data Preparation
        Path(os.path.join("data", self.conf.place_name)).mkdir(parents=True, exist_ok=True)
        Path(os.path.join("data", self.conf.place_name, "gnn_traffic")).mkdir(parents=True, exist_ok=True)

    def estimate_road_consumption(self, distance_meters:float, velocty_mph:float, grade:float):
        power_consumption = None
        time_travel = None
        if self.car_configuration is not None:
            distance = Distance(distance_meters)
            velocity = Velocity(velocty_mph,VelocityUnit.mph)
            power_consumption, time_travel = self.car_configuration.estimate_road_consumption(distance, velocity, grade)
            time_travel = time_travel.seconds
        return power_consumption, time_travel

    def get_user_desirability_distribution(self, lat1, lon1, lat2, lon2):
        val = sys.float_info.epsilon
        if (self.desirability_distribution is not None and
                self.desirability_distribution.is_valid_lat(lat1) and
                self.desirability_distribution.is_valid_lon(lon1) and
                self.desirability_distribution.is_valid_lon(lon2) and
                self.desirability_distribution.is_valid_lat(lat2)):
            val = self.desirability_distribution.get_distribution_in_line(lat1, lon1, lat2, lon2, self.des_aggr, self.useScale)
        return -math.log(val)

    def get_user_desirability_distribution_node(self, lat1, lon1):
        val = sys.float_info.epsilon
        if self.desirability_distribution is not None:
            val = self.desirability_distribution.value(lat1, lon1, self.useScale)
        return -math.log(val)

    def prepare_datasets(self):
        if (self.conf.vehicles is not None and len(self.conf.vehicles)>0) and not os.path.exists(os.path.join("data", self.conf.place_name, "Vehicles.csv")):
            # print("Downloading the pedestrian data")
            # dowload_and_extract(self.conf.walking, "Walking.csv")
            print("preparing: Downloading the vehicular data")
            dowload_and_extract(self.conf.vehicles, self.conf.place_name, "Vehicles.csv")
        elif self.conf.sumo.binary is not None and os.path.exists(self.conf.sumo.binary) and os.path.exists(self.conf.sumo.net) and os.path.exists(self.conf.sumo.config) and self.conf.sumo.begin>=0 and self.conf.sumo.end>=0 and self.conf.sumo.step>=0:
            self.useSumoData = True
            self.trace_file = os.path.join("data", self.conf.place_name, f"trace_file.xml")
            if not os.path.isfile(self.trace_file):
                print("preparing: Running Sumo")
                subprocess.run([self.conf.sumo.binary, "-c", self.conf.sumo.config, "--begin",
                                str(self.conf.sumo.begin), "--end", str(self.conf.sumo.end), "--step-length",
                                str(self.conf.sumo.step), "--fcd-output", self.trace_file])
            else:
                print(f"preparing: Trace file already available from: {self.trace_file}")

        print("Downloading the city map")
        self._import_place_information()
        if self.conf.chargers is not None:
            print("Downloading/loading the e-charging stations")
            self.chargers = send_request(self.conf.chargers)
            print("Parsing e-charging data")
            self._parse_charger_data()
        print("Loading the vehicular data")
        self._read_uo_dataset(True)

    def _parse_charger_data(self):
        """
        Future: also consider battery recharging --- Parsing the battery charging dataset
        :return:
        """
        for x in self.chargers:
            ID = x["ID"]
            UUID = x["UUID"]
            Title = x["AddressInfo"]["Title"]
            Latitude = x["AddressInfo"]["Latitude"]
            Longitude = x["AddressInfo"]["Longitude"]
            ls = list(self.query_for_nearest_junction(Latitude, Longitude))
            # assert len(ls) == 1
            random.shuffle(ls)
            for y, (nodeLat, nodeLong) in ls:
                if y not in self.visited_ids:
                    self.visited_ids.add(y)
                    if len(x["Connections"]) > 0:
                        ## Just picking the first connection
                        # amps = x["Connections"][0]["Amps"]
                        # voltage = x["Connections"][0]["Voltage"]
                        powerkw = x["Connections"][0]["PowerKW"]
                        self.charger.add(ChargingStationConf(len(self.charger), powerkw, nodeLat, nodeLong))

    def _process_node(self, node):
            """
            Adding a node to the graph
            :param node:
            :return:
            """
            name = node.getID()
            x, y = node.getCoord()
            lon, lat = self.net.convertXY2LonLat(x, y)
            if name not in self.node_id_dict:
                self.node_to_coord[len(self.node_id_dict)] = (lat, lon)
                self.networkG.add_node(len(self.node_id_dict))
                self.node_id_dict[name] = len(self.node_id_dict)
                self.node_id_to_name.append(name)
            self.coord_list.append((lat, lon))

    def init_query_for_geographical_coordinates(self):
        """returns a list containing the bottom left and the top right
        points in the sequence
        Here, we traverse the collection of points only once,
        to find the min and max for x and y
        """
        bot_left_x, bot_left_y = float('inf'), float('inf')
        top_right_x, top_right_y = float('-inf'), float('-inf')
        for x, y in self.coord_list:
            bot_left_x = min(bot_left_x, x)
            bot_left_y = min(bot_left_y, y)
            top_right_x = max(top_right_x, x)
            top_right_y = max(top_right_y, y)

        import elevation
        import os
        # cwd = os.getcwd()
        if not os.path.exists(os.path.join("data", self.conf.place_name, "coordinates.tif")) or not os.path.exists(os.path.join("data", self.conf.place_name, "coordinates.nc")):
            coordinates_tif = "coordinates.tif"
            coordinates_nc = "coordinates.nc"
            elevation.clip(bounds=(bot_left_y, bot_left_x, top_right_y, top_right_x),
                           output=coordinates_tif)
            elevation.clean()
            new_coordinats_tif = os.path.join(elevation.CACHE_DIR, elevation.DEFAULT_PRODUCT, coordinates_tif)
            assert os.path.exists(new_coordinats_tif)
            subprocess.run([self.conf.gdal_translate, "-of", "NetCDF", new_coordinats_tif, coordinates_nc])
            import shutil
            shutil.move(new_coordinats_tif, os.path.join("data", self.conf.place_name, "coordinates.tif"))
            shutil.move(coordinates_nc, os.path.join("data", self.conf.place_name, "coordinates.nc"))
        ds = xarray.open_dataset(os.path.join("data", self.conf.place_name, "coordinates.nc"))
        df = ds.to_dataframe().reset_index()
        X = df[["lat", "lon"]].to_numpy()
        self.kdTree_altitude = KDTree(X)
        for row_dict in df.to_dict(orient="records"):
            (lat, lon) = row_dict["lat"], row_dict["lon"]
            self.geotopo_coord_order.append((lat, lon))
            self.coord_dict[(lat, lon)] = row_dict["Band1"]

    def query_for_altitude(self, lat, lon):
        assert self.kdTree_altitude is not None
        dd, _ = self.kdTree_altitude.query((lat, lon), k=1)
        ii = self.kdTree_altitude.query_ball_point((lat, lon), math.nextafter(dd, 1))
        altitude = statistics.mean([self.coord_dict[self.geotopo_coord_order[idx]] for idx in ii])
        return altitude

    def query_for_nearest_junction(self, lat, lon):
        assert self.kdTree_graphnodes is not None
        dd, _ = self.kdTree_graphnodes.query((lat, lon), k=1)
        ii = self.kdTree_graphnodes.query_ball_point((lat, lon), math.nextafter(dd, 1))
        return [(x, self.node_to_coord[x]) for x in ii]

    def process_graph(self):
        vehs = set(self.conf.sumo.allowed)
        selected_edges = []

        for edge in self.net.getEdges():
            if (not edge.getType() in self.conf.sumo.ignore_links) and len(vehs.intersection(set().union(*[x.getPermissions() for x in edge.getLanes()])))>0:
                selected_edges.append(edge)
                src = edge.getFromNode()
                dst = edge.getToNode()
                self._process_node(src)
                self._process_node(dst)
        # self._resolve_coordinate_list(True)

        self.init_query_for_geographical_coordinates()
        sorted_x = [x[1] for x in sorted(self.node_to_coord.items(), key=operator.itemgetter(0))]
        self.kdTree_graphnodes = KDTree(sorted_x)

        node_properties = {}
        for name in self.node_id_dict:
            id = self.node_id_dict[name]
            lat, lon = self.node_to_coord[id]
            altitude = self.query_for_altitude(lat, lon)
            desirability = self.get_user_desirability_distribution_node(lat, lon)
            node_properties[id] = {"name": name, "lat": lat, "lon": lon, "altitude": altitude, "local_desirability": desirability}

        networkx.set_node_attributes(self.networkG, node_properties)

        for edge in selected_edges:
            name = edge.getName()
            id = edge.getID()
            src = edge.getFromNode()
            dst = edge.getToNode()
            x, y = src.getCoord()
            velocity = Velocity(edge.getSpeed(), self.velocity_unit)
            distance = Distance(edge.getLength(), self.distance_unit).meter
            lon1, lat1 = self.net.convertXY2LonLat(x, y)
            x, y = dst.getCoord()
            lon2, lat2 = self.net.convertXY2LonLat(x, y)

            meters_distance_heuristic = get_distance_in_meters(lat1, lon1, lat2, lon2)
            height = max(self.query_for_altitude(lat2, lon2) - self.query_for_altitude(lat1, lon1), 0.0)
            grade_heur = height / meters_distance_heuristic * 100.0
            grade = height / distance * 100.0
            time_s_heur = distance / velocity.mps
            d ={"relu_grade_heur": grade_heur,                #Slope using the euclidean distance
                "relu_grade": grade,                          #Slope using the actual road length
                "distance_m_heur": meters_distance_heuristic, #euclidean distance (heuristic)
                "distance_m": distance,                 #actual road length
                "velocity_mps": velocity.mps,                     #Maximum road speed
                "time_s_heur": time_s_heur,
                "sumo_name": name,
                "sumo_id": id}    #Estimated time to travel, considering no acceleration from zero (at full speed)

            ## Adding the vehicular information to the edge
            battery_cons, actual_time = self.estimate_road_consumption(distance, velocity.mph, grade)
            battery_cons_heur, _ = self.estimate_road_consumption(Distance(meters_distance_heuristic, DistanceUnit.meter), velocity.mph, grade)
            if battery_cons is not None and actual_time is not None:
                d["battery_cons"] = battery_cons              #Battery consumption for traversing the road by accelearing from 0 to velocity -- estimating traffic stop condition
                d["battery_cons_heur"] = battery_cons_heur
                d["time_s"] = actual_time                     #Time as by accelerating from 0 to velocity -- estimating traffic stop condition
            else:
                d["battery_cons"] = 0.0
                d["battery_cons_heur"] = 0.0
                d["time_s"] = time_s_heur

            ## Adding user desirability distribution for the area
            d["desirability"] = self.get_user_desirability_distribution(lat1, lon1, lat2, lon2)

            ## Adding zone desirability considerations
            self.networkG.add_edge(self.node_id_dict[src.getID()],
                                   self.node_id_dict[dst.getID()],
                                   **d)


    def _import_place_information(self):
        ox.settings.all_oneway = True
        file_sumo = self.conf.sumo.net if (self.conf.sumo.net is not None and len(self.conf.sumo.net) >0) else os.path.join("data", self.conf.place_name, f"{self.network_base_name}.net.xml")
        # file_graphml = os.path.join("data", f"{self.network_base_name}.graphml")
        # file_parking = os.path.join("data", "newcastle_upon_tyne.parking.xml")
        if not os.path.exists(file_sumo):
            ## If you have no osm network file, then you'd have to retrieve it from the
            file = os.path.join("data", self.conf.place_name, f"{self.network_base_name}.osm.xml")
            if not os.path.isfile(file):
                self.G = ox.graph_from_place(self.conf.place_name, network_type='drive', simplify=False)
                original_elevation_url = ox.settings.elevation_url_template
                ox.settings.elevation_url_template = (
                    "https://api.opentopodata.org/v1/aster30m?locations={locations}"
                )
                # self.G = ox.elevation.add_node_elevations_google(self.G, batch_size=100, pause=1)
                # self.G = ox.elevation.add_edge_grades(self.G)
                # ox.settings.elevation_url_template = original_elevation_url
                ox.io.save_graph_xml(self.G, filepath=file)
                # ox.io.save_graphml(self.G, filepath=file_graphml)
            subprocess.run(
                [self.conf.netconvert_binary, "--osm", file, "-o", file_sumo, "--geometry.remove", "--ramps.guess", "--junctions.join",
                 "--tls.guess-signals", "--tls.join", "--tls.default-type", "actuated"])
        # else:
        #     # self.G = ox.io.load_graphml(file_graphml)
        self.net = sumolib.net.readNet(file_sumo)
        # lon1, lat1 = self.net.convertXY2LonLat(0,0)
        # lon2, lat2 = self.net.convertXY2LonLat(0, 1)
        # lon3, lat3 = self.net.convertXY2LonLat(1, 0)
        # m1 = latlon_to_meters(lat1, lon1, lat2, lon2)
        # m2 = latlon_to_meters(lat1, lon1, lat3, lon3)
        self.process_graph()

    def _read_uo_dataset(self, isVehicles=False):
        if not self.useSumoData:
            self.read_observatory_data(isVehicles)
        elif isVehicles:
            self._parse_sumo_vehicular_traces()

    def _parse_sumo_vehicular_traces(self):
        """
        Internal: This method parses the traces information and provides some connection counting
        :return:
        """
        from DyMMP.dataintergration.JavaClasses import TimedEdge
        from DyMMP.dataintergration.JavaClasses import EdgeConnectionsPerSimulationTime
        rsu = set()
        assert self.trace_file is not None
        with open(self.conf.sumo.net) as f:
            tree = etree.parse(f)
            result = tree.xpath("/net/junction[@type='traffic_light']")
            for no, tl in enumerate(result):
                curr = tl.attrib
                name = curr["id"]
                if name not in self.sensor_name_to_id:
                    self.sensor_name_to_id[name] = len(self.sensor_name_to_id)
                    self.junction_to_nearest_sensors[self.node_id_dict[name]].add(name)
                x = float(curr["x"])
                y = float(curr["y"])
                rsu.add(TimedEdge(name, x, y, 0, self.conf.sumo.default_rsu_communication_radius,
                                  self.conf.sumo.default_max_vehicle_communication))
        met_junctions = set()
        d = defaultdict(lambda: defaultdict(int))
        ordered_timestamps = list()

        import xml.etree.ElementTree as ET
        S = []
        idxL = []
        if True:
            doc = ET.iterparse(self.trace_file, events=("start", "end"))
            _, root = next(doc)
            current_time = -1
            for event, element in doc:
                if event == "start" and element.tag == "timestep":
                    current_time = float(element.attrib["time"])
                    print(current_time)
                    S.clear()
                    idxL.clear()
                elif event == "start" and element.tag == "vehicle":
                    # curr = trace.attrib
                    attrs = element.attrib
                    x = float(attrs["x"])
                    y = float(attrs["y"])
                    id = attrs["id"]
                    S.append((x, y))
                    idxL.append(id)
                elif event == "end" and element.tag == "timestep":
                    if len(S)>0:
                        ss = numpy.array(S)
                        kd_tree = KDTree(S)
                        for x in rsu:
                            ii = kd_tree.query_ball_point((x.x, x.y), math.nextafter(x.communication_radius, 1))
                            assert all(map(lambda x: x < len(idxL), ii))
                            n = len(ii)
                            if n == 0:
                                continue
                            self.ts_sensor_name_to_time_to_value[x.id][current_time] = self.ts_sensor_name_to_time_to_value[x.id][current_time] + n
                            # print(f"{x.id}@{current_time} = {self.ts_sensor_name_to_time_to_value[x.id][current_time]}")
                            self.datetime_conf[current_time] = current_time
                            lon, lat = self.net.convertXY2LonLat(x.x, x.y)
                            if not self.conf.aggregate_edges_to_nodes:
                                self.edges.add(TimedEdge(x.id, x.x, x.y))
                                self.edges_latlon.add(GNNEdge(x.id, self.sensor_name_to_id[x.id], lat, lon))
                                self.connections.add(EdgeConnectionsPerSimulationTime(current_time, x.id, n))
                            else:
                                if current_time not in ordered_timestamps:
                                    ordered_timestamps.append(current_time)
                                ls = self.query_for_nearest_junction(lat, lon)
                                assert len(ls) == 1
                                node_idx = ls[0][0]
                                jlat, jlon = ls[0][1]
                                node_id = self.node_id_to_name[node_idx]
                                if node_idx not in met_junctions:
                                    jx, jy = self.net.convertLonLat2XY(jlon, jlat)
                                    self.edges_latlon.add(GNNEdge(node_id, node_idx, jlat, jlon))
                                    self.edges.add(TimedEdge(node_id, jx, jy))
                                    met_junctions.add(node_id)
                                d[current_time][node_id] = d[current_time][node_id] + n
                            ss = numpy.delete(ss, ii, axis=0)
                            if len(ss) == 0:
                                break
                            kd_tree = KDTree(ss)

                    root.clear()  # Keep memory low

        if self.conf.aggregate_edges_to_nodes:
            ordered_timestamps.sort()
            met_junctions = list(met_junctions)
            for timestamp in ordered_timestamps:
                for junction_id in met_junctions:
                    junction = self.node_id_to_name[junction_id]
                    self.connections.add(EdgeConnectionsPerSimulationTime(timestamp, junction, d[timestamp][junction]))

    def read_observatory_data(self, isVehicles):
        from DyMMP.dataintergration.JavaClasses import TimedEdge
        from DyMMP.dataintergration.JavaClasses import EdgeConnectionsPerSimulationTime
        name = 'Walking.csv' if not isVehicles else 'Vehicles.csv'
        # prefix = "ped" if not isVehicles else 'veh'
        conf_fields = self.conf.pedestrian_fields if not isVehicles else self.conf.vehicular_fields
        df = pandas.read_csv(os.path.join('data', self.conf.place_name, name))
        fields = conf_fields.list()
        for d_record in df[fields].to_dict('records'):
            name = f"{d_record[conf_fields.sensor_name]}"
            timestamp = d_record[conf_fields.time]
            no = d_record[conf_fields.count]
            if name not in self.sensor_name_to_id:
                self.sensor_name_to_id[name] = len(self.sensor_name_to_id)
            if isVehicles:
                self.ts_sensor_name_to_time_to_value[name][timestamp] = self.ts_sensor_name_to_time_to_value[name][timestamp] + no
                if timestamp not in self.datetime_conf:
                    self.datetime_conf[timestamp] = pandas.to_datetime(timestamp)
        ts = pandas.to_datetime(getattr(df, conf_fields.time))
        setattr(df, conf_fields.time, (ts - min(ts)).dt.seconds)
        met_junctions = set()
        d = defaultdict(lambda: defaultdict(int))
        ordered_timestamps = list()
        for d_record in df[fields].to_dict('records'):
            name = f"{d_record[conf_fields.sensor_name]}"
            lon = d_record[conf_fields.lon]
            lat = d_record[conf_fields.lat]
            if isVehicles:
                for junction, _ in self.query_for_nearest_junction(lat, lon):
                    self.junction_to_nearest_sensors[junction].add(name)
            timestamp = d_record[conf_fields.time]
            no = d_record[conf_fields.count]
            x, y = self.net.convertLonLat2XY(lon, lat)
            idx = self.sensor_name_to_id[name]
            if not self.conf.aggregate_edges_to_nodes:
                self.connections.add(EdgeConnectionsPerSimulationTime(timestamp, name, no))
                self.edges.add(TimedEdge(name, x, y))
                self.edges_latlon.add(GNNEdge(name, idx, lat, lon))
            else:
                if timestamp not in ordered_timestamps:
                    ordered_timestamps.append(timestamp)
                ls = self.query_for_nearest_junction(lat, lon)
                assert len(ls) == 1
                node_idx = ls[0][0]
                jlat, jlon = ls[0][1]
                node_id = self.node_id_to_name[node_idx]
                if node_idx not in met_junctions:
                    jx, jy = self.net.convertLonLat2XY(jlon, jlat)
                    self.edges.add(TimedEdge(node_id, jx, jy))
                    self.edges_latlon.add(GNNEdge(node_id, node_idx, jlat, jlon))
                    met_junctions.add(node_idx)
                d[timestamp][node_id] = d[timestamp][node_id] + no
        if self.conf.aggregate_edges_to_nodes:
            ordered_timestamps.sort()
            met_junctions = list(met_junctions)
            for timestamp in ordered_timestamps:
                for junction_id in met_junctions:
                    junction = self.node_id_to_name[junction_id]
                    self.connections.add(EdgeConnectionsPerSimulationTime(timestamp, junction, d[timestamp][junction]))

    def generate_gnn_graph(self):
        """
        This method generates the graph representation suitable for the GNN training for the traffic predictor given
        the current traffic information
        :return:
        """
        gnn_network_hop = DiGraph()
        gnn_network_mintime = DiGraph()
        gnn_network_minlen = DiGraph()
        gnn_network_hop_top_preserve = DiGraph()
        gnn_network_mintime_top_preserve = DiGraph()
        gnn_network_minlen_top_preserve = DiGraph()
        node_to_n_neighs = dict()
        for src in self.junction_to_nearest_sensors.keys():
            gnn_network_hop.add_node(src)
            gnn_network_mintime.add_node(src)
            gnn_network_minlen.add_node(src)
            gnn_network_hop_top_preserve.add_node(src)
            gnn_network_mintime_top_preserve.add_node(src)
            gnn_network_minlen_top_preserve.add_node(src)
            node_to_n_neighs[src] = self.networkG.out_degree(src)

        for src in self.junction_to_nearest_sensors.keys():
            for dst in self.junction_to_nearest_sensors.keys():
                if src != dst:
                    try :
                        hop_distance = networkx.shortest_path_length(self.networkG, src, dst, weight=None)
                        gnn_network_hop.add_edge(src, dst, weight=hop_distance)
                        gnn_network_hop_top_preserve.add_edge(src, dst, weight=hop_distance)
                    except:
                        pass
                    try:
                        time_distance = networkx.shortest_path_length(self.networkG, src, dst, weight="time_s")
                        gnn_network_mintime.add_edge(src, dst, weight=time_distance)
                        gnn_network_mintime_top_preserve.add_edge(src, dst, weight=time_distance)
                    except:
                        pass
                    try:
                        len_distance = networkx.shortest_path_length(self.networkG, src, dst, weight="distance_m")
                        gnn_network_minlen.add_edge(src, dst, weight=len_distance)
                        gnn_network_minlen_top_preserve.add_edge(src, dst, weight=len_distance)
                    except:
                        pass

        for src in self.junction_to_nearest_sensors.keys():
            deg = self.networkG.out_degree(src)
            gnn_network_hop_top_preserve = prune_graph_node_with_outdegree(gnn_network_hop_top_preserve, src, deg)
            gnn_network_mintime_top_preserve = prune_graph_node_with_outdegree(gnn_network_mintime_top_preserve, src, deg)
            gnn_network_minlen_top_preserve = prune_graph_node_with_outdegree(gnn_network_minlen_top_preserve, src, deg)

        networkx.to_pandas_adjacency(gnn_network_hop).to_csv(os.path.join("data", self.conf.place_name,"gnn_traffic", "gnn_network_hop.csv"))
        networkx.to_pandas_adjacency(gnn_network_hop_top_preserve).to_csv(os.path.join("data", self.conf.place_name,"gnn_traffic", "gnn_network_hop_top_preserve.csv"))
        networkx.to_pandas_adjacency(gnn_network_mintime).to_csv(os.path.join("data", self.conf.place_name,"gnn_traffic", "gnn_network_mintime.csv"))
        networkx.to_pandas_adjacency(gnn_network_mintime_top_preserve).to_csv(os.path.join("data", self.conf.place_name,"gnn_traffic", "gnn_network_mintime_top_preserve.csv"))
        networkx.to_pandas_adjacency(gnn_network_minlen).to_csv(os.path.join("data", self.conf.place_name,"gnn_traffic", "gnn_network_minlen.csv"))
        networkx.to_pandas_adjacency(gnn_network_minlen_top_preserve).to_csv(os.path.join("data", self.conf.place_name,"gnn_traffic", "gnn_network_minlen_top_preserve.csv"))

    def _serialize_connection_counting_for_gnn(self):
        df = list()
        sensor_to_junction = defaultdict(set)
        sensor_name_to_id = dict()
        for k, ls in self.junction_to_nearest_sensors.items():
            for x in ls:
                if x not in sensor_name_to_id:
                    sensor_name_to_id[x] = len(sensor_name_to_id)
                sensor_to_junction[x].add(k)
        S = defaultdict(int)
        for sensor_name in self.ts_sensor_name_to_time_to_value:
            # id = self.sensor_name_to_id[sensor_name]
            pairs = list(self.ts_sensor_name_to_time_to_value[sensor_name].items())
            pairs.sort(key=lambda x: self.datetime_conf[x[0]])
            if len(pairs)>1:
                for previous, current in zip(pairs, pairs[1:]):
                    prev_time = previous[0]
                    value = previous[1]
                    next_time = current[0]
                    for junction in sensor_to_junction[sensor_name]:
                        S[(junction, prev_time, next_time)] =  S[(junction, prev_time, next_time)] + value
        for (junction, prev_time, next_time), value in S.items():
                df.append({"station_id":junction,"station_name":f"{junction}","time_from":prev_time,"time_to":next_time,"volume":value})
        pandas.DataFrame(df).to_csv(os.path.join("data", self.conf.place_name,"gnn_traffic", "gnn_connections.csv"), index=False)

    def serialize_configuration_to_disk(self):
        simulator_orchestrator_output = os.path.join(self.conf.place_name, "clean_example", "1_newdft_input")
        Path(simulator_orchestrator_output).mkdir(parents=True, exist_ok=True)
        from DyMMP.dataintergration.JavaClasses import TimedEdge, EdgeConnectionsPerSimulationTime
        print("1. Generating Orchestrator Configuration")
        print(" * Serializing the IoT edges")
        self._serialize_edges(self.edges, TimedEdge, os.path.join(simulator_orchestrator_output, "rsu.csv"))
        print(" * Connections for (possible) connection counting ")
        self._serialize_edges(self.connections, EdgeConnectionsPerSimulationTime, os.path.join(simulator_orchestrator_output, "connectionPerSimTime.csv"))

        print("2. Serializing the Graph for the planning algorithm (assuming chevvy cars... to be generalized)")
        networkx.write_gexf(self.networkG, os.path.join("data", self.conf.place_name, "planner_graph.gexf"))

        print("3. Starting the dumping informations for the GNN traffic prediction: ")
        print("  * Serializing the edges (GNN)")
        self._serialize_edges(self.edges_latlon, GNNEdge, os.path.join("data", self.conf.place_name, "traffic_stations.csv"))
        print("  * Serializing the connections in GNN format")
        self._serialize_connection_counting_for_gnn()
        print("  * Serializing Network Connctions")
        self.generate_gnn_graph()




    def _serialize_edges(self, data, clazz, filename):
        if not os.path.isfile(filename):
            ls = sorted(data)
            dataclasses_to_csv(filename, ls, clazz)

    def get_place_name(self):
        return self.conf.place_name

    def get_gnn_folder(self):
        return os.path.join("data", self.conf.place_name,"gnn_traffic")