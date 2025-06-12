import xml.etree.ElementTree as ET
import re

from importNewVehicles import vehInfo

netFile = 'C:/Users/rohin/SimulatorBridger/SimulatorBridger/DyMMP/Bologna_city_center/bolognaringway_v1.0/bolognaringway.net.xml'

file = ET.parse(netFile)
root = file.getroot()
count = len(list(root));

def getLaneFromEdge(nextEdge):
    lanes = [];
    for i in range(0, count, 1):
        thisTag = root[i].tag
        if thisTag == "edge":
            numLanes = len(list(root[i]));
            thisedge = ET.tostring(root[i]).decode("utf-8")
            edgeID_search = re.search('id="(.+?)" ', thisedge).group(1)
            if(edgeID_search == str(nextEdge)):
                for j in range(0, numLanes, 1):
                    thislane = ET.tostring(root[i][j]).decode("utf-8")
                    laneID_search = re.search('id="(.+?)" ', thislane).group(1)
                    lanes.append(laneID_search)
    return lanes

def getJunctionFromLane(currentLanes):
    junctions = [];
    for i in range(0, count, 1):
        thisTag = root[i].tag
        if thisTag == "junction":
            thisjunction = ET.tostring(root[i]).decode("utf-8")
            junctiontype_search = re.search('type="(.+?)" ', thisjunction).group(1)
            if(junctiontype_search == "traffic_light"): 
                incLanes_search = re.search('incLanes="(.+?)" ', thisjunction).group(1)
                incLanes_search = incLanes_search.split(" ");
                numLanes = len(list(incLanes_search))
                for edgeLanes in currentLanes:
                    print(edgeLanes)
                    for junctionLanes in incLanes_search:
                        if (edgeLanes == junctionLanes and len(junctions) == 0):
                            junctions.append(re.search('id="(.+?)" ', thisjunction).group(1))
    return junctions                       

    
#Link to article: https://medium.com/@devonfazekas/simulating-dynamic-vehicular-detours-based-on-edge-travel-time-in-sumo-e57a50457dba
import os
import sys
if 'SUMO_HOME' in os.environ:
    sys.path.append(os.path.join(os.environ['SUMO_HOME'], 'tools'))
    
from sumolib import checkBinary
import traci

from math import floor, dist

sumoBinary = checkBinary('sumo-gui')

RED = [255, 0, 0]
GREEN = [0, 255, 0]
BLUE = [0, 0, 255]

VEHICLES = []

default_weight = 100
EDGE_ID = [] 
EDGE_WEIGHTS = {}
route = tuple;
currentVehicleInfo = list
lastTime = {}

def rerouteVehs(weights, stoppingPoint, delta, destination, weightType):
   
    destPos = traci.junction.getPosition(destination)

    if len(weights) != 0 and len(EDGE_ID) == 0:
        for key in weights:
            edge= str(key)
            edge = edge.split("Edge_", 1)[1]
            EDGE_ID.append(edge)

    vehData = []
    i = 1
    i = traci.simulation.getTime()*1000 + i

    while traci.simulation.getTime() <= stoppingPoint:

        for veh in traci.simulation.getDepartedIDList():
            VEHICLES.append(veh)

        for veh in traci.simulation.getArrivedIDList():
            VEHICLES.remove(veh)

        for vehId in VEHICLES:

            if str(vehId).__contains__("ambulance"):
                
                setVehColor(vehId, RED)

                if (traci.simulation.getTime() - 1) % delta == 0:
                    route = traci.vehicle.getRoute(vehId)
                    routeID = traci.vehicle.getRouteIndex(vehId)
                    pos = route[routeID]
                    updateEdgeWeights(vehId, route[routeID+1:len(route)-1], weights, weightType, pos, destPos)
                

                if (traci.simulation.getTime() + 1) % delta == 0:
                    pos = traci.vehicle.getPosition(vehId)
                    vehData.append(vehInfo(vehId, pos[0], pos[1], traci.simulation.getTime() + 1))

        traci.simulationStep()
    return vehData
        
def stopSim():
    traci.close()        

def startSim(scenario):
    """Starts the simulation."""
    print("Starting SUMO Simulation!")
    if scenario == "Bologna_city_center":
        traci.start(
            [
                sumoBinary,
                '--net-file', 'C:/Users/rohin/SimulatorBridger/SimulatorBridger/DyMMP/Bologna_city_center/bolognaringway_v1.0/bolognaringway.net.xml', 
                '--route-files', 'C:/Users/rohin/SimulatorBridger/SimulatorBridger/DyMMP/Bologna_city_center/bolognaringway_v1.0/bolognaringway.rou.xml,C:\\Users\\rohin\\SimulatorBridger\\SimulatorBridger\\DyMMP\\Bologna_city_center\\bolognaringway_v1.0\\bolognaAmbu.rou.xml',
                '--junction-taz','true',
                '--delay', '0',
                '--device.battery.probability', '1.0',
                '--device.battery.track-fuel', 'true',
                '--device.stationfinder.rescueAction', 'none',
                '--quit-on-end', 'true',
                '--gui-settings-file', 'C:/Users/rohin/SimulatorBridger/SimulatorBridger/DyMMP/Bologna_city_center/bolognaringway_v1.0/viewSettings.xml',
                '--start',
                '--fcd-output', 'C:/Users/rohin/SimulatorBridger/SimulatorBridger/DyMMP/DyMMP-main/fcdoutput.xml',
                '--battery-output', 'C:/Users/rohin/SimulatorBridger/SimulatorBridger/DyMMP/DyMMP-main/batteryoutput.xml'
            ]
            )
    if scenario == "Newcastle_Urban":
        traci.start(
            [
                sumoBinary,
                '--net-file', 'C:\\Users\\rohin\\SimulatorBridger\\SimulatorBridger\\DyMMP\\Newcastle_Urban\\Newcastle-Upon-Tyne.net.xml',
                '--route-files', 'C:\\Users\\rohin\\SimulatorBridger\\SimulatorBridger\\DyMMP\\Newcastle_Urban\\Newcastle-Upon-Tyne.rou.xml',
                '--junction-taz','true',
                '--delay', '0',
                '--quit-on-end', 'true',
                '--gui-settings-file', 'C:/Users/rohin/SimulatorBridger/SimulatorBridger/DyMMP/Bologna_city_center/bolognaringway_v1.0/viewSettings.xml',
                '--start',
                '--fcd-output', 'C:/Users/rohin/SimulatorBridger/SimulatorBridger/DyMMP/DyMMP-main/fcdoutput.xml'
            ]
            )

def addVehicle(route, i, time):
    print(route[0][0])
    print(route[0][len(route[0])-1])
    traci.vehicle.add(vehID="ambulance_"+str(i), depart=str(time), fromTaz="269336256", toTaz="703667413", routeID="")

def shouldContinueSim():
    """Checks that the simulation should continue running.
    Returns:
        bool: `True` if vehicles exist on network. `False` otherwise.
    """
    numVehicles = traci.simulation.getMinExpectedNumber()
    return True if numVehicles > 0 else False


def setVehColor(vehId, color):
    """Changes a vehicle's color.
    Args:
        vehId (String): The vehicle to color.
        color ([Int, Int, Int]): The RGB color to apply.
    """
    traci.vehicle.setColor(vehId, color)


def updateEdgeWeights(vehId, route, weights, weightType, pos, destPos):
       
    for edge in route:
        if traci.vehicle.getAdaptedTraveltime(vehId, traci.simulation.getTime(), edge) < 0:
            traci.vehicle.setAdaptedTraveltime(vehId, edge, 1)


    if weightType == "local": #weights updated per ambulance

        for junction in EDGE_ID:
            
            edges = traci.junction.getIncomingEdges(junction)
            junctPos = traci.junction.getPosition(junction)
            distance = dist(junctPos, destPos)
            weight = (weights["Edge_"+str(junction)])
        
            for indedge in edges:

                traci.vehicle.setAdaptedTraveltime(vehId, indedge, distance)
                thisEdge = traci.vehicle.getAdaptedTraveltime(vehId, traci.simulation.getTime(), pos) + traci.vehicle.getWaitingTime(vehId)
                traci.vehicle.setAdaptedTraveltime(vehId, pos, thisEdge)

                startdist = traci.vehicle.getDistance(vehId)
                if traci.vehicle.getDistance(vehId) == 0:
                    startdist = 0.001

                travelTime = traci.vehicle.getAdaptedTraveltime(vehId, traci.simulation.getTime(), indedge) * (distance / startdist) * weight
                traci.vehicle.setAdaptedTraveltime(vehId, indedge, travelTime)

            traci.vehicle.rerouteTraveltime(vehId)

    elif weightType == "global":#weights updated across all ambulances

        for junction in EDGE_ID:

            edges = traci.junction.getIncomingEdges(junction)
            junctPos = traci.junction.getPosition(junction)
            distance = dist(junctPos, destPos)
            vehPosition = traci.vehicle.getPosition(vehId)
            posDist = dist(junctPos, vehPosition)
            weight = (weights["Edge_"+str(junction)])

            for indedge in edges:

                if indedge not in EDGE_WEIGHTS:
                    EDGE_WEIGHTS.update({indedge: distance})

                if pos not in EDGE_WEIGHTS:
                    EDGE_WEIGHTS.update({pos: posDist})

                if indedge not in lastTime:
                    lastTime.update({indedge: weight})

                if weight == 0:
                    extraWeight = 0
                elif weight >= 1 and weight < 5:
                    extraWeight = 0.5
                elif weight >= 10 and weight < 20:
                    extraWeight = 2
                    weight = (weight + lastTime[indedge]) /2
                elif weight >= 20 and weight < 30:
                    extraWeight = 3
                    weight = (weight + lastTime[indedge]) /2
                elif weight >= 30:
                    extraWeight = 4
                    weight = (weight + lastTime[indedge]) /2
                else:
                    extraWeight = 1

                traci.vehicle.setAdaptedTraveltime(vehId, indedge, distance)
                thisEdge = traci.vehicle.getAdaptedTraveltime(vehId, traci.simulation.getTime(), pos) + traci.vehicle.getWaitingTime(vehId)
                EDGE_WEIGHTS[pos] = (EDGE_WEIGHTS[pos] + thisEdge) / 2
                traci.vehicle.setAdaptedTraveltime(vehId, pos, thisEdge)

                startdist = traci.vehicle.getDistance(vehId)
                if traci.vehicle.getDistance(vehId) == 0:
                    startdist = 0.001
                    
                travelTime = traci.vehicle.getAdaptedTraveltime(vehId, traci.simulation.getTime(), indedge) * (distance / startdist) * weight
                EDGE_WEIGHTS[indedge] = (EDGE_WEIGHTS[indedge] + travelTime)/2 * extraWeight
                traci.vehicle.setAdaptedTraveltime(vehId, indedge, EDGE_WEIGHTS[indedge])
                lastTime[indedge] = weight

            traci.vehicle.rerouteTraveltime(vehId)

def getOurDeparted(filterIds=[]):
    """Returns a set of filtered vehicle IDs that departed onto the network during this simulation step.
    Args:
        filterIds ([String]): The set of vehicle IDs to filter for.
    Returns:
        [String]: A set of vehicle IDs.
    """
    newlyDepartedIds = traci.simulation.getDepartedIDList()
    filteredDepartedIds = newlyDepartedIds if len(
        filterIds) == 0 else set(newlyDepartedIds).intersection(filterIds)
    return filteredDepartedIds

# if __name__ == "__main__":
#     main()