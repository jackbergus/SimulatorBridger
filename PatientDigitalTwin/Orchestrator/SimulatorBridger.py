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
       self.this.init(start, jpype.java.util.ArrayList())
       if deltaTime is None:
           self.deltaTime = self.this.getDeltaTime()
       else:
           self.deltaTime = deltaTime
       self.this.loopDuration = loopDuration
       self.running = True

   def run(self, time, patients=None):
       if self.running:
            self.running = self.this.run()
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
          jar1 = os.path.join(path, "SimulatorBridger-central_agent_planner","target","SimulatorBridger-central_agent_planner-1.0-SNAPSHOT-jar-with-dependencies.jar")
          jar2 = os.path.join(path, "SimulatorBridger-core","target","SimulatorBridger-core-1.0-SNAPSHOT-jar-with-dependencies.jar")
          jar3 = os.path.join(path, "SimulatorBridger-IOTSimOsmosisRES","target","SimulatorBridger-IOTSimOsmosisRES-1.0-SNAPSHOT-jar-with-dependencies.jar")
          jar4 = os.path.join(path, "SimulatorBridger-traffic_information_collector","target","SimulatorBridger-traffic_information_collector-1.0-SNAPSHOT-jar-with-dependencies.jar")
          jar5 = os.path.join(path, "SumoOsmosisBridger","target","SumoOsmosisBridger-1.0-SNAPSHOT-jar-with-dependencies.jar")
          jpype.startJVM(classpath=[jar1, jar2, jar3, jar4, jar5])
          from uk.ncl.giacomobergami.SumoOsmosisBridger import SimulatorManager
          self.this = SimulatorManager()
          SimulatorBridger.__instance = self