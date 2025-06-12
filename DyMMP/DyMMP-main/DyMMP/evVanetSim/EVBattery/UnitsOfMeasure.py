from enum import Enum

def mph_to_kmh(mph):
    return mph * 1.60934

def km_to_mph(km):
    return km / 1.60934

def kmh_to_m_per_s(kmh):
    return kmh * 1000 / 3600

class TimeUnit(Enum):
    Seconds = 1
    Minutes = 60
    Hours = 3600
    Days = 86400
    Weeks = 604800


class Time:
    """
    https://stackoverflow.com/a/64017981/1376095
    """
    # _multiplier = {'seconds': 1, 'minutes': 60, 'hours': 3600, 'days': 3600 * 24, 'weeks': 3600 * 24 * 7}

    def __init__(self, scalar : float, unit : TimeUnit = TimeUnit.Seconds):
        self.num_seconds = scalar * unit.value

    def convert_to(self, tu : TimeUnit):
        return (self.num_seconds / float(tu.value))

    def __str__(self):
        return f"{self.num_seconds} s"

    def __repr__(self):
        return self.__str__()

    @property
    def seconds(self):
        return self.num_seconds

    @property
    def minutes(self):
        return self.convert_to(TimeUnit.Minutes)

    @property
    def hours(self):
        return self.convert_to(TimeUnit.Hours)

    @property
    def days(self):
        return self.convert_to(TimeUnit.Days)

    @property
    def weeks(self):
        return self.convert_to(TimeUnit.Weeks)


    def __mul__(self, scalar):
        if isinstance(scalar, float) or isinstance(scalar, int):
            return Time(self.num_seconds * scalar)

    def __add__(self, scalar):
        if isinstance(scalar, float) or isinstance(scalar, int):
            return Time(self.num_seconds + scalar)
        elif isinstance(scalar, Time):
            return Time(self.num_seconds + scalar.num_seconds)

    def __sub__(self, scalar):
        if isinstance(scalar, float) or isinstance(scalar, int):
            return Time(self.num_seconds - scalar)
        elif isinstance(scalar, Time):
            return Time(self.num_seconds - scalar.num_seconds)

    def __truediv__(self, scalar):
        if isinstance(scalar, float) or isinstance(scalar, int):
            return Time(self.num_seconds / scalar)

class DistanceUnit(Enum):
    meter = 1
    miles = 2
    kilometer = 3

class Distance:
    """Provides an internal representation of velocity in standard m_per_s"""

    def __init__(self, scalar : float, unit: DistanceUnit = DistanceUnit.meter):
        if unit == DistanceUnit.meter:
            self.distance = scalar
        elif unit == DistanceUnit.miles:
            self.distance = scalar * 1609.34
        elif unit == DistanceUnit.kilometer:
            self.distance = scalar * 1000.0

    @property
    def miles(self):
        return self.distance * 0.000621371

    @property
    def kilometers(self):
        return self.distance / 1000

    @property
    def meter(self):
        return self.distance

    def __str__(self):
        return f"{self.distance} m"

    def __repr__(self):
        return self.__str__()

    def __mul__(self, scalar):
        if isinstance(scalar, float) or isinstance(scalar, int):
            return Distance(self.distance * scalar)

    def __add__(self, scalar):
        if isinstance(scalar, float) or isinstance(scalar, int):
            return Distance(self.distance + scalar)
        elif isinstance(scalar, Distance):
            return Distance(self.distance + scalar.distance)

    def __sub__(self, scalar):
        if isinstance(scalar, float) or isinstance(scalar, int):
            return Distance(self.distance - scalar)
        elif isinstance(scalar, Distance):
            return Distance(self.distance - scalar.distance)

    def __truediv__(self, scalar):
        if isinstance(scalar, float) or isinstance(scalar, int):
            return Distance(self.distance / scalar)
        elif isinstance(scalar, Time):
            return Velocity(self.distance / scalar.num_seconds)
        elif isinstance(scalar, Velocity):
            return Time(self.distance / scalar.velocity)


class VelocityUnit(Enum):
    m_per_s = 1
    mph = 2
    kmh = 3

class Velocity:
    """Provides an internal representation of velocity in standard m_per_s"""

    def __init__(self, velocity : float, unit: VelocityUnit = VelocityUnit.m_per_s):
        if unit == VelocityUnit.m_per_s:
            self.velocity = velocity
        elif unit == VelocityUnit.mph:
            self.velocity = kmh_to_m_per_s(mph_to_kmh(velocity))
        elif unit == VelocityUnit.kmh:
            self.velocity = kmh_to_m_per_s(velocity)

    def __str__(self):
        return f"{self.velocity} m/s"

    def __repr__(self):
        return self.__str__()

    @property
    def mps(self):
        return self.velocity

    @property
    def kmh(self):
        return self.velocity * 3.6

    @property
    def mph(self):
        return self.velocity * 2.23694

    def __mul__(self, scalar):
        if isinstance(scalar, float) or isinstance(scalar, int):
            return Velocity(self.velocity * scalar)
        elif isinstance(scalar, Time):
            return Distance(self.velocity * scalar.num_seconds)

    def __add__(self, scalar):
        if isinstance(scalar, float) or isinstance(scalar, int):
            return Velocity(self.velocity + scalar)

    def __sub__(self, scalar):
        if isinstance(scalar, float) or isinstance(scalar, int):
            return Velocity(self.velocity - scalar)

    def __truediv__(self, scalar):
        if isinstance(scalar, float) or isinstance(scalar, int):
            return Velocity(self.velocity / scalar)


class DataUnit(Enum):
    Byte = 1
    KByte = 1024
    MByte = 1024 * 1024
    GByte = 1024 * 1024 * 1024


class DataSize:
    def __init__(self, scalar: float, unit: DataUnit = DataUnit.Byte):
        if unit == DataUnit.Byte:
            self.scalar = scalar
        elif unit == DataUnit.KByte:
            self.scalar = scalar * 1024
        elif unit == DataUnit.MByte:
            self.scalar = scalar * 1024* 1024
        elif unit == DataUnit.GByte:
            self.scalar = scalar * 1024* 1024* 1024

    def convert_to(self, tu: DataUnit):
        return (self.scalar / float(tu.value))

    def __str__(self):
        return f"{self.scalar} B"

    def __repr__(self):
        return self.__str__()

    @property
    def bytes(self):
        return self.convert_to(DataUnit.Byte)

    @property
    def kilobytes(self):
        return self.convert_to(DataUnit.KByte)

    @property
    def megabytes(self):
        return self.convert_to(DataUnit.MByte)

    @property
    def gigabytes(self):
        return self.convert_to(DataUnit.GByte)

if __name__ == '__main__':
    vel = Velocity(70.0, VelocityUnit.kmh)
    dist = Distance(18.0, DistanceUnit.kilometer)
    time = Time(3.0, TimeUnit.Minutes)
    print((dist / vel))
    print((vel * time))