import dataclasses
import os.path
import json
import dacite
import requests

from dataclasses import dataclass
@dataclass()
class OpenChargeMapRequest:
  maxresults: int = 1000000
  countrycode: str = "GB"
  latitude:float =54.966667
  longitude:float = -1.6
  distance:int = 100
  distanceunit: str = "km"
  verbose:bool = True
  key: str = "4da63093-c085-4b7f-9b65-ce6f2229918b"

@dataclass()
class ChargingStation:
  ID: str
  UUID: str
  Title: str
  Latitude: float
  Longitude: float

def send_request(conf:OpenChargeMapRequest=None, site="https://api.openchargemap.io/v3/poi"):
  if not os.path.exists(os.path.join("data","OpenChargeMapConf.json")):
    if conf is None:
      import yaml
      conf = yaml.safe_load(open(os.path.join("data","OpenChargeMapConf.yaml")))
      conf = dacite.from_dict(OpenChargeMapRequest, conf)
    result = requests.get(site, dataclasses.asdict(conf), headers={"Accept":"application/json"}).json()
    json.dump(result, open(os.path.join("data","OpenChargeMapConf.json"),"w"))
  else:
    result = json.load(open(os.path.join("data","OpenChargeMapConf.json")))
  return result
