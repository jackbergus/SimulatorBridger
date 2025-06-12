import math
import sys
from typing import Optional, Union

import numpy
import numpy as np
import matplotlib.pyplot as plt

from DyMMP.evVanetSim.EVBattery.CSSim import ChargingStation
from DyMMP.evVanetSim.EVBattery.EVMatlab.ReImplement import SimTime, sim_delta, TimeArrow, SolutionRecord
from DyMMP.evVanetSim.EVBattery.EVMatlab.VehicularData import MobilityVehicle
from DyMMP.evVanetSim.EVBattery.PyChargeModel.ElectricVehicles import ElectricVehiclesChargingModel, ElectricVehicleChargingConf
from DyMMP.evVanetSim.EVBattery.UnitsOfMeasure import Time, Velocity
from dataclasses import dataclass
from DyMMP.evVanetSim.EVBattery.mininet_wifi.energy import BitZigBeeEnergy, BitZigBeeState


@dataclass
class DischargingCondition:
    isSOC_seconds_otherwise: bool
    value: float


@dataclass
class SimulateMoving:
    at_time: Time
    v: Velocity
    grade: float

@dataclass
class SimulateCharge:
    at_time: Time
    station: ChargingStation
    stop_condition: Optional[DischargingCondition]

@dataclass
class TransmitPacket:
    at_time: Time
    byte_size: int
    isTransmitting: bool
    amount_transmitted: int = 0
    transmission_time: float = 0.0


SimulationEvent = Union[SimulateMoving, SimulateCharge, TransmitPacket]

class VehicleDigitalTwin:
    def __init__(self, name:str, veh:MobilityVehicle, ecveh: ElectricVehicleChargingConf=None, transmission: BitZigBeeEnergy=None):
        self.vehicle = veh
        self.name = name
        self.car_condition = dict()
        self.time_arrow = dict()
        self.arrow = None
        self.time_list = list()
        self.ev1 = None
        if ecveh is None:
            ecveh = ElectricVehicleChargingConf()
        self.ecveh = ecveh
        self.log_recharging_state = dict()
        self.transmission = transmission
        self.packet_queue = []
        self.event_queue = []
        self.no_packets_handled = 0

    @property
    def soc(self):
        """Returning the vehicle's state of charge"""
        if len(self.time_list) == 0:
            return self.vehicle.init_soc
        else:
            return self.car_condition[self.time_list[-1]].battery_StateOfCharge

    @property
    def soc_empty(self):
        return (self.vehicle.drivetrain.pack.soc_empty / 100.0) * self.vehicle.init_soc

    @property
    def soc_full(self):
        return (self.vehicle.drivetrain.pack.soc_full / 100.0) * self.vehicle.init_soc

    def isOutOfBattery(self):
        """
        This is for the Digital Twin Vehicular Simulation, live: this is to check whether the vehicle's battery is dead
        :return:
        """
        if len(self.time_list) == 0:
            return False
        else:
            return self.soc <= self.soc_empty

    def isVehiclePerformingCharging(self):
        """
        This is for the Digital Twin Vehicular Simulation, live: checking whether the current vehicle is busy charging, and therefore cannot move
        :return:
        """
        return self.ev1 is not None

    def _update_with_soc(self, soc, t):
        # self.car_condition[t.seconds] = self.car_condition[self.time_list[-1]].update_with_soc(obj["soc"] * self.vehicle.init_soc)
        # self.arrow = self.time_arrow[self.time_list[-1]].update_with_soc(obj["soc"] * self.vehicle.init_soc, t)
        # self.time_arrow[t.seconds] = self.arrow
        toUpdateIdx = None
        notInArrow = True
        prev_ta = None
        prev_cond = None
        if t.seconds not in self.time_arrow:
            if len(self.time_list) == 0:
                prev_ta = TimeArrow.zero_time(t.seconds, soc)
                prev_cond = SolutionRecord.withSocVal(soc)
            else:
                toUpdateIdx = self.time_list[-1]
        else:
            toUpdateIdx = t.seconds
            notInArrow = False
        if (prev_ta is None) and (prev_cond is None):
            prev_ta = self.time_arrow[toUpdateIdx].update_with_soc(soc, t)
            prev_cond = self.car_condition[toUpdateIdx].update_with_soc(soc)
        self.car_condition[t.seconds] = prev_cond
        self.arrow = prev_ta
        self.time_arrow[t.seconds] = self.arrow
        if notInArrow:
            assert t.seconds not in self.time_list
            self.time_list.append(t.seconds)

    def _override_with_soc(self, t, condition, arrow):
        self.car_condition[t.seconds] = condition
        self.time_arrow[t.seconds] = arrow
        if t.seconds not in self.time_list:
            self.time_list.append(t.seconds)

    def _packet_scheduling_event(self, t:Time, transmit:bool, bytes_size:int, transmission_duration:float):
        v = self.vehicle.drivetrain.pack.vnom
        pmc = self.vehicle.drivetrain.pack.module.capacity
        if not self.isOutOfBattery():
            if bytes_size <= 1.0:
                new_soc = self.transmission.getTotalSOCConsumption(BitZigBeeState.IDLE, 00, v, pmc, transmission_duration)
            elif transmit:
                new_soc = self.transmission.getTotalSOCConsumption(BitZigBeeState.TX, bytes_size, v, pmc, transmission_duration)
            else:
                new_soc = self.transmission.getTotalSOCConsumption(BitZigBeeState.RX, bytes_size, v, pmc, transmission_duration)
            if new_soc > 0.0:
                soc = self.soc - new_soc
                self._update_with_soc(soc, t)
                # soc = self.soc - new_soc
                # if len(self.time_list) == 0:
                #     prev_ta = TimeArrow.zero_time(t.seconds, soc)
                #     prev_cond = SolutionRecord.withSocVal(soc)
                # else:
                #     prev_ta = self.time_arrow[self.time_list[-1]].update_with_soc(soc, t)
                #     prev_cond = self.car_condition[self.time_list[-1]].update_with_soc(soc)
                #     print(
                #         f"TR {self.time_list[-1]}: {self.car_condition[self.time_list[-1]].battery_StateOfCharge} vs. {prev_cond.battery_StateOfCharge}")
                #     assert self.car_condition[
                #                self.time_list[-1]].battery_StateOfCharge > prev_cond.battery_StateOfCharge
                # self.car_condition[t.seconds] = prev_cond
                # self.arrow = prev_ta
                # self.time_arrow[t.seconds] = self.arrow
            return True
        else:
            print(f"time: {t.seconds}, OUT OF BATTERY DETECTED!")
            return False

    def transmit_packets_in_queue(self, current_time:Time):
        """
        This is for the Digital Twin Vehicular Simulation, live: depleting the packet transmission from the queue
        :param current_time:    Current simulation time
        :return:                Goodness state of the simulation
        """
        assert self.transmission is not None
        if (len(self.packet_queue) == 0) or (self.isOutOfBattery()) or (self.isVehiclePerformingCharging()):
            print(f"No transmission happening: {current_time}")
            self._packet_scheduling_event(current_time, False, 0, 0.0)
            return False
        while len(self.packet_queue) > 0:
            assert self.packet_queue[0].at_time.seconds <= current_time.seconds
            remaining_to_send = self.packet_queue[0].byte_size - self.packet_queue[0].amount_transmitted
            remaining_transmission_time = self.transmission.time_to_send_packet(remaining_to_send)
            scheduled_transmission_time = min(remaining_transmission_time, current_time.seconds - (self.packet_queue[0].at_time.seconds +  self.packet_queue[0].transmission_time))
            to_send = int(math.floor(self.transmission.transmittable_bytes(scheduled_transmission_time, remaining_to_send)))
            transmission_time = self.transmission.time_to_send_packet(to_send)
            if (transmission_time >= sys.float_info.epsilon) and (math.floor(to_send) > 0.0) and self._packet_scheduling_event(current_time, self.packet_queue[0].isTransmitting, to_send, transmission_time):
                print(f"Transmitting {to_send} bytes for {transmission_time} seconds")
                self.packet_queue[0].transmission_time += transmission_time
                self.packet_queue[0].amount_transmitted += to_send
                if self.packet_queue[0].byte_size - self.packet_queue[0].amount_transmitted <= sys.float_info.epsilon:
                    self.packet_queue.pop(0)
                    self.no_packets_handled += 1
                    print(f"Finished to transmit Packet #{self.no_packets_handled}")
                else:
                    assert current_time.seconds - (self.packet_queue[0].at_time.seconds +  self.packet_queue[0].transmission_time) <= sys.float_info.epsilon
                    print(f"Unfinished transmission: {current_time}")
                    return True # If within this time I was only able to send a part of the packet
            else:
                print(f"Interrupted transmission: {current_time}")
                return False
        return True

    def _handle_event(self, x: SimulationEvent):
        """
        Internal function for the vehicle digital twin: scheduling a vehicle moving
        :param x:
        :return:
        """
        if x is None:
            return False
        if isinstance(x, SimulateMoving):
            return self.simulate_moving_with_velocity(x.at_time, x.v, x.grade)
        elif isinstance(x, SimulateCharge):
            return self.simulate_recharge(x.at_time, x.station, x.stop_condition)
        elif isinstance(x, TransmitPacket):
            self.packet_queue.append(x)
            return True
        assert False

    def receive_event(self, ev: SimulationEvent, force: bool = False):
        isEmpty = len(self.event_queue) == 0
        alreadyInserted = False
        if isEmpty:
            if (not isinstance(ev, TransmitPacket)) or ev.byte_size > 0:
                ## Adding a transmission event only if it is not idle
                self.event_queue.append(ev)
                alreadyInserted = True
        if (not isEmpty) or force:
            assert min(map(lambda x: x.at_time.seconds, self.event_queue)) <= ev.at_time.seconds
            if not alreadyInserted and ((not isinstance(ev, TransmitPacket)) or ev.byte_size > 0):
                self.event_queue.append(ev)
            self.event_queue.sort(key=lambda x: x.at_time.seconds)
            curr_event = self.event_queue[0].at_time if (not force) else ev.at_time
            max_time = float("-inf")
            if (ev.at_time.seconds > self.event_queue[0].at_time.seconds) or force:
                curr_event = self.event_queue[0].at_time if (not force) else ev.at_time
                while len(self.event_queue)>0:
                    if (not force) and (self.event_queue[0].at_time.seconds != curr_event.seconds):
                        break
                    curr = self.event_queue.pop(0)
                    max_time = max(curr_event.seconds, max_time)
                    self._handle_event(curr)
            self.transmit_packets_in_queue(curr_event)

    def simulate_moving_with_velocity(self, t:Time, v:Velocity, grade=0.3):
        """
        This is for the Digital Twin Vehicular Simulation, live: this simulates a live vehicle moving
        :param t:           Simulation time
        :param v:           Current velocity of the vehicle
        :param grade:       Slope grade
        :return:            Goodness condition of the action
        """
        if not self.isOutOfBattery():
            st = SimTime(t, v, grade)
            continue_, result, self.arrow = sim_delta(self.vehicle, st, self.arrow)
            if (result is not None) and (self.arrow is not None):
                # if len(self.time_list)>0:
                #     print(f"{self.time_list[-1]}: {self.car_condition[self.time_list[-1]].battery_StateOfCharge} vs. {result.battery_StateOfCharge}")
                #     assert self.car_condition[self.time_list[-1]].battery_StateOfCharge > result.battery_StateOfCharge
                self._override_with_soc(t, result, self.arrow)
                # self.car_condition[t.seconds] = result
                # self.time_arrow[t.seconds] = self.arrow
                # self.time_list.append(t.seconds)
                # print(f"Moving @time: {t.seconds}, soc:{result.battery_StateOfCharge}")
            return continue_
        else:
            print(f"time: {t.seconds}, OUT OF BATTERY DETECTED!")
            return False


    def simulate_recharge(self, t:Time, evse_instance:ChargingStation, condition:DischargingCondition=None):
        """
        This is for the Digital Twin Vehicular Simulation, live: This method simulates an electric vehicle recharge.
        :param t:                   Current simulation time
        :param evse_instance:       Charging station where the charge is happining
        :param condition:           Condition for stopping the charge
        :return:                    Good-condition of the termination of the charge
        """
        if self.ev1 is None:
            print(f"time {t.seconds}: Initiating the recharge phase")
            target_soc = None
            recharge_timeout = None
            if condition is None:
                target_soc = self.soc_full
            elif condition.isSOC_seconds_otherwise:
                target_soc = min(max(condition.value, numpy.nextafter(self.soc_empty, 1)), self.soc_full)
            else:
                recharge_timeout = condition.value
            self.ev1 = ElectricVehiclesChargingModel(self.name,
                                                     self.ecveh,
                                                     ev_cellcapacity=self.vehicle.ev_cellcapacity,
                                                     ev_packcapacity=self.vehicle.ev_packcapacity,
                                                     initial_soc=self.soc / 100.0,
                                                     arrival_time=t.seconds,
                                                     target_soc=target_soc,
                                                     batterycapacity_kwh=self.vehicle.batterycapacity_kWh,
                                                     departure_time=recharge_timeout)
            self._override_with_soc(t, self.car_condition[self.time_list[-1]].zero_with_distance(), self.time_arrow[self.time_list[-1]].stopped_at_time(t.seconds))
            # self.car_condition[t.seconds] = self.car_condition[self.time_list[-1]].zero_with_distance()
            # self.time_arrow[t.seconds] = self.time_arrow[self.time_list[-1]].stopped_at_time(t.seconds)
            # self.time_list.append(t.seconds)
            if not self.ev1.assign_evse(evse_instance.setChargingStationAsLocked(self.ev1)):
                self.ev1 = None
                print("Cannot start the recharging phase, as another vehicle is using the same station")
                return False
            else:
                print("The charging sequence was initiated")
                return True
        else:
            print(f"time {t.seconds}: Charging continuing the charge phase")
            dt = (t - self.time_list[-1]).seconds
            self.ev1.chargevehicle(t.seconds, dt, evse_instance)
            obj = self.ev1.getvehiclestate()
            self.log_recharging_state[t.seconds] = obj
            self._update_with_soc(obj["soc"]*self.vehicle.init_soc, t)
            # self.car_condition[t.seconds] = self.car_condition[self.time_list[-1]].update_with_soc(obj["soc"]*self.vehicle.init_soc)
            # self.arrow = self.time_arrow[self.time_list[-1]].update_with_soc(obj["soc"]*self.vehicle.init_soc, t)
            # self.time_arrow[t.seconds] = self.arrow

            print(f"time {t.seconds}: Charging continuing the charge phase (soc={obj['soc']})")
            self.time_list.append(t.seconds)
            if not evse_instance.run_protocol(self.ev1):
                print(f"ERROR on running the protocol")
                return False
            if self.ev1.ischargecomplete(t.seconds):
                self.ev1 = None
                print(f"The charging sequence was complete!")
                evse_instance.unlockChargingStation()
                return True
            else:
                return True

    def __len__(self):
        return len(self.time_list)

    def __getitem__(self, item):
        if isinstance(item, int):
            t = self.time_list[item]
            return self.time_list[item], self.time_arrow[t], self.car_condition[t]
        elif isinstance(item, float):
            t = item
            return t, self.time_arrow[t], self.car_condition[t]
        elif isinstance(item, str):
            return np.asarray([getattr(self.car_condition[x], item) for x in self.time_list])

    def plot(self, *dims):
        """
        Displays a simple plot of battery demand power and current.
        :param t_array: time array, s
        :param veh_alias_name: vehicle alias name
        :return: None
        """
        fig = plt.figure()


        arrow = np.asarray(self.time_list) / 60
        idx = 1
        for dim1 in dims:
            ax1 = fig.add_subplot(len(dims), 1, idx)
            ax1.plot(arrow, self[str(dim1)])  # the time will be in minutes
            ax1.set_title(f"Vehicle")
            ax1.set_xlabel('Time [min]')
            ax1.set_ylabel(str(dim1))
            idx += 1

        plt.tight_layout()
        plt.show()

