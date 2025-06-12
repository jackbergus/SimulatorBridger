"""
This script unpacks the zipped datafile into a pickled dataframe.
"""
import math
import os

import pandas
import pandas as pd
from os.path import isfile
from tqdm import tqdm

def parse_time_column(traffic_data):
    if traffic_data["time_from"].dtype == float and traffic_data["time_to"].dtype == float:
        today = pd.to_datetime('today')
        traffic_data["time_from"] = pd.to_timedelta(traffic_data["time_from"], 's') + today
        traffic_data["time_to"] = pd.to_timedelta(traffic_data["time_to"], 's') + today
    else:
        traffic_data["time_from"] = pd.to_datetime(traffic_data["time_from"])
        traffic_data["time_to"] = pd.to_datetime(traffic_data["time_to"])
    return traffic_data

def gnn_processing_csv_connections(csv, freq, val_fraction = 0.15, test_fraction = 0.15, normalize_data = None):
    from datetime import datetime, timedelta
    train_data_file = f"{csv}_train.pickle"
    val_data_file = f"{csv}_val.pickle"
    test_data_file = f"{csv}_test.pickle"
    all_data_file = f"{csv}_all.pickle"
    stations_included_file = f"{csv}_stations.csv"
    if not os.path.exists(train_data_file) and not os.path.exists(test_data_file) and not os.path.exists(val_data_file) and not os.path.exists(stations_included_file):
        traffic_data = pandas.read_csv(csv)
        traffic_data = parse_time_column(traffic_data)

        # traffic_data = pd.read_pickle(data_file_pkl)
        # station_df = pd.read_csv(stations_data_file)
        unique_stations = traffic_data["station_id"].unique()
        first_timestamp = traffic_data["time_from"].min()
        last_timestamp = traffic_data["time_from"].max()
        print(len(unique_stations))
        print(unique_stations)

        print(f"First timestamp: {first_timestamp}")
        print(f"Last timestamp: {last_timestamp}")
        print("Building time series dataframe... Please grab a coffee!")

        # range = pd.date_range(first_timestamp, last_timestamp, freq=freq)
        # time_series_data = pd.DataFrame(index=pd.date_range(first_timestamp, last_timestamp, freq=freq),
        #                                 columns=unique_stations)

        dd = dict()
        for station_id in tqdm(unique_stations):
            df = traffic_data.loc[traffic_data["station_id"] == station_id, ["volume", "time_from"]]
            ts = pandas.Series(df["volume"])
            ts.index =  df["time_from"]
            dd[station_id] = ts.groupby(pd.Grouper(freq=freq)).max()
        time_series_data = pandas.DataFrame(dd)

        # Drop stations with too many NaNs / too few observations
        # print(f"Dropping stations with too few observations (<{min_number_of_observations})...")
        # time_series_data.dropna(thresh=min_number_of_observations, axis=1, inplace=True)

        # All stations are missing values at 22:00 every day.
        # Replace these all-NaN rows by the mean of the row before and the row after.
        print("Filling rows with all NaN...")
        time_series_data.loc[time_series_data.isnull().all(axis=1), :] = (time_series_data.ffill(
            limit=1) + time_series_data.bfill(limit=1)) / 2

        # Split the dataset into training, validation and testing data
        n_total = len(time_series_data)
        val_size = int(val_fraction * n_total)
        test_size = int(test_fraction * n_total)
        train_size = n_total - val_size - test_size
        train_df = time_series_data.iloc[0: train_size]
        val_df = time_series_data.iloc[train_size: train_size + val_size]
        test_df = time_series_data.iloc[train_size + val_size: n_total]

        if normalize_data:
            print("Normalizing data...")
            if normalize_data == "minmax":
                # Scale to [0,1]
                min_val, max_val = train_df.min(), train_df.max()
                mean, std = min_val, max_val - min_val
            elif normalize_data == "normal":
                # Compute z-scores
                mean, std = train_df.mean(), train_df.std()
            else:
                print("Invalid normalization method: {normalize_data}.")
                mean, std = 0, 1

            train_df = (train_df - mean) / std
            val_df = (val_df - mean) / std
            test_df = (test_df - mean) / std

        train_df.to_pickle(train_data_file)
        val_df.to_pickle(val_data_file)
        test_df.to_pickle(test_data_file)
        time_series_data.to_pickle(all_data_file)

        print(
            f"Time series contain {n_total} hours of data from {len(time_series_data.columns)} stations. (Missing observations have value NaN)")
        print(f"Split: {len(train_df)} (train), {len(val_df)} (val) and {len(test_df)} (test) samples")
        print(f"Time series data saved to \"{train_data_file}\", \"{val_data_file}\" and \"{test_data_file}\"")

        stations_included = train_df.columns
        # assert set(stations_included) == set(traffic_data.station_name.unique())
        pd.Series(stations_included).to_csv(stations_included_file)
        time_series_data.to_csv(f"{csv}_merged.csv")

        print(f"IDs of stations included in pre-processed data saved to {stations_included_file}.")
    return train_data_file, val_data_file, test_data_file, stations_included_file, all_data_file


def configuration(config_name, folder, epochs=100, batch_size = 128, lr=0.001, earlystop_limit=20):
    config_gnn = {}
    config_gnn["name"] = "GNN"
    config_gnn["batch_size"] = batch_size
    config_gnn["lr"] = lr
    config_gnn["epochs"] = epochs
    config_gnn["val_per_epoch"] = 4
    config_gnn["checkpoint_file"] = os.path.join("data", folder, "gnn_traffic", f"{config_name}_checkpoint.pth")
    config_gnn["prediction_plot_dir"] = os.path.join("data", folder, "gnn_traffic", f"{config_name}_predictions")
    config_gnn["loss_plot_file"] = os.path.join("data", folder, "gnn_traffic", f"{config_name}_loss_plot.png")
    config_gnn["earlystop_limit"] = earlystop_limit
    return config_gnn