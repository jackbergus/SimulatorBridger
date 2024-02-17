package uk.ncl.giacomobergami.SumoOsmosisBridger.traffic_converter;

import com.opencsv.CSVWriter;
import me.tongfei.progressbar.ProgressBar;
import org.jooq.DSLContext;
import org.jooq.Result;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;
import uk.ncl.giacomobergami.components.iot.IoTDeviceTabularConfiguration;
import uk.ncl.giacomobergami.components.iot.IoTEntityGenerator;
import uk.ncl.giacomobergami.utils.data.YAML;
import uk.ncl.giacomobergami.utils.database.jooq.tables.Vehinformation;
import uk.ncl.giacomobergami.utils.database.jooq.tables.records.VehinformationRecord;
import uk.ncl.giacomobergami.utils.shared_data.iot.TimedIoT;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Time;
import java.util.*;

public class SUMODataParser extends DefaultHandler {
    private static final String TIMESTEP = "timestep";
    private static final String VEHICLE = "vehicle";
    private SUMOData SD;
    private StringBuilder elementValue;
    static List<Double> temporalOrdering;
    double timestep = 0;
    CSVWriter writer = null;
    String CSVFilePath;
    static HashMap<String, TimedIoT> FirstEntry = new HashMap<>();
    static HashMap<String, TimedIoT> SecondEntry = new HashMap<>();
    static TreeSet<Double> wakeUpTimes = new TreeSet<>();

    public SUMODataParser(List<Double> temporalOrdering, String vehicleCSVFile) {
            SUMODataParser.temporalOrdering = temporalOrdering;
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
        SD = new SUMOData();
        try {
            writer = new CSVWriter(new FileWriter(CSVFilePath));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        String[] headers = {"id", "x", "y", "angle", "type", "speed", "pos", "lane", "slope", "simtime"};
        writer.writeNext(headers);
    }

    @Override
    public void startElement(String uri, String lName, String qName, Attributes attr) {
        switch (qName) {
            case TIMESTEP:
                SD.setSUMOData(new ArrayList<>());
                for (int i = 0; i < attr.getLength(); i++) {
                    if (Objects.equals(attr.getLocalName(i), "time")) {
                        timestep = Double.parseDouble(attr.getValue(i));
                        temporalOrdering.add(timestep);
                    }
                }
                break;
            case VEHICLE:
                TimedIoT TI = new TimedIoT();
                TI.setId(attr.getValue(0));
                TI.setX(Double.parseDouble(attr.getValue(1)));
                TI.setY(Double.parseDouble(attr.getValue(2)));
                TI.setAngle(Double.parseDouble(attr.getValue(3)));
                TI.setType(attr.getValue(4));
                TI.setSpeed(Double.parseDouble(attr.getValue(5)));
                TI.setPos(Double.parseDouble(attr.getValue(6)));
                TI.setLane(attr.getValue(7));
                TI.setSlope(Double.parseDouble(attr.getValue(8)));
                /*for (int i = 0; i < attr.getLength(); i++) {
                    if (Objects.equals(attr.getLocalName(i), "id")) {
                        TI.setId(attr.getValue(i));
                    } else if (Objects.equals(attr.getLocalName(i), "x")) {
                        TI.setX(Double.parseDouble(attr.getValue(i)));
                    } else if (Objects.equals(attr.getLocalName(i), "y")) {
                        TI.setY(Double.parseDouble(attr.getValue(i)));
                    } else if (Objects.equals(attr.getLocalName(i), "angle")) {
                        TI.setAngle(Double.parseDouble(attr.getValue(i)));
                    } else if (Objects.equals(attr.getLocalName(i), "type")) {
                        TI.setType(attr.getValue(i));
                    } else if (Objects.equals(attr.getLocalName(i), "speed")) {
                        TI.setSpeed(Double.parseDouble(attr.getValue(i)));
                    } else if (Objects.equals(attr.getLocalName(i), "pos")) {
                        TI.setPos(Double.parseDouble(attr.getValue(i)));
                    } else if (Objects.equals(attr.getLocalName(i), "lane")) {
                        TI.setLane(attr.getValue(i));
                    } else if (Objects.equals(attr.getLocalName(i), "slope")) {
                        TI.setSlope(Double.parseDouble(attr.getValue(i)));
                    }*/
                TI.setSimtime(timestep);
                wakeUpTimes.add(timestep);
                SD.sdAddTo(TI);
                toTimedIoTCSV(TI, writer);
                if (SecondEntry.containsKey(TI.getId())) {
                    break;
                }
                if (FirstEntry.containsKey(TI.getId())) {
                    SecondEntry.putIfAbsent(TI.getId(), TI);
                    break;
                }
                FirstEntry.putIfAbsent(TI.getId(), TI);
                break;
        }
    }

    private static void toTimedIoTCSV(TimedIoT vehicle, CSVWriter writer) {
        String[] data = {String.valueOf(vehicle.getId()), String.valueOf(vehicle.getX()), String.valueOf(vehicle.getY()), String.valueOf(vehicle.getAngle()), String.valueOf(vehicle.getType()), String.valueOf(vehicle.getSpeed()), String.valueOf(vehicle.getPos()), String.valueOf(vehicle.getLane()), String.valueOf(vehicle.getSlope()), String.valueOf(vehicle.getSimtime())};
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
