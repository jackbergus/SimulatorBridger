package uk.ncl.giacomobergami.SumoOsmosisBridger;

import org.jooq.DSLContext;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;

import static uk.ncl.giacomobergami.utils.database.JavaPostGres.*;

public class MainExample {

   /*private static final String converter_out = "1_traffic_information_collector_output";
    private static final String converter_out_RSUCsvFile = "rsu.csv";
    private static final String converter_out_VehicleCsvFile = "vehicle.csv";
    private static final String orchestrator_out = "2_central_agent_oracle_output";
    private static final String orchestrator_out_rsuJsonFile = "rsu.json";
    private static final String orchestrator_out_vehicleJsonFile = "vehicle.json";
    private static final String orchestrator_out_output_stats_folder = "stats";
    private static final String orchestrator_out_output_experiment_name = "test";
    private static final String final_out = "3_extIOTSim_output";

    static {
        File file = new File("log4j2.xml");
        LoggerContext context = (LoggerContext) LogManager.getContext(false);
        context.setConfigLocation(file.toURI());
    }*/

    public static void main(String[] args) throws InterruptedException {
        SimulatorManager sb = new SimulatorManager();

        boolean running = true;
        double start = (args.length >= 1) ? Double.parseDouble(args[0]) : sb.getSimBegin();
        double fullEnd = (args.length >= 2) ? Double.parseDouble(args[1]) : sb.getSimEnd();
        double deltaTime = (args.length >= 3) ? Double.parseDouble(args[2]) : sb.getDeltaTime();
        double loopEnd = (args.length >= 4) ? Double.parseDouble(args[3]) : 5;

        sb.init(start, new ArrayList<>()); //does initialization and first loop

        while (running) { //second loop onwards
            running = sb.run(loopEnd, deltaTime, fullEnd, new ArrayList<>());
        }

        sb.fini(); //calls finish simulation and logging of results to database and csv files
    }

}
