__author__ = "Giacomo Bergami"
__copyright__ = "Copyright 2024, SimulatorBridger"
__credits__ = ["Giacomo Bergami",
                    "https://github.com/IshaanAdarsh/MedStream-Analytics/blob/main/digital-twin/Digital-twin-juyp/GE.ipynb"]
__license__ = "GPL"
__version__ = "1.0.1"
__maintainer__ = "Giacomo Bergami"
__email__ = "bergamigiacomo@gmail.com"
__status__ = "Production"

# 
import json
from typing import List

from marshmallow_dataclass import dataclass

@dataclass
class PatientConfiguration:
    seed: int
    x: float
    y: float
    HR_MU: float
    HR_SD: float
    SBP_MU: float
    SBP_SD:float
    TEMP_MU:float
    TEMP_SD:float

@dataclass
class PatientGenerator:
    patients: List[PatientConfiguration]
    granularity: float
    boolean_risk: bool

class PatientDigitalTwin:
    def __init__(self, idx, pc: PatientConfiguration, boolean_risk = False):
        # Initialize empty lists to store data
        self.idx = idx
        self.time_values = []
        self.boolean_risk = boolean_risk
        self.heart_rate_values = []
        self.temperature_values = []
        self.blood_pressure_values = []
        self.pc = pc
        from Orchestrator.distr.MersenneTwister import MersenneTwister
        from Orchestrator.distr.Distributions import Gaussian
        self.hr_mt = MersenneTwister(seed=self.pc.seed)
        self.hr = Gaussian(self.hr_mt, self.pc.HR_MU, self.pc.HR_SD)
        self.sbp_mt = MersenneTwister(seed=self.pc.seed+1)
        self.sbp = Gaussian(self.sbp_mt, self.pc.SBP_MU, self.pc.SBP_SD)
        self.t_mt = MersenneTwister(seed=self.pc.seed+2)
        self.t = Gaussian(self.sbp_mt, self.pc.TEMP_MU, self.pc.TEMP_SD)

    def get_random_heart_rate(self):
        return int(next(self.hr))

    # Function to get random systolic blood pressure based on normal distribution
    def get_random_systolic_blood_pressure(self):
        return int(next(self.sbp))

    # Function to get random temperature based on normal distribution
    def get_random_temperature(self):
        return round(next(self.t),2)

    def getRisk(self):
        result = self.calculate_stroke_risk(self.get_random_heart_rate(),
                                          self.get_random_temperature(),
                                          self.get_random_systolic_blood_pressure())
        return {"name": f"patient_{self.idx}",
                "x": self.pc.x,
                "y": self.pc.y,
                "risk": result}

    def calculate_stroke_risk(self,heart_rate, temperature, blood_pressure):
        # Define risk factors based on provided logic
        heart_risk = 0
        temp_risk = 0
        bp_risk = 0

        # Heart Risk Logic
        if heart_rate <= 50:
            heart_risk += 1
        if heart_rate <= 40:
            heart_risk += 1
        if heart_rate >= 91:
            heart_risk += 1
        if heart_rate >= 110:
            heart_risk += 1
        if heart_rate >= 131:
            heart_risk += 1

        # Temperature Risk Logic
        if temperature <= 36:
            temp_risk += 1
        if temperature <= 35:
            temp_risk += 2
        if temperature >= 38.1:
            temp_risk += 1
        if temperature >= 39.1:
            temp_risk += 1

        # Blood Pressure Risk Logic
        if blood_pressure <= 110:
            bp_risk += 1
        if blood_pressure <= 100:
            bp_risk += 1
        if blood_pressure <= 90:
            bp_risk += 1
        if blood_pressure >= 220:
            bp_risk += 3

        # Calculate the total risk
        total_risk = heart_risk + temp_risk + bp_risk

        return total_risk>3 if self.boolean_risk else total_risk

class PatientDataGenerator:
    def __init__(self, path):
        with open(path, 'r') as f:
            self.p = PatientGenerator.Schema().load(
                json.load(f))
            self.patients = [PatientDigitalTwin(idx, x, self.p.boolean_risk) for idx, x in enumerate(self.p.patients)]
            self.time = 0
    def _next1Sec(self):
        t = self.time
        self.time += 1
        return t, ([x.getRisk() for x in self.patients])

    def __next__(self):
        t, l = None, None
        for x in range(round(self.p.granularity)):
            t, l = self._next1Sec()
        return t, l



# Press the green button in the gutter to run the script.
if __name__ == '__main__':
    # Normal vital signs information: https://medlineplus.gov/ency/article/002341.htm, https://www.researchgate.net/publication/257943370_Technical_Evaluation_of_an_E-Health_Platform
    pdg = PatientDataGenerator("/home/giacomo/PythonMedicalOrchestrator/pythonProject1/example.json")
    for x in range(20):
        print(next(pdg))

