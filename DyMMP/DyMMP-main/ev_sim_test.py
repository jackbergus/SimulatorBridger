from DyMMP.evVanetSim.EVBattery.CSSim import ChargingStation
from DyMMP.evVanetSim.EVBattery.EVMatlab.conf import load_car_from_configuration, load_configuration_from_file
from DyMMP.evVanetSim.EVBattery.UnitsOfMeasure import Time, Velocity, VelocityUnit, DataSize, DataUnit
from DyMMP.evVanetSim.VehicleDigitalTwin import DischargingCondition, SimulateCharge, TransmitPacket, SimulateMoving


def legacy_example(RunningEvents, veh_conf, current_time):
    evse_instance = ChargingStation(efficiency=0.99, Prated_kW=6.6, evse_id=1)
    granularity = Time(1)
    latest_time = Time(current_time.seconds)
    veh = load_car_from_configuration(veh_conf, "my_summer_car")
    while len(RunningEvents) > 0:
        ## Depleting the vehicle running events
        while len(RunningEvents) > 0:
            if veh.isOutOfBattery() or veh.isVehiclePerformingCharging():
                break
            else:
                velocity, time = RunningEvents.pop(0)
                current_time += (time - latest_time)
                veh.simulate_moving_with_velocity(current_time, velocity)
                latest_time = time

        ## Freely assuming for the sake of the argument that I can promptly recharge something
        while veh.isVehiclePerformingCharging() or veh.isOutOfBattery():
            current_time += granularity
            if not veh.simulate_recharge(current_time, evse_instance, DischargingCondition(True, .9)):
                raise ValueError("Protocol error!")
    veh.plot("actual_speed_kmph", "current", "battery_StateOfCharge")


def towards_digital_twin(end_sim_time : Time, veh_conf):
    evse_instance = ChargingStation(efficiency=0.99, Prated_kW=6.6, evse_id=1)
    granularity = Time(1)
    cVel = Velocity(50, VelocityUnit.kmh)
    veh = load_car_from_configuration(veh_conf, "my_summer_car")
    dCond = DischargingCondition(True, .9)
    veh.receive_event(TransmitPacket(Time(0), DataSize(1, DataUnit.MByte).bytes, True))
    for t in range(end_sim_time.seconds):
        current_time = Time(t)
        if veh.isVehiclePerformingCharging() or veh.isOutOfBattery():
            veh.receive_event(SimulateCharge(current_time, evse_instance, dCond))
        else:
            veh.receive_event(SimulateMoving(current_time, cVel, 0.2))
    veh.plot("actual_speed_kmph", "current", "battery_StateOfCharge")


if __name__ == '__main__':
    veh_conf = load_configuration_from_file("/home/giacomo/PycharmProjects/pythonProject1/ChevyVolt.yaml")
    print('\n\nStarting sims...')
    # current_time = Time(0)
    # RunningEvents = [(Velocity(50, VelocityUnit.kmh), Time(x)) for x in range(0, 3600)]
    # legacy_example(RunningEvents, veh_conf, current_time)

    towards_digital_twin(Time(3600), veh_conf)
    # veh.plot("actual_speed_kmph", "current", "battery_StateOfCharge")



