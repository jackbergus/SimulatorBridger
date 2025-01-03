package uk.ncl.giacomobergami.SumoOsmosisBridger;

import com.opencsv.exceptions.CsvException;
import uk.ncl.giacomobergami.utils.shared_data.iot.TimedIoT;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

import static java.lang.Double.parseDouble;

public class MainExample {

    public static void main(String[] args) {

        SimulatorManager sb = new SimulatorManager();

        boolean running = true;
        double start = (args.length >= 1) ? parseDouble(args[0]) : 0;

        sb.init(start, new ArrayList<>());

        double deltaTime = (args.length >= 2) ? parseDouble(args[1]) : 0.001;
        sb.loopDuration = (args.length >= 3) ? parseDouble(args[2]) : 10;

        List<TimedIoT> timedIoTList = new ArrayList<>();
        while (running) {
            running = sb.run(start, deltaTime, timedIoTList);
        }

        sb.fini(); //calls finish simulation and logging of results to database and csv files
    }
}