import dacite

from DyMMP.dataintergration.DataIntegration import DataIntegrationConfiguration, DataIntegration
import yaml

if __name__ == "__main__":
    with open("config.yaml", 'r') as ymlfile:
        data = yaml.safe_load(ymlfile)
        conf = dacite.from_dict(DataIntegrationConfiguration, data)
    if conf is None:
        exit(1)
    di = DataIntegration(conf)
    di.serialize_configuration_to_disk()