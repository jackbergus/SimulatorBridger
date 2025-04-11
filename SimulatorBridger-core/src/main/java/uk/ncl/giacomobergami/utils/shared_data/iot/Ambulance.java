package uk.ncl.giacomobergami.utils.shared_data.iot;

import org.jooq.DSLContext;
import org.jooq.Result;
import org.xml.sax.Attributes;
import uk.ncl.giacomobergami.utils.database.jooq.tables.Ambulanceinformation;
import uk.ncl.giacomobergami.utils.database.jooq.tables.records.AmbulanceinformationRecord;
import java.util.HashMap;

public class Ambulance extends TimedIoT {

    public static Ambulance addNewAmbulance(Attributes attr, double timestep) {
        Ambulance ambulance = new Ambulance();
        ambulance.setId(attr.getValue(0));
        ambulance.setX(Double.parseDouble(attr.getValue(1)));
        ambulance.setY(Double.parseDouble(attr.getValue(2)));
        ambulance.setAngle(Double.parseDouble(attr.getValue(3)));
        ambulance.setType(attr.getValue(4));
        ambulance.setSpeed(Double.parseDouble(attr.getValue(5)));
        ambulance.setPos(Double.parseDouble(attr.getValue(6)));
        ambulance.setLane(attr.getValue(7));
        ambulance.setSlope(Double.parseDouble(attr.getValue(8)));
        ambulance.setSimtime(timestep);
        ambulance.setInjected(false);
        return ambulance;
    }

    public static Result<AmbulanceinformationRecord> collectAmbulanceData(DSLContext context, Double simTime, Double deltaTime) {
        Result<AmbulanceinformationRecord> dataRange = context.select(Ambulanceinformation.AMBULANCEINFORMATION.VEHICLE_ID, Ambulanceinformation.AMBULANCEINFORMATION.X, Ambulanceinformation.AMBULANCEINFORMATION.Y, Ambulanceinformation.AMBULANCEINFORMATION.SIMTIME, Ambulanceinformation.AMBULANCEINFORMATION.INJECTED).from(Ambulanceinformation.AMBULANCEINFORMATION).where("simtime between " + (simTime - deltaTime / 2) + " and " + (simTime + deltaTime / 2)).orderBy(Ambulanceinformation.AMBULANCEINFORMATION.SIMTIME).fetchInto(Ambulanceinformation.AMBULANCEINFORMATION);
        return dataRange;
    }

    public static HashMap<String, String> ambulancesToCollectPatients(Result<AmbulanceinformationRecord> dataRange, double x, double y, double dist, String id) {
        HashMap<String, String> patientAmbulance = new HashMap<>();
        for (AmbulanceinformationRecord amb : dataRange) {
            if (distance(amb.get(Ambulanceinformation.AMBULANCEINFORMATION.X), amb.get(Ambulanceinformation.AMBULANCEINFORMATION.Y), x, y) < dist) {
                if (amb.get(Ambulanceinformation.AMBULANCEINFORMATION.VEHICLE_ID).contains("from"))
                    patientAmbulance.putIfAbsent(id, amb.get(Ambulanceinformation.AMBULANCEINFORMATION.VEHICLE_ID));
                break;
            }
        }
        return patientAmbulance;
    }

    public static Result<AmbulanceinformationRecord> retrieveAmbulancesWithPatients(DSLContext context, String id, double simTime, double deltaTime, HashMap<String, String> patientAmbulance) {
        Result<AmbulanceinformationRecord> attachedVehicles = context.select(Ambulanceinformation.AMBULANCEINFORMATION.VEHICLE_ID, Ambulanceinformation.AMBULANCEINFORMATION.X, Ambulanceinformation.AMBULANCEINFORMATION.Y, Ambulanceinformation.AMBULANCEINFORMATION.SIMTIME, Ambulanceinformation.AMBULANCEINFORMATION.INJECTED).from(Ambulanceinformation.AMBULANCEINFORMATION).where("simtime between " + (simTime - deltaTime / 2) + " and " + (simTime + deltaTime / 2) + " AND vehicle_id ='" + patientAmbulance.get(id) + "'").orderBy(Ambulanceinformation.AMBULANCEINFORMATION.SIMTIME).fetchInto(Ambulanceinformation.AMBULANCEINFORMATION);
        return attachedVehicles;
    }

    private static double distance(double x1, double y1, double x2, double y2) {
        double ac = Math.abs(y2 - y1);
        double cb = Math.abs(x2 - x1);
        return Math.hypot(ac, cb);
    }
}
