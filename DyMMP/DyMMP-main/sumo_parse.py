

import dataclasses
import math
import os.path
import subprocess

from lxml import etree
from scipy.spatial import KDTree

from DyMMP.dataintergration.JavaClasses import TimedEdge, EdgeConnectionsPerSimulationTime


@dataclasses.dataclass
class SumoConfiguration:
    trace_file : str
    sumo_program : str
    sumo_configuration_file_path : str
    begin: int
    end: int
    step_length: int
    sumo_network_file: str
    default_rsu_communication_radius: int = 20
    default_max_vehicle_communication: int = 20

if __name__ == "__main__":
    concreteConf = SumoConfiguration("data/trace_file.xml", "/usr/bin/sumo",
                                     "/home/giacomo/Scaricati/bolognaringway_1.0/bolognaringway.sumo.cfg", 0, 200, 1,
                                     "/home/giacomo/Scaricati/bolognaringway_1.0/bolognaringway.net.xml")

    if not os.path.isfile(concreteConf.trace_file):
        subprocess.run([concreteConf.sumo_program, "-c", concreteConf.sumo_configuration_file_path, "--begin", str(concreteConf.begin), "--end", str(concreteConf.end), "--step-length", str(concreteConf.step_length), "--fcd-output", concreteConf.trace_file])
    rsu = set()



    with open(concreteConf.sumo_network_file) as f:
        tree = etree.parse(f)
        result = tree.xpath("/net/junction[@type='traffic_light']")
        for tl in result:
            curr = tl.attrib
            x = float(curr["x"])
            y = float(curr["y"])
            rsu.add(TimedEdge(curr["id"], x, y, 0, concreteConf.default_rsu_communication_radius, concreteConf.default_max_vehicle_communication))


    conncetions = []
    with open(concreteConf.trace_file) as f:
        tree = etree.parse(f)
        result = tree.xpath("/fcd-export/timestep")
        S = []
        for trace in result:
            curr = trace.attrib
            currTime = float(curr["time"])
            for veh in trace.getchildren():
                attrs = veh.attrib
                x = float(attrs["x"])
                y = float(attrs["y"])
                id = attrs["id"]
                S.append((x, y))
            kd_tree = KDTree(S)
            for x in rsu:
                n = len(kd_tree.query_ball_point((x.x, x.y), math.nextafter(x.communication_radius,1)))
                conncetions.append(EdgeConnectionsPerSimulationTime(currTime, x.id, n))
                


