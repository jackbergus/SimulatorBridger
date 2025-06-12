import dataclasses
import os.path
from dataclasses import dataclass
from typing import List

import pandas
from lxml import etree


@dataclass
class Translation:
    en: str
    it: str

@dataclass
class IntervalEntry:
    min: float
    max: float
    value: float

@dataclass
class Interval:
    field: str
    entries: List[IntervalEntry]

    def __call__(self, *args, **kwargs):
        val = args[0] if len(args) > 0 else None
        val = kwargs.get(self.field, val)
        assert val is not None
        for entry in self.entries:
            if entry.min <= val and val <= entry.max:
                return entry.value
        return 0.0

@dataclass
class ClazzEntry:
    clazz: str
    value: float

@dataclass
class Clazz:
    field: str
    entries: List[ClazzEntry]

    def __call__(self, *args, **kwargs):
        val = args[0] if len(args) > 0 else None
        val = kwargs.get(self.field, val)
        assert val is not None
        for entry in self.entries:
            if entry.clazz == val:
                return entry.value
        return 0.0

@dataclass
class IfPresent:
    field: str
    value: float

    def __call__(self, *args, **kwargs):
        val = args[0] if len(args) > 0 else None
        val = kwargs.get(self.field, val)
        if val is None:
            return 0.0
        isTrue = (((not isinstance(val, bool)) or (val))
                  and ((not isinstance(val, float)) or (val == 1.0))
                  and ((not isinstance(val, int)) or (val == 1))
                  and (val is not None))
        return self.value if isTrue else 0.0

import matplotlib.pyplot as plt

if __name__ == "__main__":
    if not os.path.exists("/media/giacomo/80CEAABECEAAABBA/UNIBO/Dottorato/Oplon/backup/frag_15_anonimo_pruned.csv"):
        exit(1)
    if not os.path.exists("/media/giacomo/80CEAABECEAAABBA/UNIBO/Dottorato/Oplon/ComuneDiBologna/merged_geoloc 2.csv"):
        exit(2)
    census_to_risk = pandas.read_csv("/media/giacomo/80CEAABECEAAABBA/UNIBO/Dottorato/Oplon/backup/frag_15_anonimo_pruned.csv").dropna()
    census_to_risk["codiceistat"] = (census_to_risk.sez_cens2001 - 370060000000.0).astype(int).astype(str)
    pandas_geoloc = pandas.read_csv("/media/giacomo/80CEAABECEAAABBA/UNIBO/Dottorato/Oplon/ComuneDiBologna/merged_geoloc 2.csv").dropna()
    pandas_geoloc.query("lat <= 44.5631 and lat >= 44.4191 and lon <= 11.4436 and lon >= 11.2205", inplace=True)
    geo_to_risk_score = pandas_geoloc.merge(census_to_risk)[["lat", "lon", "risk_score"]]
    geo_to_risk_score.risk_score = geo_to_risk_score.risk_score / 100.0
    max_frail = geo_to_risk_score.groupby(['lat', 'lon'], as_index=False).max()
    max_frail.to_csv("max_frail_distribution.csv")
    mean_frail = geo_to_risk_score.groupby(['lat', 'lon'], as_index=False).mean()
    mean_frail.to_csv("mean_frail_distribution.csv")