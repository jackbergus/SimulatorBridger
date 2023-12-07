package uk.ncl.giacomobergami.SumoOsmosisBridger.traffic_converter;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;
import uk.ncl.giacomobergami.utils.shared_data.iot.TimedIoT;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class SUMODataParser extends DefaultHandler {
    private static final String TIMESTEP = "timestep";
    private static final String VEHICLE = "vehicle";

    private SUMOData SD;
    private StringBuilder elementValue;
    static List<Double> temporalOrdering;
    static HashMap<Double, List<TimedIoT>> timedIoTDevices;
    double timestep = 0;

    public SUMODataParser(List<Double> temporalOrdering, HashMap<Double, List<TimedIoT>> timedIoTDevices) {
            this.temporalOrdering = temporalOrdering;
            this.timedIoTDevices = timedIoTDevices;
    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        if (elementValue == null) {
            elementValue = new StringBuilder();
        } else {
            elementValue.append(ch, start, length);
        }
    }

    @Override
    public void startDocument() throws SAXException {
        SD = new SUMOData();
    }

    @Override
    public void startElement(String uri, String lName, String qName, Attributes attr) throws SAXException {
        switch (qName) {
            case TIMESTEP:
                SD.setSUMOData(new ArrayList<>());
                for (int i = 0; i < attr.getLength(); i++) {
                    if (attr.getLocalName(i) == "time") {
                        timestep = Double.parseDouble(attr.getValue(i));
                        temporalOrdering.add(timestep);
                    }
                }
                break;
            case VEHICLE:
                TimedIoT TI = new TimedIoT();
                for (int i = 0; i < attr.getLength(); i++) {
                    if (attr.getLocalName(i) == "id") {
                        TI.setId(attr.getValue(i));
                    } else if (attr.getLocalName(i) == "x") {
                        TI.setX(Double.parseDouble(attr.getValue(i)));
                    } else if (attr.getLocalName(i) == "y") {
                        TI.setY(Double.parseDouble(attr.getValue(i)));
                    } else if (attr.getLocalName(i) == "angle") {
                        TI.setAngle(Double.parseDouble(attr.getValue(i)));
                    } else if (attr.getLocalName(i) == "type") {
                        TI.setType(attr.getValue(i));
                    } else if (attr.getLocalName(i) == "speed") {
                        TI.setSpeed(Double.parseDouble(attr.getValue(i)));
                    } else if (attr.getLocalName(i) == "pos") {
                        TI.setPos(Double.parseDouble(attr.getValue(i)));
                    } else if (attr.getLocalName(i) == "lane") {
                        TI.setLane(attr.getValue(i));
                    } else if (attr.getLocalName(i) == "slope") {
                        TI.setSlope(Double.parseDouble(attr.getValue(i)));
                    }
                    TI.setSimtime(timestep);
                }
                SD.sdAddTo(TI);
                this.timedIoTDevices.put(timestep, SD.getSUMOData());
                break;
        }
    }

    /*@Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        switch (qName) {
            case TIMESTEP:

    }*/

    public static List<Double> getTemporalOrdering() {
        return temporalOrdering;
    }

    public static HashMap<Double, List<TimedIoT>> getTimedIoTDevices() {
        return timedIoTDevices;
    }
}
