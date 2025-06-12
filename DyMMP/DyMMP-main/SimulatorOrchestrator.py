__author__ = "Giacomo Bergami"
__copyright__ = "Copyright 2024, SimulatorBridger"
__credits__ = ["Giacomo Bergami"]
__license__ = "GPL"
__version__ = "1.0.1"
__maintainer__ = "Giacomo Bergami"
__email__ = "bergamigiacomo@gmail.com"
__status__ = "Production"

import math
import os.path
from typing import List

import jpype # pip install jpype1 then update
import jpype.imports
from jpype.types import *

class SimulatorBridger:
    __instance = None
    @staticmethod
    def getInstance(path):
        #print("Path = " + path)
        """ Static access method. """
        if SimulatorBridger.__instance == None:
            SimulatorBridger(path)
        return SimulatorBridger.__instance

    def new_IoTDevice(self, name, x, y, timestamp):
        from uk.ncl.giacomobergami.utils.shared_data.iot import TimedIoT
        TI = TimedIoT()
        TI.setId(jpype.java.lang.String(str(name)))
        TI.setX(x)
        TI.setY(y)
        TI.setAngle(0)
        TI.setType(jpype.java.lang.String(str("ambulance")))
        TI.setSpeed(0.0) #keep
        TI.setPos(0.0) 
        TI.setLane(jpype.java.lang.String(str("")))
        TI.setSlope(0.0) #keep
        TI.setSimtime(timestamp)
        TI.setInjected(True)
        return TI

    def init(self, start=0, deltaTime=None, loopDuration=10):
        self.this.init(start, jpype.java.util.ArrayList())
        if deltaTime is None:
            self.deltaTime = self.this.getDeltaTime()
        else:
            self.deltaTime = deltaTime
        self.this.loopDuration = loopDuration
        print(self.deltaTime)
        self.running = True


    edgeEnergies = {}
    def updateEnergies(self):
        edges = []
        energies =[]
        
        if self.this.getEnergies() is not None:
                for k,v in dict(self.this.getEnergies()).items():
                    edges.append(str(k))
                    energies.append(v)
                    self.edgeEnergies.update({str(k): v})

    edgeTelemetry = {}
    def updateTelemetry(self):
        edges = []
        telemetry =[]
        
        if self.this.getTelemetry() is not None:
                for k,v in dict(self.this.getTelemetry()).items():
                    edges.append(str(k))
                    telemetry.append(v)
                    self.edgeTelemetry.update({str(k): v})


    devicesPerEdge = {}
    def updateDevicesPerEdge(self):
        edges = []
        devicesPer =[]
        
        if self.this.numberOfDevicesPerEdge() is not None:
                for k,v in dict(self.this.numberOfDevicesPerEdge()).items():
                    edges.append(str(k))
                    devicesPer.append(v)
                    self.devicesPerEdge.update({str(k): v})


    commsPerEdge = {}
    def updateCommsPerEdge(self):
        edges = []
        commPer =[]
        
        if self.this.numberOfActiveCommsPerEdge() is not None:
                for k,v in dict(self.this.numberOfActiveCommsPerEdge()).items():
                    edges.append(str(k))
                    commPer.append(v)
                    self.commsPerEdge.update({str(k): v})

    times= []
    def run(self, time, vehData):
        
        self.updateEnergies()
        self.updateDevicesPerEdge()
        self.updateCommsPerEdge()
        self.updateTelemetry()
        
        p = jpype.java.util.ArrayList()
        forDelta = time
       # print("The time is: " + str(math.floor(time)))
        if math.floor(time + 1) % self.deltaTime == 0:
            newVehs = vehData#collectNewVehInfo(time)
            if newVehs is not None and not self.times.__contains__(math.floor(time)):
                self.times.append(math.floor(time))
                #print("There are " + str(len(newVehs)) + " new vehicles")
                for veh in newVehs:
                    time = min(time, float(veh.timestamp))
                    forDelta = max(forDelta, float(veh.timestamp))
                    #print("The timestamp is: " + str(veh.timestamp))
                    p.add(self.new_IoTDevice(str(veh.name), float(veh.x), float(veh.y), float(veh.timestamp)))
                #print(p)
                        


        forDelta = math.nextafter(forDelta, math.inf)
        forDelta = max(forDelta, self.deltaTime)
        self.running = self.this.run(time, self.deltaTime, p)
        return self.running, self.devicesPerEdge

    def stop(self):
        print("Stopping Now")
        self.this.fini()
        jpype.shutdownJVM()

    def __init__(self, path):
        """ Virtually private constructor. """
        if SimulatorBridger.__instance != None:
            raise Exception("This class is a singleton!")
        else:
            jar1 = os.path.join(path, "SimulatorBridger-central_agent_planner-1.0-SNAPSHOT-jar-with-dependencies.jar")
            jar2 = os.path.join(path, "SimulatorBridger-core-1.0-SNAPSHOT-jar-with-dependencies.jar")
            jar3 = os.path.join(path, "SimulatorBridger-IOTSimOsmosisRES-1.0-SNAPSHOT-jar-with-dependencies.jar")
            jar4 = os.path.join(path, "SimulatorBridger-traffic_information_collector-1.0-SNAPSHOT-jar-with-dependencies.jar")
            jar5 = os.path.join(path, "SumoOsmosisBridger-1.0-SNAPSHOT-jar-with-dependencies.jar")
            jpype.startJVM(classpath=[jar1, jar2, jar3, jar4, jar5])
            from uk.ncl.giacomobergami.SumoOsmosisBridger import SimulatorManager
            self.this = SimulatorManager()
            SimulatorBridger.__instance = self