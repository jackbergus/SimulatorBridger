import xml.etree.ElementTree as ET
import re
from math import ceil

class vehInfo:
    def __init__(self, name, x,y, timestamp):
        self.name = name
        self.x = x
        self.y = y
        self.timestamp = timestamp


#print(count)
path = 'C:\\Users\\rohin\\SimulatorBridger\\SimulatorBridger\\DyMMP\\DyMMP-main\\fcdoutput.xml'
file = ET.parse(path)
root = file.getroot()
timesDone = []

def collectNewVehInfo(currentTime):

    root = file.getroot()
    count = len(list(root));
    currentTime = ceil(currentTime)
    allNewVehInfo = []
    for i in range(0, count, 1):
        temp1 = ET.tostring(root[i]).decode("utf-8")
        timestamp = re.search('time="(.+?)"', str(temp1)).group(1)

        
        # print("time = " + timestamp)
        # print("current time = " + str(currentTime))
        if float(timestamp) > currentTime:
            break
        if (float(timestamp) == currentTime):
            vehs = list(root[i])
            for veh in vehs:
                temp2 = ET.tostring(veh).decode("utf-8")
                #print(temp2)
                name = re.search('id="(.+?)"', str(temp2)).group(1)
                #print("id = " + str(name))
                x = re.search('x="(.+?)"', str(temp2)).group(1)
                #print("x = " + str(x))
                y = re.search('y="(.+?)"', str(temp2)).group(1)
                #print("y = " + str(y))
                newVehicle = vehInfo(name, x, y, timestamp)
                allNewVehInfo.append(newVehicle)
    return allNewVehInfo