from re import M
import xml.etree.ElementTree as ET
import re
import sys
from pathlib import Path

file1 = sys.argv[1]
file2 = sys.argv[2]
cfgpath = sys.argv[3]

newfile1 = file1.replace(file1[file1.rindex(
    '/')+1: file1.find('.')], 'modified_' + file1[file1.rindex('/')+1: file1.find('.')])
newfile2 = file2.replace(file2[file2.rindex(
    '/')+1: file2.find('.')], 'modified_' + file2[file2.rindex('/')+1: file2.find('.')])

root = ET.parse(file1).getroot()

multiplier = float(sys.argv[4])

while (multiplier < 1):
    multiplier *= 10

#current freq is 1800 so divide by factor of 1.8
factor = 1.8

for detector in root.iter('e1Detector'):
    original = re.search('freq="(.+?)"', ET.tostring(detector).decode("utf-8"))
    default = float(original[1])
    temp = float(default / factor * multiplier)
    detector.set("freq", str(temp))

ET.ElementTree(root).write(newfile1)

root = ET.parse(file2).getroot()

for tau in root.iter('vType'):
    if (re.search('tau="(.+?)"', ET.tostring(tau).decode("utf-8")) == None):
        tau.set('tau', "1.0")

for tau in root.iter('vType'):
    original = re.search('tau="(.+?)"', ET.tostring(tau).decode("utf-8"))
    default = float(original[1])
    temp = float(default * multiplier)
    tau.set("tau", str(temp))

ET.ElementTree(root).write(newfile2)

cfgroot = ET.parse(cfgpath).getroot()
edit = False

for afv in cfgroot.iter('additional-files'):
    original = re.search('value="(.+?)"', ET.tostring(afv).decode("utf-8"))
    org = str(original[1])
    if not (org.__contains__('modified')):
        edit = True
        change1 = org.replace(file1[file1.rindex(
            '/')+1: file1.find('.')], 'modified_' + file1[file1.rindex('/')+1: file1.find('.')])
        change2 = change1.replace(file2[file2.rindex(
            '/')+1: file2.find('.')], 'modified_' + file2[file2.rindex('/')+1: file2.find('.')])

if(edit):
    cfgfile = Path(cfgpath)
    cfgfile.write_text(cfgfile.read_text().replace(org, change2))
