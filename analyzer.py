from matplotlib import pyplot as plt
import numpy as np
import pandas as pd
import glob
import argparse

# Path: analizer.py

parser = argparse.ArgumentParser(description='Analyze data')

parser.add_argument('-v', '--verbose',
                    action='store_true', help='verbose mode')
parser.add_argument('-d', '--detailed',
                    help='Detailed analysis')

args = parser.parse_args()

configs = []

with open("configs.txt", "r") as f:
    for line in f:
        if '#' not in line and line.strip() != "":
            configs.append(line.replace("\n", ""))

totConfigs = len(configs)


def analyze_dataset(dt: str):

    dataset = pd.read_csv(dt)
    data = dataset.to_numpy()

    # data blueprint: [M, N, X, first, totMoves, totNodes, totEvaluatedNodes, totPrunedNodes, totReusedNodes]

    # order data based on first three columns
    data = data[np.lexsort((data[:, 2], data[:, 1], data[:, 0]))]

    # divide data in subarrays based on first three columns
    data = np.split(data, np.where(np.diff(data[:, 0:3], axis=0) != 0)[0] + 1)

    # eliminate empty subarrays
    data = [x for x in data if x.size != 0]

    mean_data = []

    for config in data:
        # collaps subarrays into two, substituting the first three columns with the config and the other ones with the mean
        config = np.concatenate(
            (config[0, 0:3], np.mean(config[:, 4:], axis=0)))
        mean_data.append(config)

    mean_data = np.array(mean_data)

    y = []
    for i in range(3, 8):
        y.append(mean_data[:, i])

    y = np.array(y)

    plt.figure(figsize=(20, 10))
    # plot data
    for i in range(0, 5):
        plt.plot(y[i], label=mean_data[0, i])

    plt.title(f'Average performance of {dt[5:-4]}')
    plt.xticks(np.arange(0, totConfigs, 1), configs, rotation=90)
    plt.legend(['Total Moves', 'Nodes visited', 'Nodes evaluated',
                'Nodes pruned', 'Nodes reused(TT)'])
    plt.xlabel('Configuration')
    plt.ylabel('Average value')
    plt.yscale('log')
    plt.grid(True)
    plt.savefig(f'plots/plot_{dt[5:-4]}.png')

    return mean_data


files = glob.glob('data_*.csv')
datasets = []
for f in files:
    print(f)
    datasets.append(analyze_dataset(f))

datasets = np.array(datasets)
data_types = ["Total Moves", "Nodes visited","Nodes evaluated","Nodes pruned","Nodes reused(TT)"]

# plot the comparison between the datasets, per column
for i in range(3, 8):
    plt.figure(figsize=(20, 10))
    title = "Comparison for " + data_types[i-3]
    for j in range(0, len(datasets)):
        plt.plot(datasets[j][:, i], label=files[j][5:-4])
    
    plt.title(f'Comparison performance for {data_types[i-3]}')
    plt.xticks(np.arange(0, totConfigs, 1), configs, rotation=90)
    plt.legend()
    plt.yscale('log')
    plt.xlabel('Configuration')
    plt.ylabel('Average value')
    plt.grid(True)
    plt.savefig(f'plots/plot_comparison_{data_types[i-3]}.png')
