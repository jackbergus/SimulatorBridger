package uk.ncl.giacomobergami.SumoOsmosisBridger;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import uk.ncl.giacomobergami.utils.shared_data.iot.TimedIoT;

import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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

        double deltaTime = (args.length >= 2) ? Double.parseDouble(args[1]) : sb.getDeltaTime();
        sb.loopDuration = (args.length >= 3) ? Double.parseDouble(args[2]) : 10;

        List<TimedIoT> timedIoTList = makeExampleList();

        while (running) {
            running = sb.run(start+15, deltaTime, timedIoTList);
        }

        sb.fini(); //calls finish simulation and logging of results to database and csv files
    }
}
