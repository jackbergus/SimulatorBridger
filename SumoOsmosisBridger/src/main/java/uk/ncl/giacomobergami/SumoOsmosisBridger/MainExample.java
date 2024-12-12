package uk.ncl.giacomobergami.SumoOsmosisBridger;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import com.opencsv.exceptions.CsvException;
import uk.ncl.giacomobergami.utils.shared_data.iot.TimedIoT;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Time;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MainExample {

    public static List<TimedIoT> makeExampleList() throws IOException, CsvException {
        List<TimedIoT> timedIoTList = new ArrayList<>();
        String vehicleCSVFile = "example-injection-data.csv";
        CSVReader reader = new CSVReader(new FileReader(vehicleCSVFile));
        List<String[]> csvBody = reader.readAll();
        for (int i = 1; i < csvBody.size(); i++) {
            String[] strings = csvBody.get(i);
            TimedIoT TI = getTimedIoT(strings);
            timedIoTList.add(TI);
        }
        return timedIoTList;
    }

    public static List<TimedIoT> generateHealthData(double endTime) throws IOException, CsvException {
        Random rand = new Random();
        List<TimedIoT> healthData = new ArrayList<>();
        String CSVFile = "clean_example/1_traffic_information_collector_output/rsu.csv";
        CSVReader reader = new CSVReader(new FileReader(CSVFile));
        int noEdges = 16;
        int randNo = rand.nextInt((noEdges-1)+1);
        String[] csvBody = reader.readNext();
        for (int i = 1; i < randNo; i++) {
            csvBody = reader.readNext();
        }
        String[] info = new String[10];
        info[0] = "Emergency_Data";
        info[1] = String.valueOf(csvBody[0]);
        info[2] = String.valueOf(csvBody[1]);
        info[3] = String.valueOf(0);
        info[4] = "n/a";
        info[5] = String.valueOf(0);
        info[6] = String.valueOf(0);
        info[7] = "n/a";
        info[8] = String.valueOf(0);
        for (double j = 0; j < endTime; j+=0.001) {
            info[9] = String.valueOf(j);
            TimedIoT TI = getTimedIoT(info);
            healthData.add(TI);
        }
        return healthData;
    }

    private static TimedIoT getTimedIoT(String[] strings) {
        TimedIoT TI = new TimedIoT();
        TI.setId(strings[0] + "_injected");
        TI.setX(Double.parseDouble(strings[1]));
        TI.setY(Double.parseDouble(strings[2]));
        TI.setAngle(Double.parseDouble(strings[3]));
        TI.setType(strings[4]);
        TI.setSpeed(Double.parseDouble(strings[5]));
        TI.setPos(Double.parseDouble(strings[6]));
        TI.setLane(strings[7]);
        TI.setSlope(Double.parseDouble(strings[8]));
        TI.setSimtime(Double.parseDouble(strings[9]));
        TI.setInjected(true);
        return TI;
    }

    public static void main(String[] args) throws IOException, CsvException {
        SimulatorManager sb = new SimulatorManager();

        boolean running = true;
        double start = (args.length >= 1) ? Double.parseDouble(args[0]) : 0;

        sb.init(start, new ArrayList<>());

        double deltaTime = (args.length >= 2) ? Double.parseDouble(args[1]) : 0.001;
        sb.loopDuration = (args.length >= 3) ? Double.parseDouble(args[2]) : 10;

        List<TimedIoT> timedIoTList = makeExampleList();
        //List<TimedIoT> healthData = generateHealthData(sb.simEnd);

        while (running) {
            running = sb.run(start+15, deltaTime, timedIoTList);
        }

        sb.fini(); //calls finish simulation and logging of results to database and csv files
    }
}