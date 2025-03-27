import random
import xml.etree.ElementTree as ET
import sys
import re

#path to SUMO rou xml file for ambulance data
path = "C:\\Users\\rohin\\Sumo\\Bologna-Reham\\ambulances.rou.xml"

edges = sys.argv[1].split(',')

file = ET.parse(path)
root = file.getroot()
count = len(list(root));
f = random.sample(range(0, count), int(count))
f.sort()

toBegin = 0.00
fromBegin = 160.00
mapEntrance = 137
mapExit = 114
vtype = "emergency" 
end = 12000.00
vehsPerHour = 20

for x in reversed(f):
    root.remove(file.getroot()[x])

for edge in edges:
    item = ET.SubElement(root, "flow")
    item.attrib["id"] = "ambulance_to_" + str(re.sub('[^A-Za-z0-9]+', '_', edge))
    item.attrib["begin"] = str(toBegin)
    item.attrib["from"] = str(mapEntrance)
    item.attrib["toJunction"] = str(re.sub('[^A-Za-z0-9]+', '', edge))
    item.attrib["type"] = str(vtype)
    item.attrib["end"] = str(end)
    item.attrib["vehsPerHour"] = str(vehsPerHour)
    item.tail = "\n"

for edge in edges:
    item = ET.SubElement(root, "flow")
    item.attrib["id"] = "ambulance_from_" + str(re.sub('[^A-Za-z0-9]+', '_', edge))
    item.attrib["begin"] = str(fromBegin)
    item.attrib["fromJunction"] = str(re.sub('[^A-Za-z0-9]+', '', edge))
    item.attrib["to"] = str(mapExit)
    item.attrib["type"] = str(vtype)
    item.attrib["end"] = str(end)
    item.attrib["vehsPerHour"] = str(vehsPerHour)
    item.tail = "\n"

tree = ET.ElementTree(root)
ET.indent(tree, '   ',)
tree.write(path)