__author__ = "Giacomo Bergami"
__copyright__ = "Copyright 2024, SimulatorBridger"
__credits__ = ["Giacomo Bergami"]
__license__ = "GPL"
__version__ = "1.0.1"
__maintainer__ = "Giacomo Bergami"
__email__ = "bergamigiacomo@gmail.com"
__status__ = "Production"
import statistics


class Gaussian:
    def __init__(self, mt, mu=0, sigma=1):
        self.mu = mu
        self.mt = mt
        self.sigma = sigma
        self.gauss_next = None

    def __next__(self):
        # random = self.random
        z = self.gauss_next
        self.gauss_next = None
        if z is None:
            import math
            x2pi = next(self.mt) * 6.283185307179586
            g2rad = math.sqrt(-2.0 * math.log(1.0 - next(self.mt)))
            z = math.cos(x2pi) * g2rad
            self.gauss_next = math.sin(x2pi) * g2rad

        return self.mu + z * self.sigma


if __name__ == '__main__':
    from Orchestrator.distr.MersenneTwister import MersenneTwister
    t = MersenneTwister()
    g = Gaussian(t)

    data = []

    for x in range(200000):
        data.append(next(g))

    # summarize
    print('mean=%.3f stdv=%.3f' % (statistics.mean(data), statistics.stdev(data)))
