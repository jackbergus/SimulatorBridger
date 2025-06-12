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

import jpype # pip install jpype1
import jpype.imports
from jpype.types import *

class SimulatorBridger:
   __instance = None
   @staticmethod
   def getInstance(path):
      print("Path = " + path)
      """ Static access method. """
      if SimulatorBridger.__instance == None:
         SimulatorBridger(path)
      return SimulatorBridger.__instance

   def new_patient(self, name, x, y, timestamp):
       from uk.ncl.giacomobergami.utils.shared_data.iot import TimedIoT
       TI = TimedIoT()
       TI.setId(jpype.java.lang.String(str(name)))
       TI.setX(x)
       TI.setY(y)
       TI.setAngle(0)
       TI.setType(jpype.java.lang.String(str("patient")))
       TI.setSpeed(0.0)
       TI.setPos(0.0)
       TI.setLane(jpype.java.lang.String(str("")))
       TI.setSlope(0.0)
       TI.setSimtime(timestamp)
       TI.setInjected(True)
       return TI

   def init(self, start=0, deltaTime=None, loopDuration=10):
       ## TODO: EdgeList
        #583.69,854.5,           09
        #323.63,227.57,          12
        #977.75,829.69,          52
        #768.63,816.34,          53
        #1425.56,1082.94,        78
        #1470.88,1073.94,        44
        #778.0,613.44,           82
        #1504.51,825.41,         1c
        #1573.69,569.0,          27
        #798.63,778.69,          15
        #1489.07,825.69,         03
        #1579.38,473.19,         34
        #874.28,625.54,          63
        #1485.04,841.29,         1b
        #1498.87,847.16,         01
        #1457.38,1043.5,       204c

       self.this.init(start, jpype.java.util.ArrayList())
       if deltaTime is None:
           self.deltaTime = self.this.getDeltaTime()
       else:
           self.deltaTime = deltaTime
       self.this.loopDuration = loopDuration
       self.running = True

   def run(self, time, patients=None):
       print(self.this.getEnergies())
       p = jpype.java.util.ArrayList()
       forDelta = time
       if patients is not None:
         for x in patients:
             time = min(time, float(x["timestamp"]))
             forDelta = max(forDelta, float(x["timestamp"]))
             p.add(self.new_patient(x["name"], x["x"], x["y"], float(x["timestamp"])))
       forDelta -= time
       forDelta = math.nextafter(forDelta, math.inf)
       forDelta = max(forDelta, self.deltaTime)
       self.running = self.this.run(time, forDelta, p)
       return self.running

   def stop(self):
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
          jar = "C:\\Users\\rohin\\SimulatorBridger\\SimulatorBridger\\out\\artifacts\\SimulatorBridger_jar\\SimulatorBridger.jar"
          print(jar5)
          print(jpype.getDefaultJVMPath())
          print([jar1, jar2, jar3, jar4, jar5])
          jpype.startJVM(classpath=[jar1, jar2, jar3, jar4, jar5])
          from uk.ncl.giacomobergami.SumoOsmosisBridger import SimulatorManager
          self.this = SimulatorManager()
          SimulatorBridger.__instance = self