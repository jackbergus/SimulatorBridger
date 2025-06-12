from dataclasses import dataclass

@dataclass(frozen=True, eq=True, order=True)
class EdgeConnectionsPerSimulationTime:
    time: float
    edge_host: str
    ioTDevices: int

@dataclass(frozen=True, eq=True, order=True)
class TimedEdge:
    id: str
    x: float
    y: float
    simtime: float = 0.0
    communication_radius: float = 0.0
    max_vehicle_communication: float = 0.0


@dataclass(frozen=True, eq=True, order=True)
class GNNEdge:
    id: str
    idx: int
    lat: float
    lon: float