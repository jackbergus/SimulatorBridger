import re
import sys
import pickle

from math import floor

sys.path.append("C:\\Users\\rohin\\SimulatorBridger\\SimulatorBridger\\DyMMP\\DyMMP-main")
sys.path.append("C:\\Users\\rohin\\SimulatorBridger\\SimulatorBridger\\PatientDigitalTwin")

from DyMMP.routing.MultiDimensionalCostGraph import output
from SUMORerouting import rerouteVehs, startSim, stopSim

from SimulatorOrchestrator import SimulatorBridger
from generateVehicles import generateVehiclesScenario
from importNewVehicles import collectNewVehInfo
from sortBatteryInfo import sortBatteryInfo

beg = "src_node='"
allroutes = []

def getRoutes(allVehs):
    vehroutes = []
    for veh in allVehs:
        result  = str(veh).split()
        newroute = []
        for word in result:
            if word.__contains__(beg):
                newresult = re.search(beg +'(.*)\',', word).group(1)
                newroute.append(newresult)
                print(newresult)
        #print(newroute)
        vehroutes.append(newroute)

    #print(allroutes)
    return vehroutes

def main():
    runtype = "run"
    weightType = "none"
    scenario = "Bologna_city_center" #"Newcastle_Urban"
    injectPathing = False

    srcs =    ["332670690", "418871458", "1018788113", "1825317805", "2648102479", "252164592", "250763460", "1374484818", "269336256", "332670690"]
    targets = ["2596932281", "251147812", "33344141", "272884109", "1645754615", "251893273", "399737354", "400902911", "703667413"]

    src = srcs[8]
    target = targets[8]

    endTime = 3600
    generationDelta = 100
    stopGenDelta = 0
    numVehs = floor(endTime/generationDelta)

    if runtype == "gen":
        allVehs = output(numVehs, scenario, src, target)
        allRoutes = getRoutes(allVehs)

        with open('allroutes.pkl', 'wb') as f:
            pickle.dump(allRoutes, f) 

        exit()
        
    if runtype == "run":
        with open('allroutes.pkl', 'rb') as f:
            allRoutes = pickle.load(f)
 
        # generateVehiclesScenario(scenario, allRoutes, src, target, 3600, pathing, 150)
        # exit()
        # startSim(scenario)
        # rerouteVehs(allRoutes, scenario, allRoutes, 3600, 60)
        # stopSim()
        # exit()

        vehData = []
        vehInfo = []
        devicesPerEdge = {}
        path = "C:\\Users\\rohin\\SimulatorBridger\\SimulatorBridger\\out\\transferred"
        sb = SimulatorBridger.getInstance(path)
        generateVehiclesScenario(scenario, allRoutes, src, target, endTime, injectPathing, generationDelta, stopGenDelta)

        if True:
            sb.init()
            startSim(scenario)
            delta = sb.deltaTime
            go = delta
            print("Delta Time = " + str(go))
            while go <= endTime:
                vehInfo = rerouteVehs(devicesPerEdge, go, delta, target, weightType)
                for info in vehInfo:
                    vehData.append(info)
                loopstart, devicesPerEdge =  sb.run(go, vehInfo)
                vehInfo = []
                go = loopstart

            stopSim()
            sb.stop()
    
        print("The simulation is finished")
        sortBatteryInfo(generationDelta)
        print("The data is now all collated!")
        exit()

if __name__ == "__main__":
    main()