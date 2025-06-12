import dataclasses
import os
import subprocess
from typing import List, Optional

import networkx
import overpy

import osmnx as ox

def get_all_streets(input):
    import geopandas as gpd
    G = ox.project_graph(input)
    nodes = G.nodes()
    connections = {}

    for n in nodes:
        connections[n] = set([])
        for nbr in networkx.neighbors(G, n):
            for d in G.get_edge_data(n, nbr).values():
                if 'name' in d:
                    if type(d['name']) == str:
                        connections[n].add(d['name'])
                    elif type(d['name']) == list:
                        for name in d['name']:
                            connections[n].add(name)
                    else:
                        connections[n].add(None)
                else:
                    connections[n].add(None)

import sumolib

@dataclasses.dataclass
class Sumo:
    ignore_links: List[str]
    allowed: List[str]
    binary: Optional[str]
    config: Optional[str]
    net:    Optional[str]
    begin: int = -1
    end: int = -1
    step: int = -1
    default_rsu_communication_radius: float = 20.0
    default_max_vehicle_communication: float = 10.0


def city_map(place_name = None, netconvert="/usr/bin/netconvert")->sumolib.net.Net:
    if place_name is None:
        place_name = "Newcastle Upon Tyne"  # Replace with your desired country or city name
    ox.settings.all_oneway = True
    file = os.path.join("data", "newcastle_upon_tyne.osm.xml")
    file_sumo = os.path.join("data", "newcastle_upon_tyne.net.xml")
    # file_parking = os.path.join("data", "newcastle_upon_tyne.parking.xml")
    if not os.path.exists(file_sumo):
        if not os.path.isfile(file):
            graph = ox.graph_from_place(place_name, network_type='all', simplify=False)
            # get_all_streets(graph)
            ox.io.save_graph_xml(graph, filepath=file)
        subprocess.run([netconvert, "--osm", file, "-o", file_sumo,  "--geometry.remove", "--ramps.guess", "--junctions.join", "--tls.guess-signals", "--tls.discard-simple", "--tls.join", "--tls.default-type", "actuated"])
    net = sumolib.net.readNet(file_sumo)
    return net