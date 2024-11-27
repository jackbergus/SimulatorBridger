package uk.ncl.giacomobergami.SumoOsmosisBridger;

import com.opencsv.exceptions.CsvException;

import java.io.IOException;
import java.util.ArrayList;

public class MainExample {

    public static void main(String[] args) throws IOException, CsvException {
        SimulatorManager sb = new SimulatorManager();

        boolean running = true;
        double start = (args.length >= 1) ? Double.parseDouble(args[0]) : 0;

        sb.init(start, new ArrayList<>()); //does initialization and first loop

        double fullEnd = (args.length >= 2) ? Double.parseDouble(args[1]) : sb.getSimEnd();
        double deltaTime = (args.length >= 3) ? Double.parseDouble(args[2]) : sb.getDeltaTime();
        sb.loopEndTime = (args.length >= 4) ? Double.parseDouble(args[3]) : 10;
        double injectionTime = (args.length >= 5) ? Double.parseDouble(args[4]) : 5; //simulation time at which new data will be injected

        while (running) { //second loop onwards
            running = sb.run(deltaTime, fullEnd, injectionTime);
        }

        sb.fini(); //calls finish simulation and logging of results to database and csv files
    }

}
