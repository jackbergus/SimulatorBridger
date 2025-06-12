from dataclasses import dataclass

from DyMMP.evVanetSim.EVBattery.PyChargeModel.ElectricVehicles import ElectricVehicleChargingConfRaw
from DyMMP.evVanetSim.EVBattery.UnitsOfMeasure import Velocity, Distance, Time
from DyMMP.evVanetSim.EVBattery.mininet_wifi.energy import TransmissionModel, BitZigBeeEnergy


@dataclass
class CellConf:
    capacity: int
    weight: int
    vmax: float
    vnom: float
    vmin: float

    def asCell(self):
        from DyMMP.evVanetSim.EVBattery.EVMatlab.VehicularData import Cell
        return Cell(self.capacity, self.weight, self.vmax, self.vnom, self.vmin)

@dataclass
class ModuleConf:
    num_parallel: int
    num_series: int
    overhead: float
    cell: CellConf

    def asModule(self):
        cell = self.cell.asCell()
        from DyMMP.evVanetSim.EVBattery.EVMatlab.VehicularData import Module
        return Module(self.num_parallel, self.num_series, self.overhead, cell)

@dataclass
class PackConf:
    num_series: int
    overhead: float
    soc_full: int
    soc_empty: int
    efficiency: float
    module: ModuleConf

    def asPack(self):
        module = self.module.asModule()
        from DyMMP.evVanetSim.EVBattery.EVMatlab.VehicularData import Pack
        return Pack(self.num_series, self.overhead, self.soc_full, self.soc_empty, self.efficiency, module)

@dataclass
class MotorConf:
    Lmax: int
    RPMrated: int
    RPMMax: int
    efficiency: float
    inertia: float

    def asMotor(self):
        from DyMMP.evVanetSim.EVBattery.EVMatlab.VehicularData import Motor
        return Motor(self.Lmax, self.RPMrated, self.RPMMax, self.efficiency, self.inertia)

@dataclass
class WheelConf:
    radius: float
    inertia: int
    roll_coeff: float

    def asWheel(self):
        from DyMMP.evVanetSim.EVBattery.EVMatlab.VehicularData import Wheel
        return Wheel(self.radius, self.inertia, self.roll_coeff)

@dataclass
class DriveTrainConf:
    inverter_efficiency: float
    regen_torque: float
    gear_ratio: int
    gear_inertia: float
    gear_efficiency: float
    pack: PackConf
    motor: MotorConf
    wheel: WheelConf

    def asDriveTrain(self):
        pack = self.pack.asPack()
        motor = self.motor.asMotor()
        wheel = self.wheel.asWheel()
        from DyMMP.evVanetSim.EVBattery.EVMatlab.VehicularData import Drivetrain
        return Drivetrain(self.inverter_efficiency, self.regen_torque, self.gear_ratio, self.gear_inertia, self.gear_efficiency, pack, motor, wheel)

@dataclass
class Vehicle:
    wheels: int
    road_force: int
    Cd: float
    A: float
    weight: float
    payload: float
    overhead_pwr: int
    init_soc: int
    drive_train: DriveTrainConf
    charging_model: ElectricVehicleChargingConfRaw
    transmission: TransmissionModel

    def asBitZigBeeEnergyModel(self):
        return BitZigBeeEnergy(self.transmission)

    def asVehicle(self, soc=None):
        drivetrain = self.drive_train.asDriveTrain()
        from DyMMP.evVanetSim.EVBattery.EVMatlab.VehicularData import MobilityVehicle
        return MobilityVehicle(self.wheels, self.road_force, self.Cd, self.A, self.weight, self.payload, self.overhead_pwr, drivetrain, self.init_soc if soc is None else soc)

class VehicleEntryPoint:
    def __init__(self, yaml_file):
        import yaml
        with open(yaml_file, 'r') as f:
            d = yaml.safe_load(f)
        import dacite
        self.conf:Vehicle = dacite.from_dict(Vehicle, d)

    def normalized_rate(self, curr_value, veh = None):
        if veh is None:
            veh = self.instantiate()
        return 1.0 - (curr_value - veh.soc_empty) / (veh.soc_full- veh.soc_empty)

    def estimate_road_consumption(self, distance:Distance, velocity:Velocity, grade:float):
        current_time = distance/velocity
        veh = self.instantiate()
        init_soc = veh.soc
        veh.simulate_moving_with_velocity(current_time, velocity, grade)
        return 1.0 - (veh.soc - veh.soc_empty) / (init_soc- veh.soc_empty), current_time

    def estimate_recharge_from_empty(self, recharging_station_conf, granularity_sec = 1, max_charge = 0.9):
        from DyMMP.evVanetSim.EVBattery.CSSim import ChargingStationConf
        assert isinstance(recharging_station_conf, ChargingStationConf)
        evse_instance = recharging_station_conf.asChargingStation()
        empty_veh = self.conf.asVehicle(self.conf.drive_train.pack.soc_empty)
        from DyMMP.evVanetSim.VehicleDigitalTwin import VehicleDigitalTwin
        from DyMMP.evVanetSim.VehicleDigitalTwin import DischargingCondition
        veh = VehicleDigitalTwin("car", empty_veh, self.conf.charging_model.asCompactConf())
        assert veh.isOutOfBattery()
        current_time = Time(0)
        granularity = Time(granularity_sec)
        while veh.isVehiclePerformingCharging() or veh.isOutOfBattery():
            current_time += granularity
            if not veh.simulate_recharge(current_time, evse_instance, DischargingCondition(True, max_charge)):
                raise ValueError("Protocol error!")
        return self.normalized_rate(veh.soc - self.conf.drive_train.pack.soc_empty, veh), current_time

    def instantiate(self):
        veh = self.conf.asVehicle()
        from DyMMP.evVanetSim.VehicleDigitalTwin import VehicleDigitalTwin
        veh = VehicleDigitalTwin("car", veh, self.conf.charging_model.asCompactConf())
        return veh

def load_configuration_from_file(yaml_file):
    import yaml
    with open(yaml_file, 'r') as f:
        d = yaml.safe_load(f)
    import dacite
    return dacite.from_dict(Vehicle, d)

def load_car_from_configuration(conf: Vehicle, name:str):
    veh = conf.asVehicle()
    from DyMMP.evVanetSim.VehicleDigitalTwin import VehicleDigitalTwin
    veh = VehicleDigitalTwin("car", veh, conf.charging_model.asCompactConf(), conf.asBitZigBeeEnergyModel())
    return veh

def load_car_from_configuration_file(yaml_file):
    import yaml
    with open(yaml_file, 'r') as f:
        d = yaml.safe_load(f)
    import dacite
    conf = dacite.from_dict(Vehicle, d)
    veh = conf.asVehicle()
    from DyMMP.evVanetSim.VehicleDigitalTwin import VehicleDigitalTwin
    veh = VehicleDigitalTwin("car", veh, conf.charging_model.asCompactConf(), conf.asBitZigBeeEnergyModel())
    return veh