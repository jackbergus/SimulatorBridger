import random, csv

path = 'C:/Users/rohin/SimulatorBridger/SimulatorBridger/clean_example/1_traffic_information_collector_output/vehicle.csv' #filepath for output csv from SB
data = []; #initialize data array
per = 80; #percentage to delete
i = 0; #initialize iterator variable

with open(path, "r", newline='\n') as csvfile: #count rows in csv file
    lines = csv.reader(csvfile, delimiter=',')
    count = sum(1 for rows in lines)

f = random.sample(range(0, count), int((1 - (per/100)) * count)) #select random rows to keep
if 0 not in f : f.append(0)
f.sort()

with open(path, "r", newline='\n') as csvfile: #add data from csv to data array
    lines = csv.reader(csvfile, delimiter=',')        
    for row in lines:
        data.append(row)

with open('example-injection-data.csv', 'w', newline='\n') as csvfile: #write data from rows matching the randomly chosen rows to new csv
    writer = csv.writer(csvfile, delimiter=',')
    for row in data:
        if i in f: writer.writerow(row);
        i = i + 1