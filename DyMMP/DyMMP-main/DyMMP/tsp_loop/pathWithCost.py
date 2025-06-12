#!/usr/bin/env python3
# -*- coding: utf-8 -*-
#
# Copyright 2022 Giacomo Bergami
#
# This file is part of MaxCDijkstra
#
# This program is free software; you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation; either version 2 of the License, or
# (at your option) any later version.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with this program; if not, see <http://www.gnu.org/licenses/>.

class pathWithCost(object):
    def __init__(self, path, cost, nRecharges, vehicleChargeCapacity = 0, vehicleMaxCapacity = 0):
        self.path = path
        self.cost = cost
        self.nRecharges = nRecharges
        self.vehicleChargeCapacity = vehicleChargeCapacity
        self.vehicleMaxCapacity = vehicleMaxCapacity

    def __str__(self):
        return "path: " + str(self.path) + " with cost:" + str(self.cost) + " and greedy recharges: " + str(
            self.nRecharges)

    def setVehicleChargeCapacity(self, x):
        self.vehicleChargeCapacity = x
    def getVehicleChargeCapacity(self):
        return self.vehicleChargeCapacity

    def setVehicleMaxCapacity(self, x):
        self.vehicleMaxCapacity = x
    def getVehicleMaxCapacity(self):
        return self.vehicleMaxCapacity

    def extendWith(self, x):
        self.path.extend(x.getPath())
        self.cost = x.getCost()
        self.nRecharges = x.getNRecharges()
        self.vehicleMaxCapacity = x.getVehicleMaxCapacity()
        self.vehicleChargeCapacity = x.getVehicleChargeCapacity()

    def getNRecharges(self):
        return self.nRecharges

    def setNRecharges(self, x):
        self.nRecharges = x

    def getPath(self):
        return self.path

    def getCost(self):
        return self.cost

    def setPath(self, x):
        self.path = x

    def appendToPath(self, x):
        self.path.append(x)

    def setCost(self, c):
        self.cost = c

    def addToCost(self, c):
        self.cost = self.cost + c

    def print(self, g):
        return "path: " + str(self.path) + " with cost:" + str(self.cost) + " and greedy recharges: " + str(
            g.countTotalPowerUsedInRecharge(self.nRecharges)) + " from " + str(self.nRecharges) + " and vehicle arriving with energy "+str(self.vehicleChargeCapacity)+" out of maximum "+str(self.vehicleMaxCapacity)
