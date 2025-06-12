import csv
from dataclasses import fields, asdict
import re


def urlify(s):

    # Remove all non-word characters (everything except numbers and letters)
    s = re.sub(r"[^\w\s]", '', s)

    # Replace all runs of whitespace with a single dash
    s = re.sub(r"\s+", '-', s)

    return s

def dataclasses_to_csv(filename, ls:list, dataclazz):
    assert all(map(lambda x: isinstance(x, dataclazz), ls))
    with open(filename, 'w') as f:
        flds = [fld.name for fld in fields(dataclazz)]
        w = csv.DictWriter(f, flds)
        w.writeheader()
        w.writerows([asdict(prop) for prop in ls])


import requests
from lxml.html import fromstring

def get_distance_in_meters(lat1, lon1, lat2, lon2):
    import geopy.distance
    coords_1 = (lat1, lon1)
    coords_2 = (lat2, lon2)
    return (geopy.distance.geodesic(coords_1, coords_2).m)


## Rotating the HTTP requests while performing the scraping: you might get a close on trying to get multiple elements to analyse
def get_proxies(url = 'https://www.sslproxies.org/'):
    response = requests.get(url)
    parser = fromstring(response.text)
    proxies = set()
    for i in parser.xpath('//tbody/tr')[:10]:
        # Grabbing IP and corresponding PORT
        proxy = ":".join([i.xpath('.//td[1]/text()')[0], i.xpath('.//td[2]/text()')[0]])
        proxies.add(proxy)
    return proxies




def serialize(call_graph, file_path):
    import networkx
    import jsonpickle
    from networkx.readwrite import json_graph
    '''Function to serialize a NetworkX DiGraph to a JSON file.'''
    if not isinstance(call_graph, networkx.DiGraph):
        raise Exception('call_graph has be an instance of networkx.DiGraph')

    with open(file_path, 'w+') as _file:
        _file.write(jsonpickle.encode(
            json_graph.adjacency_data(call_graph))
        )

def deserialize(file_path):
    import networkx
    import jsonpickle
    from networkx.readwrite import json_graph
    '''Function to deserialize a NetworkX DiGraph from a JSON file.'''
    call_graph = None
    with open(file_path, 'r+') as _file:
        call_graph = json_graph.adjacency_graph(
            jsonpickle.decode(_file.read()),
            directed=True
        )
    return call_graph