import os
import xml

import dacite

from DyMMP.dataintergration.DataIntegration import DataIntegrationConfiguration, DataIntegration
import yaml

from DyMMP.gnn_train.gnn_main import gnn_train

if __name__ == "__main__":
    with open("config_Bologna.yaml", 'r') as ymlfile:
        data = yaml.safe_load(ymlfile)
        conf = dacite.from_dict(DataIntegrationConfiguration, data)
    if conf is None:
        exit(1)

    ## Integrating all the data
    di = DataIntegration(conf)
    di.prepare_datasets()
    di.serialize_configuration_to_disk()
    # # TODO: GNN Training!

    # gnn_folder = di.get_gnn_folder()
    # gnn_train(os.path.join(gnn_folder, "gnn_connections.csv"), "1min", di.get_place_name())