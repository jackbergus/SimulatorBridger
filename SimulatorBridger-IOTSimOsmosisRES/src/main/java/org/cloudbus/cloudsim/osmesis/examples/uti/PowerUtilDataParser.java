package org.cloudbus.cloudsim.osmesis.examples.uti;

import com.opencsv.CSVWriter;
import org.cloudbus.cloudsim.sdn.SDNHost;
import org.cloudbus.cloudsim.sdn.power.PowerUtilizationHistoryEntry;
import org.xml.sax.Attributes;

import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class PowerUtilDataParser {

    private List<SDNHost> hostList;

    public PowerUtilDataParser (String fileName, List<SDNHost> hostList) {
        CSVFilePath = fileName;
        this.hostList = hostList;
        startDocument(fileName);
    }

    private StringBuilder elementValue;
    public void characters(char[] ch, int start, int length) {
        if (elementValue == null) {
            elementValue = new StringBuilder();
        } else {
            elementValue.append(ch, start, length);
        }
    }

    CSVWriter writer = null;
    String CSVFilePath;
    public void startDocument(String csvPath) {
        try {
            writer = new CSVWriter(new FileWriter(csvPath));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        String[] headers = {"dcName", "name", "startTime", "usedMips"};
        writer.writeNext(headers);
    }

    public void startElement(String dcName, String name, List<PowerUtilizationHistoryEntry> utilizationHistory) {
        for(PowerUtilizationHistoryEntry uhe : utilizationHistory) {
            toPUHECSV(new PrintResults.ActualPowerUtilizationHistoryEntry(dcName, name, uhe));
        }
    }

    private void toPUHECSV(PrintResults.ActualPowerUtilizationHistoryEntry PUHE) {
        String[] data = {String.valueOf(PUHE.dcName), String.valueOf(PUHE.name), String.valueOf(PUHE.startTime), String.valueOf(PUHE.usedMips)};
        writer.writeNext(data);
    }

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

}
