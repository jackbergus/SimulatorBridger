package uk.ncl.giacomobergami.SumoOsmosisBridger;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import uk.ncl.giacomobergami.utils.shared_data.iot.TimedIoT;

import java.io.FileReader;
import java.io.IOException;
import java.sql.Time;
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

        sb.init(start, new ArrayList<>()); //does initialization and first loop

        double fullEnd = (args.length >= 2) ? Double.parseDouble(args[1]) : sb.getSimEnd();
        double deltaTime = (args.length >= 3) ? Double.parseDouble(args[2]) : sb.getDeltaTime();
        sb.loopEndTime = (args.length >= 4) ? Double.parseDouble(args[3]) : 10;
        double injectionTime = (args.length >= 5) ? Double.parseDouble(args[4]) : 5; //simulation time at which new data will be injected

        List<TimedIoT> timedIoTList = makeExampleList();

        while (running) { //second loop onwards
            running = sb.run(deltaTime, fullEnd, injectionTime, timedIoTList);
        }

        sb.fini(); //calls finish simulation and logging of results to database and csv files
    }
}
