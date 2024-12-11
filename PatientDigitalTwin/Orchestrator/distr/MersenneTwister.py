__author__ = "Giacomo Bergami"
__copyright__ = "Copyright 2024, SimulatorBridger"
__credits__ = ["Giacomo Bergami",
                    "https://github.com/yinengy/Mersenne-Twister-in-Python/blob/master/MT19937.py"]
__license__ = "GPL"
__version__ = "1.0.1"
__maintainer__ = "Giacomo Bergami"
__email__ = "bergamigiacomo@gmail.com"
__status__ = "Production"
# 
class MersenneTwister:
    def __init__(self, seed=0):
        self.w, self.n, self.m, self.r = (32, 624, 397, 31)
        self.a = 0x9908B0DF
        self.u, self.d = (11, 0xFFFFFFFF)
        self.s, self.b = (7, 0x9D2C5680)
        self.t, self.c = (15, 0xEFC60000)
        self.l = 18
        self.f = 1812433253
        # make a arry to store the state of the generator
        self.MT = [0 for i in range(self.n)]
        self.index = self.n + 1
        self.lower_mask = 0x7FFFFFFF  # (1 << r) - 1 // That is, the binary number of r 1's
        self.upper_mask = 0x80000000  # lowest w bits of (not lower_mask)
        self.MT[0] = seed
        for i in range(1, self.n):
            temp = self.f * (self.MT[i - 1] ^ (self.MT[i - 1] >> (self.w - 2))) + i
            self.MT[i] = temp & 0xffffffff


    def __iter__(self):
        return self

    def __next__(self):
        if self.index >= self.n:
            self._twist()
            self.index = 0

        y = self.MT[self.index]
        y = y ^ ((y >> self.u) & self.d)
        y = y ^ ((y << self.s) & self.b)
        y = y ^ ((y << self.t) & self.c)
        y = y ^ (y >> self.l)

        self.index += 1
        return (y & 0xffffffff) / (2 ** (self.w)-1)

    def forOffset(self, idx=-1):
        if idx>=0:
            self.index = idx
        return self.__next__()

    def _twist(self):
        for i in range(0, self.n):
            x = (self.MT[i] & self.upper_mask) + (self.MT[(i + 1) % self.n] & self.lower_mask)
            xA = x >> 1
            if (x % 2) != 0:
                xA = xA ^ self.a
            self.MT[i] = self.MT[(i + self.m) % self.n] ^ xA


if __name__ == '__main__':
    t = MersenneTwister()
    for x in range(200):
        print(next(t))
