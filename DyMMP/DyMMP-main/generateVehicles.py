import random
import xml.etree.ElementTree as ET
import re


#path to SUMO rou xml file for ambulance data
def generateVehiclesScenario(scenario, edgeData, src, target, endTime, pathing, delta, fin):

    path = ""

    if scenario == "Newcastle_Urban":
        path = 'C:\\Users\\rohin\\SimulatorBridger\\SimulatorBridger\\DyMMP\\Newcastle_Urban\\Newcastle-Upon-Tyne.rou.xml'
    elif scenario == "Bologna_city_center":
        path = 'C:/Users/rohin/SimulatorBridger/SimulatorBridger/DyMMP/Bologna_city_center/bolognaringway_v1.0/bolognaAmbu.rou.xml'
    
    file = ET.parse(path)
    root = file.getroot()
    count = len(list(root));
    f = random.sample(range(0, count), int(count))
    f.sort()

    for x in reversed(f):
        root.remove(file.getroot()[x])

    if pathing == True:

        routes = []
        exclude = ["252105676"]

        for x in range(len(edgeData)):
            edges = list()
            for key in edgeData[x]:
                if key != src and key != target and not exclude.__contains__(key):
                    edges.append(key)
            routes.append(edges)

        count = 1
        time = 0

        for x in range(len(routes)):
            if time == 0:
                time = time + delta
                continue
            edges = " ".join(str(i) for i in routes[x]) 
            item = ET.SubElement(root, "trip")
            item.attrib["id"] = "ambulance_" + str(count) + "_to_" + str(target)
            item.attrib["depart"] = format(time, '.2f')
            item.attrib["fromJunction"] = str(src)
            item.attrib["toJunction"] = str(target)
            item.attrib["viaJunctions"] = edges
            item.tail = "\n"
            count = count + 1
            time = time + delta
    
    else:
         count = 1
         for i in range(1, endTime - fin):
            if i % delta == 0:
                time = i
                if time == 0:
                    time = time + delta
                    continue

                item = ET.SubElement(root, "trip")
                item.attrib["id"] = "ambulance_" + str(count) + "_to_" + str(target)
                item.attrib["depart"] = format(time, '.2f')
                item.attrib["fromJunction"] = str(src)
                item.attrib["toJunction"] = str(target)
                item.tail = "\n"
                count = count + 1


    tree = ET.ElementTree(root)
    ET.indent(tree, '   ',)
    tree.write(path)