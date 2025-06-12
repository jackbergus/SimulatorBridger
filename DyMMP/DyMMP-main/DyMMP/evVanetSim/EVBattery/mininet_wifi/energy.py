"""
   Mininet-WiFi: A simple networking testbed for Wireless OpenFlow/SDWN!
   @author: Ramon Fontes (ramonrf@dca.fee.unicamp.br)
"""
import dataclasses
import sys
import warnings
import time
from enum import Enum

def wh_to_amp(wh, v):
    return wh / v

class BitZigBeeState(Enum):
    IDLE = 0
    TX = 1
    RX = 2
    SLEEP = 3

@dataclasses.dataclass
class TransmissionModel:
    cost_tx_J: float
    cost_rx_J: float
    idle_J:    float
    Bps:       float

class BitZigBeeEnergy(object):
    """
    adapted from:
        Polastre et al. (2004): "Telos: Enabling ultra-low power wireless research."
        Ye et al. (2002): "An energy-efficient MAC protocol for wireless sensor networks."
    """

    def __init__(self, conf: TransmissionModel):
        """
        Initializes the BitZigBeeEnergy monitoring class.
        Spawns a background thread to monitor energy consumption.
        """
        # Conversion factor: Joules to Watt-hours
        self.joules_to_wh = 0.1 / 3600

        # Zigbee energy cost per byte in Joules (adjust based on literature or experiments)
        self.cost_tx =conf.cost_tx_J  # Energy per transmitted byte (J/byte)
        self.cost_rx = conf.cost_rx_J  # Energy per received byte (J/byte)
        self.idle_wh = conf.idle_J * self.joules_to_wh
        self.bytes_per_second = conf.Bps

    def time_to_send_packet(self, byte_size):
        return  byte_size / self.bytes_per_second

    def transmittable_bytes(self, delta_time, max_byte_size):
        return min(delta_time * self.bytes_per_second, max_byte_size)

    def get_rx_consumption(self, n_bytes):
        # Calculate energy consumption in Joules
        energy_in_joules = (n_bytes * self.cost_rx)

        # Convert Joules to Watt-hours
        energy_in_wh = energy_in_joules * self.joules_to_wh
        return energy_in_wh

    def get_tx_consumption(self, b_bytes):
        energy_in_joules = (b_bytes * self.cost_tx)
        # Convert Joules to Watt-hours
        energy_in_wh = energy_in_joules * self.joules_to_wh
        return energy_in_wh

    @staticmethod
    def get_energy(current_A, factor, pack_module_capacity, dt):
        return current_A * factor * dt / (36 * pack_module_capacity)

    def getTotalSOCConsumption(self, state, byte_size, voltage, pack_module_capacity, dt):
        val = 0.0
        if (state == BitZigBeeState.IDLE) or (byte_size <= 1.0):
            assert byte_size == 0.0
            a = wh_to_amp(self.idle_wh, voltage)
            val = BitZigBeeEnergy.get_energy(a, 0.273, pack_module_capacity, 1.0 if dt <= sys.float_info.epsilon else dt)
        elif state == BitZigBeeState.TX:
            assert byte_size > 0.0
            wh = self.get_tx_consumption(byte_size)
            a = wh_to_amp(wh, voltage)
            val = BitZigBeeEnergy.get_energy(a, 0.380, pack_module_capacity, dt)
        elif state == BitZigBeeState.RX:
            assert byte_size > 0.0
            wh = self.get_tx_consumption(byte_size)
            a = wh_to_amp(wh, voltage)
            val = BitZigBeeEnergy.get_energy(a, 0.313, pack_module_capacity, dt)
        elif state == BitZigBeeState.SLEEP:
            assert byte_size == 0.0
            a = wh_to_amp(self.idle_wh, voltage)
            val = BitZigBeeEnergy.get_energy(a, 0.033, pack_module_capacity, dt)
        if val <= sys.float_info.epsilon:
            return 0.0
        return val

