import copy
from typing import Tuple, Optional

## http://mocha-java.uccs.edu/ECE5720/ECE5720-Notes02.pdf (original MatLab Code)

from DyMMP.evVanetSim.EVBattery.EVMatlab.VehicularData import *
from DyMMP.evVanetSim.EVBattery.UnitsOfMeasure import Time, Velocity, Distance


# from evVanetSim.EVSim import VehSimulation, DischargingCondition


@dataclass
class SolutionRecord:
    des_acc: float = 0
    des_acc_F: float = 0
    aero_F: float = 0
    roll_grade_F: float = 0
    demand_torque: float = 0
    max_torque: float = 0
    limit_regen: float = 0
    limit_torque: float = 0
    motor_torque: float = 0
    actual_acc_F: float = 0
    actual_acc: float = 0
    motor_speed: float = 0
    actual_speed: float = 0  # actual speed, m/s
    actual_speed_kmph: float = 0  # actual speed, km/h
    distance: float = 0
    demand_power: float = 0
    limit_power: float = 0
    battery_demand: float = 0
    current: float = 0
    cell_current: float = 0
    battery_StateOfCharge: float = 0
    grade: float = 0

    @staticmethod
    def withSocVal(soc):
        s = SolutionRecord()
        s.battery_StateOfCharge = soc
        return s

    def zero_with_distance(self):
        s = SolutionRecord()
        s.distance = self.distance
        return s

    def update_with_soc(self, new_soc):
        cpy = copy.deepcopy(self)
        cpy.battery_StateOfCharge = new_soc
        return cpy

@dataclass
class SimTime:
    time: Time
    velocity: Velocity
    grade: float = 0.3

@dataclass
class TimeArrow:
    time: Time = Time(-1)
    speed: Velocity = Velocity(0)
    motor_speed: Velocity = Velocity(0)
    distance: Distance = Distance(0)
    soc: float = 0.0

    @staticmethod
    def zero_time(init_time, soc_full):
        return TimeArrow(time=Time(init_time-1),soc=soc_full)

    def stopped_at_time(self, time):
        cpy = copy.deepcopy(self)
        cpy.time = time
        cpy.speed = Velocity(0)
        cpy.motor_speed = Velocity(0)
        return cpy

    def update_with_soc(self, new_soc, time):
        cpy = copy.deepcopy(self)
        cpy.time = time
        cpy.soc = new_soc
        return cpy

    def zero_with_soc(self, new_soc, time):
        return TimeArrow(time=time,soc=new_soc)

def sim_delta(vehicle: MobilityVehicle,
              current: SimTime,
              prev : TimeArrow = None,
              rho = 1.225) -> Tuple[bool, Optional[SolutionRecord], Optional[TimeArrow]]:
    results: SolutionRecord = SolutionRecord()
    if prev is None:
        prev = TimeArrow.zero_time(current.time.seconds, vehicle.init_soc)
    if prev.soc <= vehicle.drivetrain.pack.soc_empty:
        return False, None, None
    dt = current.time.seconds - prev.time.seconds
    if dt == 0:
        dt = 1e-6  # prevent division by zero
    results.grade = np.arctan(current.grade / 100)
    results.des_acc = (current.velocity.mps - prev.speed.mps) / dt  # m/s2
    results.des_acc_F = vehicle.equiv_mass * results.des_acc
    results.aero_F = 0.5 * rho * vehicle.Cd * vehicle.A * prev.speed.mps ** 2
    results.roll_grade_F = vehicle.max_weight * 9.81 * np.sin(results.grade)
    if abs(prev.speed.mps) > 0:
        results.roll_grade_F += vehicle.drivetrain.wheel.roll_coef * vehicle.max_weight * 9.81
    results.demand_torque = (results.des_acc_F +
                                results.aero_F +
                                results.roll_grade_F +
                                vehicle.road_force) * \
                               vehicle.drivetrain.wheel.radius / \
                               vehicle.drivetrain.gear_ratio
    if prev.motor_speed.mps < vehicle.drivetrain.motor.RPMrated:
        results.max_torque = vehicle.drivetrain.motor.Lmax
    else:
        results.max_torque = vehicle.drivetrain.motor.Lmax * \
                                vehicle.drivetrain.motor.RPMrated / prev.motor_speed.mps
    results.limit_regen = min(results.max_torque,
                                 vehicle.drivetrain.regen_torque *
                                 vehicle.drivetrain.motor.Lmax)
    results.limit_torque = min(results.demand_torque,
                                  results.max_torque)
    if results.limit_torque > 0:
        results.motor_torque = results.limit_torque
    else:
        results.motor_torque = max(-results.limit_regen,
                                      results.limit_torque)
    results.actual_acc_F = (results.limit_torque *
                                     vehicle.drivetrain.gear_ratio /
                                     vehicle.drivetrain.wheel.radius -
                                     results.aero_F -
                                     results.roll_grade_F -
                                     vehicle.road_force)
    results.actual_acc = results.actual_acc_F / vehicle.equiv_mass
    results.motor_speed = min(vehicle.drivetrain.motor.RPMmax,
                                 vehicle.drivetrain.gear_ratio *
                                 (prev.speed.mps + results.actual_acc * dt) * 60 /
                                 (2 * np.pi * vehicle.drivetrain.wheel.radius))
    results.actual_speed = results.motor_speed * 2 * np.pi * vehicle.drivetrain.wheel.radius / (
            60 * vehicle.drivetrain.gear_ratio)
    results.actual_speed_kmph = results.actual_speed * 3600 / 1000
    # print(results.motor_speed)
    deltadistance = (results.actual_speed + prev.speed.mps) / 2 * dt / 1000
    results.distance = prev.distance.meter + deltadistance
    if results.limit_torque > 0:
        results.demand_power = results.limit_torque
    else:
        results.demand_power = max(results.limit_torque,
                                      -results.limit_regen)
    results.demand_power = results.demand_power * 2 * np.pi * \
                              (prev.motor_speed.mps + results.motor_speed) / 2 / 60000
    results.limit_power = max(-vehicle.drivetrain.motor.max_power,
                                 min(vehicle.drivetrain.motor.max_power,
                                     results.demand_power))
    results.battery_demand = vehicle.overhead_pwr / 1000
    if results.limit_power > 0:
        results.battery_demand += results.limit_power / vehicle.drivetrain.efficiency
    else:
        results.battery_demand += results.limit_power * vehicle.drivetrain.efficiency
    results.current = results.battery_demand * 1000 / vehicle.drivetrain.pack.vnom
    results.battery_StateOfCharge = prev.soc - results.current * dt / (36 * vehicle.drivetrain.pack.module.capacity)
    ta = TimeArrow(current.time, Velocity(results.actual_speed), Velocity(results.motor_speed), Distance(results.distance), results.battery_StateOfCharge)
    return results.battery_StateOfCharge > vehicle.drivetrain.pack.soc_empty, results, ta



