import datetime
import os
import torch
import torch.nn as nn


from DyMMP.gnn_train.dataloader import create_edge_index_and_features, TrafficVolumeGraphDataLoader
from DyMMP.gnn_train.gnn import GNNModel
from DyMMP.gnn_train.trainer import GNNTrainer
from DyMMP.gnn_train.unpack_data import gnn_processing_csv_connections, configuration


def gnn_train(csv, freq, folder):
    idx = 0
    train_data_file, val_data_file, test_data_file, stations_included_file, all_data_file = gnn_processing_csv_connections(csv, freq)
    ls = ["gnn_network_hop", "gnn_network_hop_top_preserve", "gnn_network_mintime", "gnn_network_mintime_top_preserve",
          "gnn_network_minlen", "gnn_network_minlen_top_preserve"]
    for x in ls:
        config = configuration(x, folder)
        name = config["name"]
        lr = config["lr"]
        batch_size = config["batch_size"]
        loss_function = nn.L1Loss()

        stations_data_file = os.path.join("data", folder, "traffic_stations.csv")
        model = GNNModel()
        num_workers = int(os.cpu_count() / 3 * 2)
        print("Number of workers: ", num_workers)
        device = "cpu" #"cuda:4", "cpu"

        edge_index, edge_weight = create_edge_index_and_features(stations_included_file, stations_data_file, os.path.join("data", folder, "gnn_traffic", f"{x}.csv"))
        train_dataloader = TrafficVolumeGraphDataLoader(train_data_file, edge_index, edge_weight, batch_size, num_workers, shuffle=True)
        val_dataloader = TrafficVolumeGraphDataLoader(val_data_file, edge_index, edge_weight, batch_size, num_workers)
        test_dataloader = TrafficVolumeGraphDataLoader(test_data_file, edge_index, edge_weight, batch_size, num_workers)
        merged_dataloader = TrafficVolumeGraphDataLoader(all_data_file, edge_index, edge_weight, batch_size, num_workers)
        trainer = GNNTrainer(model, train_dataloader, val_dataloader, test_dataloader, config, loss_function, device)

        name = os.path.join("data", folder, f"{x}_stats.txt")
        f = open(name, "a+")
        if not os.path.isfile(config["checkpoint_file"]):
            try:
                f.write(f"Experiments started at: {datetime.datetime.now()}" + os.linesep)
                trainer.print_model_size(f)
                optimizer = torch.optim.Adam(model.parameters(), lr=lr)
                # Uncomment to use learning rate scheduler
                # scheduler = torch.optim.lr_scheduler.StepLR(optimizer, 5, 0.5)
                # trainer.train(optimizer, scheduler)
                trainer.train(optimizer)
                f.write(f"Experiments finished at: {datetime.datetime.now()}" + os.linesep)
                trainer.summarize_training()
                trainer.evaluate(f, merged_dataloader)
                trainer.print_model_size(f)
                trainer.save_prediction_plot(0, 200)
                trainer.dump_data_all(f, merged_dataloader)
                f.write(f"Dumping finished at: {datetime.datetime.now()}" + os.linesep)
            except Exception as e:
                print(f"ERROR ON {x}")
        # else:
        #     f.write(f"Re-evaluation started at: {datetime.datetime.now()}" + os.linesep)
        #     trainer.model_load()
        #     trainer.dump_data_all(f, merged_dataloader)
        #     f.write(f"Dumping finished at: {datetime.datetime.now()}" + os.linesep)
        f.close()

        # else:
        #     print("Checkpoint file exists. Please delete checkpoint file to re-train model.")

if __name__ == "__main__":
    idx = 0
    train_data_file, val_data_file, test_data_file, stations_included_file = gnn_processing_csv_connections("/media/giacomo/BigData/backuppo/data/gnn_connections.csv", "15min")
    ls = ["gnn_network_hop", "gnn_network_hop_top_preserve", "gnn_network_mintime", "gnn_network_mintime_top_preserve",
          "gnn_network_minlen", "gnn_network_minlen_top_preserve"]


        # Evaluate model on test data and compute test loss



        # from_index = 100
        # length = 10
        # trainer.save_prediction_plot(from_index, length)