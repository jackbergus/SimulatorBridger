from chargingstation import ChargingStation
from ElectricVehicles import ElectricVehiclesChargingModel, ElectricVehicleChargingConf

import numpy as np

####################################################################
### Initialization
####################################################################
conf = ElectricVehicleChargingConf()
ev1 = ElectricVehiclesChargingModel("genoveffo", conf, departure_time=1023*60*60, vehicle_type='bev', arrival_time=1*60, initial_soc=0.1, target_soc=0.75, batterycapacity_kWh = 120.0)
evse_instance = ChargingStation(efficiency=0.99, Prated_kW=6.6, evse_id=1)
# evse_instance.server_setpoint = 10



t0 = 0
tf = 1023.1*60*60
dt = 1

Pmax = 0.0
ev1.assign_evse(evse_instance.evse_id)

####################################################################
### Start simulation
####################################################################
for t in np.arange(t0, tf, dt):

    ev1.chargevehicle(t, dt, evse_instance)
    evse_instance.run_protocol(ev1)

    print('t:{0}, soc:{1}, plugged:{2}, Pmax [kW]:{3}, Pevse from grid [W]:{4}, Pevse to EV [W]:{5}'.format(
           t, ev1.soc, ev1.pluggedin, Pmax, evse_instance.ev_power/evse_instance.efficiency, evse_instance.ev_power))
        