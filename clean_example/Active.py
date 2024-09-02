#For Large XML Files
import numpy as np
import math
from lxml import etree
import sys
import os

arr = sys.argv[1].split(',')
arr = np.array([float(i) for i in arr])
larr = int(len(arr) / 2)
rad = int(sys.argv[2])
print("Communication radius = " + str(rad))

directory = os.path.dirname(__file__)
trace = directory + "\\1_sumo_output\\sumo_trace_TAVF.xml"
newTrace = directory + "\\1_sumo_output\\active_sumo_trace_TAVF.xml"

def willTransmit(veh_x,veh_y, arr, larr):
        for i in range(larr):
            if(math.dist([veh_x,veh_y], [arr[2*i], arr[2*i+1]]) <= rad):
                return True
            
with open(newTrace, 'wb') as file:

    file.write(b'<fcd-export>')

    context = etree.iterparse(trace, events=("start", "end"))
    context = iter(context)
    event, root = context.__next__()
    for event, elem in context:
        if (event == "end" and elem.tag == "timestep"):
            for vehicle in elem:
                veh_x = float(vehicle.attrib['x'])
                veh_y = float(vehicle.attrib['y'])
                if not (willTransmit(veh_x, veh_y, arr, larr)):
                    elem.remove(vehicle)
            file.write(etree.tostring(elem, encoding='UTF-8', xml_declaration=False))
             
    file.write(b'</fcd-export>')


'''For small XML files
from re import M
import numpy as np
import math
from lxml import etree
import sys
import os

arr = sys.argv[1].split(',')
arr = np.array([float(i) for i in arr])
larr = int(len(arr) / 2)
rad = int(sys.argv[2])
print("Communication radius = " + str(rad))

directory = os.path.dirname(__file__)
trace = directory + "\\1_sumo_output\\sumo_trace_TAVF.xml"
newTrace = directory + "\\1_sumo_output\\active_sumo_trace_TAVF.xml"
file = etree.parse(open(trace, 'r'))

def willTransmit(veh_x,veh_y, arr, larr):
    for i in range(larr):
        if(math.dist([veh_x,veh_y], [arr[2*i], arr[2*i+1]]) <= rad):
            return True
  
for vehicle in file.xpath("//vehicle"):
    veh_x = float(vehicle.attrib['x'])
    veh_y = float(vehicle.attrib['y'])
    if(willTransmit(veh_x, veh_y, arr, larr)):
        continue
    vehicle.getparent().remove(vehicle)

file.write(newTrace, encoding='UTF-8', xml_declaration=True)'''