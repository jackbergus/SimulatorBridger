import dataclasses
from typing import Optional


class ChargingStation():
    def __init__(self, efficiency, Prated_kW, evse_id, server_setpoint=10.0):
        """
        :param efficiency: Efficiency of the EVSE as a fraction between 0 and 1 [reqd.]
        :param Prated_kW: Rated power capacity of the EVSE in kW [reqd.] The EVSE model is also
                           compatible for smart charge management application. The power dispensed by
                           the EVSE can be controlled by setting: evse_instance.server_setpoint = 5.0
        :param evse_id: A numerical EVSE ID [reqd.]
        """
        self.efficiency = efficiency
        self.Prated_kW  = Prated_kW
        self.evse_id   = evse_id

        self.ev_voltage = 0.0
        self.ev_power   = 0.0
        self.ev_soc     = 0.0
        self.ev_plugged = False
        self.state      = 'A'
        self.server_setpoint = server_setpoint if (server_setpoint is not None) and (server_setpoint>0.0) else Prated_kW
        self.Pmax = 0.0
        self.locked = None

    def setChargingStationAsLocked(self, ev1):
        if self.locked is None:
            self.locked = ev1.name
            return self.evse_id
        else:
            return None

    def unlockChargingStation(self):
        self.locked = None
       
    def receive_from_ev(self, ev1):
        if self.locked == ev1.name:
            Vbatt, Pbatt_kW, soc, plugged, ready = ev1.packvoltage, ev1.packpower, ev1.soc, ev1.pluggedin, ev1.readytocharge
            ### receive Vbatt, Pbatt, SOC, plugged via TCP or something if there needs a connection
            self.ev_voltage = Vbatt
            self.ev_power = Pbatt_kW
            self.ev_soc = soc
            self.ev_plugged = plugged
            self.ev_ready = ready

            if self.ev_plugged and self.ev_power < 0.1:
                self.state = 'B'
            if self.ev_plugged and self.ev_power >= 0.1:
                self.state = 'C'
            if not self.ev_plugged:
                self.state = 'A'
            return True
        else:
            return False


    def send_to_ev(self):
        if self.locked is not None:
            if self.ev_ready:
                Pmax = min(self.server_setpoint, self.Prated_kW)*self.efficiency
            else:
                Pmax = 0.0

            ### send Pmax via TCP or something if there needs a connection
            return Pmax
        else:
            return 0.0


    # def receive_from_server(self, setpoint_kW):
    #     self.server_setpoint = setpoint_kW
    # def send_to_server(self):
    #     Vbatt    = self.ev_voltage
    #     Pbatt_kW = self.ev_power
    #     soc      = self.ev_soc
    #
    #     return [Vbatt, Pbatt_kW, soc]

    def pre_transfer(self):
        return self.Pmax

    def run_protocol(evse_instance, ev1):
        if evse_instance.locked == ev1.name:
            ### EV -> EVSE
            assert evse_instance.receive_from_ev(ev1)

            ### EVSE -> EV
            evse_instance.Pmax = evse_instance.send_to_ev()
            return True
        else:
            return False

@dataclasses.dataclass(frozen=True, eq=True)
class ChargingStationConf:
    evse_id: int
    Prated_kW: float
    lat: int
    lon: int
    server_setpoint: Optional[float] = -1.0
    efficiency: Optional[float] = 0.99

    def asChargingStation(self):
        return ChargingStation(self.efficiency, self.Prated_kW, self.evse_id, self.server_setpoint)
