from re import M
import xml.etree.ElementTree as ET
import re
import sys

file1 = sys.argv[1]
file2 = sys.argv[2]

newfile1 = file1.replace(file1[file1.rindex('/')+1 : file1.find('.')], 'modified_' + file1[file1.rindex('/')+1 : file1.find('.')])
newfile2 = file2.replace(file2[file2.rindex('/')+1 : file2.find('.')], 'modified_' + file2[file2.rindex('/')+1 : file2.find('.')])

root = ET.parse(file1).getroot()

multiplier = float(sys.argv[3])

while (multiplier < 1):
    multiplier *= 10

for detector in root.iter('e1Detector'):
    original = re.search('freq="(.+?)"', ET.tostring(detector).decode("utf-8"))
    default = float(original[1])
    temp = float(default * multiplier)
    detector.set("freq", str(temp))

ET.ElementTree(root).write(newfile1) 

root = ET.parse(file2).getroot()

for tau in root.iter('vType'):
    original = re.search('tau="(.+?)"', ET.tostring(tau).decode("utf-8"))
    default = float(original[1])
    temp = float(default * multiplier)
    tau.set("tau", str(temp))

ET.ElementTree(root).write(newfile2) 