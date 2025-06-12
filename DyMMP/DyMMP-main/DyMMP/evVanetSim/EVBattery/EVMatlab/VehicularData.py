from dataclasses import dataclass
## http://mocha-java.uccs.edu/ECE5720/ECE5720-Notes02.pdf (original MatLab Code)

import numpy as np
import pandas

# from evVanetSim.PyChargeModel.ElectricVehicles import ElectricVehicles
# from evVanetSim.UnitsOfMeasure import Velocity, Distance, Time, VelocityUnit, TimeUnit
# import matplotlib.pyplot as plt

class Cell:
    def __init__(self, capacity, weight, vmax, vnom, vmin):
        self.capacity = capacity  # ampere hours
        self.weight = weight  # grams
        self.vmax = vmax  # volts
        self.vnom = vnom  # volts
        self.vmin = vmin  # volts
        self.energy = self.vnom * self.capacity  # Watt-hours
        self.specific_energy = 1000 * self.capacity * self.vnom / self.weight  # Wh/kg

    @property
    def ev_cellcapacity(self):
        return self.capacity

    @property
    def ev_nominalvoltage(self):
        return self.vnom

class Module:
    def __init__(self, num_parallel, num_series, overhead, cell):
        self.num_parallel = num_parallel
        self.num_series = num_series
        self.overhead = overhead
        self.cell = cell
        self.num_cells = self.num_parallel * self.num_series
        self.capacity = self.num_parallel * self.cell.capacity
        self.weight = self.num_cells * self.cell.weight * 1 / (1 - self.overhead) / 1000  # kg
        self.energy = self.num_cells * self.cell.energy / 1000  # kWh
        self.specific_energy = 1000 * self.energy / self.weight  # Wh/kg

    @property
    def ev_cellcapacity(self):
        return self.cell.capacity

    @property
    def ev_nominalvoltage(self):
        return self.cell.vnom * self.num_parallel

class Pack:
    def __init__(self, num_series, overhead, soc_full, soc_empty, efficiency, module):
        self.num_series = num_series
        self.overhead = overhead
        self.module = module
        self.soc_full = soc_full
        self.soc_empty = soc_empty  # unitless
        self.efficiency = efficiency  # unitless, captures I*I*R losses
        self.num_cells = self.module.num_cells * self.num_series
        self.weight = self.module.weight * self.num_series * 1 / (1 - self.overhead)  # kg
        self.energy = self.module.energy * self.num_series  # kWh
        self.specific_energy = 1000 * self.energy / self.weight  # Wh/kg
        self.vmax = self.num_series * self.module.num_series * self.module.cell.vmax
        self.vnom = self.num_series * self.module.num_series * self.module.cell.vnom
        self.vmin = self.num_series * self.module.num_series * self.module.cell.vmin

    @property
    def batterycapacity_kWh(self):
        return self.energy

    @property
    def ev_cellcapacity(self):
        return self.module.ev_cellcapacity

class Motor:
    def __init__(self, Lmax, RPMrated, RPMmax, efficiency, inertia):
        self.Lmax = Lmax  # N-m
        self.RPMrated = RPMrated
        self.RPMmax = RPMmax
        self.efficiency = efficiency
        self.inertia = inertia  # kg-m2
        self.max_power = 2 * np.pi * self.Lmax * self.RPMrated / 60000  # kW


class Wheel:
    def __init__(self, radius, inertia, roll_coef):
        self.radius = radius  # m
        self.inertia = inertia  # kg-m2
        self.roll_coef = roll_coef

class Drivetrain:
    def __init__(self, inverter_efficiency, regen_torque, gear_ratio, gear_inertia, gear_efficiency, pack, motor,
                 wheel):
        self.inverter_efficiency = inverter_efficiency
        # regen torque is fraction of braking power that is used to charge
        # battery; e.g., value of 0.9 means 90% of braking power contributes
        # to charging battery; 10% lost to heat in friction brakes
        self.regen_torque = regen_torque
        self.pack = pack
        self.motor = motor
        self.wheel = wheel
        self.gear_ratio = gear_ratio
        self.gear_inertia = gear_inertia  # kg-m2, measured on motor side
        self.gear_efficiency = gear_efficiency
        self.efficiency = self.pack.efficiency * self.inverter_efficiency * \
                          self.motor.efficiency * self.gear_efficiency

    @property
    def batterycapacity_kWh(self):
        return self.pack.batterycapacity_kWh

    @property
    def ev_packcapacity(self):
        return self.pack.batterycapacity_kWh

    @property
    def ev_cellcapacity(self):
        return self.pack.ev_cellcapacity

class MobilityVehicle:
    def __init__(self, wheels, road_force, Cd, A, weight, payload, overhead_pwr, drivetrain, init_soc):
        self.drivetrain = drivetrain
        self.wheels = wheels  # number of them
        self.road_force = road_force  # N
        self.Cd = Cd  # drag coeff
        self.A = A  # frontal area, m2
        self.weight = weight  # kg
        self.payload = payload  # kg
        self.overhead_pwr = overhead_pwr  # W
        self.curb_weight = self.weight + self.drivetrain.pack.weight
        self.max_weight = self.curb_weight + self.payload
        self.rot_weight = ((self.drivetrain.motor.inertia +
                            self.drivetrain.gear_inertia) *
                           self.drivetrain.gear_ratio ** 2 +
                           self.drivetrain.wheel.inertia * self.wheels) / \
                          self.drivetrain.wheel.radius ** 2
        self.equiv_mass = self.max_weight + self.rot_weight
        self.max_speed = 2 * np.pi * self.drivetrain.wheel.radius * \
                         self.drivetrain.motor.RPMmax * 60 / \
                         (1000 * self.drivetrain.gear_ratio)  # km/h
        self.init_soc = init_soc #if init_soc is not None else self.drivetrain.pack.soc_full

    @property
    def batterycapacity_kWh(self):
        return self.drivetrain.batterycapacity_kWh

    @property
    def ev_cellcapacity(self):
        return self.drivetrain.ev_cellcapacity

    @property
    def ev_packcapacity(self):
        return self.drivetrain.ev_packcapacity