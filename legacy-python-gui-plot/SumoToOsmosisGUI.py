#!/usr/bin/env python3
import json
import os
import SumoNetVis
import matplotlib.pyplot as plt
from SumoNetVis import USA_STYLE
import seaborn as sns
import imageio
import numpy as np
import progressbar
import yaml
from pathlib import Path
import xml.etree.ElementTree as et
import sys
import csv
import math

widgets=[
    ' [', progressbar.Timer(), '] ',
    progressbar.Bar(),
    ' (', progressbar.ETA(), ') ',
]

def rgb_to_255(t):
    return tuple([int(t[0] * 255), int(t[1]*255), int(t[2]*255)])

def generate_palette(N):
    return map(lambda x: '#%02x%02x%02x' % rgb_to_255(x), sns.color_palette(n_colors=N))

def latex_color_def(name, hex_html_colour):
    return "\\definecolor{"+name+"}{HTML}{"+hex_html_colour[1:]+"}\n"
def latex_node_name(name, x, y, colourname):
    return "\\node[fill="+colourname+",circle] at ("+str(x)+","+str(y)+") ("+name+") {};\n"
def latex_veh_name(name, x, y, colourname):
    return "\\node[fill="+colourname+"] at ("+str(x)+","+str(y)+") ("+name+") {"+name+"};\n"
def latex_cluster(semname, vehColl):
    return "\n".join(map(lambda x: "\\draw[->,"+semname+",very thick] ("+x+") -- ("+semname+");", vehColl))

class Point(object):
    def __init__(self, x = 0, y = 0, col = '#%02x%02x%02x' % rgb_to_255(tuple([0,0,0]))):
        self.x = x
        self.y = y
        self.col = col

class SumoToOsmosisGUI(object):
    def __init__(self, network, trajectory, intersection_file, sem_pos):
        self.semaphores = dict()
        self.network = network
        self.trajectories = SumoNetVis.Trajectories()
        self.trajectories.read_from_fcd(trajectory)

        trajScanMap = dict()
        for veh in self.trajectories:
            trajScanMap[veh.id] = dict()
            idx = 0
            for time in veh.time:
                trajScanMap[veh.id][time] = idx
                idx = idx + 1

        self.semaphores[""] = Point()
        self.intersections = dict()
        with open(intersection_file) as f:
            self.intersections = json.load(f)
            for time in self.intersections:
                for sem in self.intersections[time]:
                    self.intersections[time][sem] = set(self.intersections[time][sem])
                    self.semaphores[sem] = Point(0,0)

        self.semx = list()
        self.semy = list()
        self.semcol = list()
        with open(sem_pos, mode='r') as csv_file_sem:
            csv_reader = list(csv.DictReader(csv_file_sem))
            for (row, col) in zip(csv_reader, generate_palette(len(csv_reader))):
                id = row["Id"]
                if id not in self.semaphores:
                    self.semaphores[id] = Point(float(row["X"]), float(row["Y"]), col)
                else:
                    self.semaphores[id].x = float(row["X"])
                    self.semaphores[id].y = float(row["Y"])
                    self.semaphores[id].col = col
                self.semx.append(self.semaphores[id].x)
                self.semy.append(self.semaphores[id].y)
                self.semcol.append(self.semaphores[id].col)

        idx = 0
        self.trajName = dict()
        for traj in self.trajectories:
            self.trajName[traj.id] = idx
            traj.point_plot_kwargs["ms"] = 10
            idx = idx + 1

        for time in self.intersections:
            for sem in self.intersections[time]:
                for veh in self.intersections[time][sem]:
                    self.trajectories.trajectories[self.trajName[veh]].colors[trajScanMap[veh][float(time)]] = self.semaphores[sem].col


    def generate_static(self, target):
        for (veh, col) in zip(self.trajName.keys(), generate_palette(len(self.trajName))):
            self.trajectories.trajectories[self.trajName[veh]].colors = list(map(lambda x: col, self.trajectories.trajectories[self.trajName[veh]].colors))
        net = SumoNetVis.Net(self.network)
        fig, ax = plt.subplots()
        fig.set_figheight(10)
        fig.set_figwidth(16)
        net.plot(ax, style=USA_STYLE, stripe_width_scale=3)
        self.trajectories.plot(ax)
        plt.savefig(target)

    def generate_latex(self, target, factor, delta):
        mainFile = ["\\documentclass[tikz]{standalone}\n\\usepackage{xcolor}"]
        for sem in self.semaphores:
            if len(sem) > 0:
                mainFile.append(latex_color_def(sem, self.semaphores[sem].col))
        mainFile.append("\\begin{document}")
        xmax = 0
        ymax = 0
        xmin = sys.float_info.max
        ymin = sys.float_info.max
        pad = math.ceil(math.log10(self.trajectories.end))
        d = dict()
        for time in np.arange(self.trajectories.start, self.trajectories.end+self.trajectories.timestep, self.trajectories.timestep):
            latexFile = list()
            latexFile.append("\\begin{tikzpicture}")
            for sem in self.semaphores:
                if len(sem)==0: continue
                x,y,col = self.semaphores[sem].x, self.semaphores[sem].y, self.semaphores[sem].col
                xmax = max(x+delta, xmax)
                ymax = max(y+delta, ymax)
                xmin = min(x-delta, xmin)
                ymin = min(y-delta, ymin)
                latexFile.append(latex_node_name(sem, x * factor, y * factor, sem))
            if str(time) in self.intersections:
                strtime = str(time)
                for sem in self.intersections[strtime]:
                    if len(sem)==0: continue
                    for veh in self.intersections[strtime][sem]:
                        traj = self.trajectories.trajectories[self.trajName[veh]]
                        values = traj._get_values_at_time(time)
                        x, y = values["x"], values["y"]
                        if x is None or y is None:
                            continue
                        else:
                            latexFile.append(latex_veh_name(veh, x * factor, y * factor, sem))
                    latexFile.append(latex_cluster(sem, self.intersections[strtime][sem]))
            outfile = target+"_"+str(time).zfill(pad)+".tex"
            d[outfile] = latexFile
        xmax = xmax * factor
        ymax = ymax * factor
        xmin = xmin * factor
        ymin = ymin * factor
        for outfile in d:
                d[outfile].append("\\tikz \\draw (" + str(xmin) + "," + str(ymin) + ") rectangle (" + str(xmax) + "," + str(ymax) + ");")
                d[outfile].append("\\end{tikzpicture}\n")
                d[outfile] = "\n".join(d[outfile])
        mainFile.append("\n\\newpage\n".join(d.values()))
        mainFile.append("\\end{document}")
        with open(target, "w") as f:
            f.write("\n".join(mainFile))

    def generate(self, target):
        print(self.network)
        net = SumoNetVis.Net(self.network)
        fig, ax = plt.subplots()
        fig.set_figheight(10)
        fig.set_figwidth(16)

        filenames = []
        net.plot(ax, style=USA_STYLE, stripe_width_scale=3)
        idx = 1
        for time in np.arange(self.trajectories.start, self.trajectories.end+self.trajectories.timestep, self.trajectories.timestep):
            self.trajectories.plot_points(time, ax, animate_color=True)
            f = str(time) + ".png"
            filenames.append(f)
            ax.scatter(self.semx, self.semy, color=self.semcol)
            plt.savefig(f)
            print(idx)
            idx = idx + 1

        images = []
        for filename in filenames:
            images.append(imageio.v2.imread(filename))
        imageio.mimsave(target, images)
        for filename in filenames:
            os.remove(filename)


if __name__ == '__main__':
    if len(sys.argv)>1:
        with open(sys.argv[1], "r") as stream:
            try:
                dynamic = True
                latex = False
                factor = 0.0
                if len(sys.argv)==3:
                    if (sys.argv[2] == "static"):
                        dynamic = False
                if len(sys.argv) == 4:
                    if (sys.argv[2] == "latex"):
                            latex = True
                            factor = float(sys.argv[3])
                d = yaml.safe_load(stream)
                cfg = os.path.abspath(d["sumo_configuration_file_path"])
                root = et.parse(cfg)
                text = root.getroot().find("input/net-file").attrib["value"]
                netFile = str(Path(cfg).parent / text)
                sumo_trace = str(os.path.abspath(d["trace_file"]))
                tracesMatch = str(Path(os.path.abspath(d["OsmosisOutput"])) / (d["experimentName"]+"_tracesMatch.json"))
                sem_pos = str(Path(os.path.abspath(d["OsmosisOutput"])) / (d["experimentName"]+"_tls.csv"))
                print(str(tracesMatch))
                obj = SumoToOsmosisGUI(netFile,
                                 sumo_trace,
                                 tracesMatch,
                                 sem_pos)
                if latex:
                    obj.generate_latex(os.path.abspath(d["SimulationOutGif"])+"_latex.tex", factor, d["maximum_tl_distance_in_meters"])
                elif dynamic:
                    print(os.path.abspath(d["SimulationOutGif"]))
                    obj.generate(os.path.abspath(d["SimulationOutGif"]))
                else:
                    obj.generate_static(os.path.abspath(d["SimulationOutGif"]+"_static.png"))
            except yaml.YAMLError as exc:
                print(exc)
    else:
        print("ERROR: you should provide the YAML configuration file in Java, so to generate the simulation output!")
