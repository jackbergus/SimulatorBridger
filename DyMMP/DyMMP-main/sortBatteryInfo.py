from lxml import etree

def sortBatteryInfo(delta):

    print("Sorting SUMO Battery Information...")

    path = "C:\\Users\\rohin\\SimulatorBridger\\SimulatorBridger\\DyMMP\\DyMMP-main\\batteryoutput.xml"
    csvpath = "C:\\Users\\rohin\\SimulatorBridger\\SimulatorBridger\\clean_example\\3_extIOTSim_output\\SUMOBatteryInfo.csv"

    context = etree.iterparse(path, events=("start", "end"))
    context = iter(context)
    event, root = context.__next__()

    import csv
    file = open(csvpath, 'w+', newline ='')

    with file:
        header = ['vehicleID', 'energyConsumed', 'time']
        writer = csv.DictWriter(file, fieldnames = header)
        
        writer.writeheader()
        for event, elem in context:
            if (event == "end" and elem.tag == "timestep"):
                time = str(elem.attrib["time"])
                if float(time) % delta == 0:
                    print("At time = " + time)
                for vehicle in elem:
                    if str(vehicle.attrib['id']).__contains__("ambulance"):
                        vehId = str(vehicle.attrib['id'])
                        energyConsumed = str(vehicle.attrib['energyConsumed'])
                        line = {'vehicleID' : vehId, 'energyConsumed' : energyConsumed, 'time' : time}
                        writer.writerow(line)
        file.close()
    print("Done Information Sorted!")
