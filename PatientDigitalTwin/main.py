__author__ = "Giacomo Bergami"
__copyright__ = "Copyright 2024, SimulatorBridger"
__credits__ = ["Giacomo Bergami"]
__license__ = "GPL"
__version__ = "1.0.1"
__maintainer__ = "Giacomo Bergami"
__email__ = "bergamigiacomo@gmail.com"
__status__ = "Production"

from Orchestrator.SimulatorBridger import SimulatorBridger
from Orchestrator.dt.Patient import PatientDataGenerator

# Press the green button in the gutter to run the script.
if __name__ == '__main__':
    ## Generate the jar within the repository folder via "mvn clean compile assembly:single"
    repository_folder = "/home/giacomo/Scaricati/SimulatorBridger-Rohin-Branch(1)/SimulatorBridger-Rohin-Branch"
    patient_configuration = "example.json"
    sb = SimulatorBridger.getInstance(repository_folder)

    # Normal vital signs information: https://medlineplus.gov/ency/article/002341.htm, https://www.researchgate.net/publication/257943370_Technical_Evaluation_of_an_E-Health_Platform
    pdg = PatientDataGenerator(patient_configuration)
    if sb.init(): # Performing the simulation only if all went well at initialisation time
        for x in range(20):
            sb.run(x, next(pdg))
        sb.stop()

