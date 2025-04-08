package uk.ncl.giacomobergami.SumoOsmosisBridger.traffic_converter;

import com.opencsv.CSVWriter;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;
import uk.ncl.giacomobergami.utils.shared_data.iot.TimedIoT;

import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class BaseCollectorParser extends DefaultHandler {
    private static final String TIMESTEP = "timestep";
    private static final String VEHICLE = "vehicle";
//    private SUMOData SD;
    private StringBuilder elementValue;
    static List<Double> temporalOrdering;
    double timestep = 0;
    CSVWriter writer = null;
    String CSVFilePath;
    static HashMap<String, TimedIoT> FirstEntry = new HashMap<>();
    static HashMap<String, TimedIoT> SecondEntry = new HashMap<>();
    static TreeSet<Double> wakeUpTimes = new TreeSet<>();

    public BaseCollectorParser(List<Double> temporalOrdering, String vehicleCSVFile) {
            BaseCollectorParser.temporalOrdering = temporalOrdering;
            CSVFilePath = vehicleCSVFile;
    }

    @Override
    public void characters(char[] ch, int start, int length) {
        if (elementValue == null) {
            elementValue = new StringBuilder();
        } else {
            elementValue.append(ch, start, length);
        }
    }

    @Override
    public void startDocument() {
//        SD = new SUMOData();
        try {
            writer = new CSVWriter(new FileWriter(CSVFilePath));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        String[] headers = {"id", "x", "y", "angle", "type", "speed", "pos", "lane", "slope", "simtime", "injected", "batterydepletion", "usebattery", "packetsize", "usepacketinfo"};
        writer.writeNext(headers);
    }

    void addTimestamp(double thisTimestamp) {
        temporalOrdering.add(thisTimestamp);
    }

    void addIoTDevice(TimedIoT TI) {
        wakeUpTimes.add(TI.simtime);
        toTimedIoTCSV(TI, writer);
        if (SecondEntry.containsKey(TI.getId())) {
            return;
        }
        if (FirstEntry.containsKey(TI.getId())) {
            SecondEntry.putIfAbsent(TI.getId(), TI);
            return;
        }
        FirstEntry.putIfAbsent(TI.getId(), TI);
    }

    @Override
    public void startElement(String uri, String lName, String qName, Attributes attr) {
    }

    private static void toTimedIoTCSV(TimedIoT vehicle, CSVWriter writer) {
        String[] data = {String.valueOf(vehicle.getId()), String.valueOf(vehicle.getX()), String.valueOf(vehicle.getY()), String.valueOf(vehicle.getAngle()),
                String.valueOf(vehicle.getType()), String.valueOf(vehicle.getSpeed()), String.valueOf(vehicle.getPos()), String.valueOf(vehicle.getLane()),
                String.valueOf(vehicle.getSlope()), String.valueOf(vehicle.getSimtime()), String.valueOf(vehicle.isInjected()),
                String.valueOf(vehicle.getBatteryDepletion()), String.valueOf(vehicle.isUseBattery()), String.valueOf(vehicle.getPacketSize()),String.valueOf(vehicle.isUsePacket())};
        writer.writeNext(data);
    }

    @Override
    public void endDocument() {
        try {
            writer.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        try {
            writer.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static HashMap<String, TimedIoT> getFirstEntry() {
        return FirstEntry;
    }

    public static HashMap<String, TimedIoT> getSecondEntry() {
        return SecondEntry;
    }

    public static TreeSet<Double> getWakeUpTimes() {
        return wakeUpTimes;
    }
}
