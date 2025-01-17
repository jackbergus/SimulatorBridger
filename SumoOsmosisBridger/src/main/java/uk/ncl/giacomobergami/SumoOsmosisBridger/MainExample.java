package uk.ncl.giacomobergami.SumoOsmosisBridger;

import uk.ncl.giacomobergami.utils.shared_data.iot.TimedIoT;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static java.lang.Double.parseDouble;

public class MainExample {

    public static void main(String[] args) {
        SimulatorManager sb = new SimulatorManager();
        boolean running = true;
        double start = (args.length >= 1) ? parseDouble(args[0]) : 0;
        sb.init(start, new ArrayList<>());
        double deltaTime = (args.length >= 2) ? parseDouble(args[1]) : 0.075;
        sb.loopDuration = (args.length >= 3) ? parseDouble(args[2]) : sb.getCurrentLatency();

        List<TimedIoT> timedIoTList = sb.parseJSONHealthData("PatientDigitalTwin/patient.json");
        while (running) {
            double loopStart = start;
            List<TimedIoT> currentDelta = timedIoTList.stream().filter(x -> (x.simtime >= loopStart) && (x.simtime < loopStart + sb.loopDuration)).collect(Collectors.toList());
            for (TimedIoT timedIoT : currentDelta) {
                timedIoTList.remove(timedIoT);
            }
            start = sb.run(loopStart, deltaTime, currentDelta); //providing the current time interval, as a starting time and a delta time
            running = start < sb.getSimEnd(); //explicitly incrementing the start time to the next slot
        }

        sb.fini(); //calls finish simulation and logging of results to database and csv files
    }
}