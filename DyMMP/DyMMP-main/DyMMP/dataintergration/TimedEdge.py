from dataclasses import dataclass, asdict

@dataclass(order=True, eq=True, frozen=True)
class TimedEdge:
    id: str
    x: float
    y: float
    simtime: float
    communication_radius: float
    max_vehicle_communication: float
