package uk.ncl.giacomobergami.utils.shared_data.dft;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import uk.ncl.giacomobergami.utils.shared_data.abstracted.TimedObject;
import uk.ncl.giacomobergami.utils.structures.ImmutablePair;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

@JsonPropertyOrder({
        "Count_point_id",
        "Direction_of_travel",
        "Year",
        "Count_date",
        "hour",
        "Region_id",
        "Region_name",
        "Region_ons_code",
        "Local_authority_id",
        "Local_authority_name",
        "Local_authority_code",
        "Road_name",
        "Road_category",
        "Road_type",
        "Start_junction_road_name",
        "End_junction_road_name",
        "Easting",
        "Northing",
        "Latitude",
        "Longitude",
        "Link_length_km",
        "Link_length_miles",
        "Pedal_cycles",
        "Two_wheeled_motor_vehicles",
        "Cars_and_taxis",
        "Buses_and_coaches",
        "LGVs",
        "HGVs_2_rigid_axle",
        "HGVs_3_rigid_axle",
        "HGVs_4_or_more_rigid_axle",
        "HGVs_3_or_4_articulated_axle",
        "HGVs_5_articulated_axle",
        "HGVs_6_articulated_axle",
        "All_HGVs",
        "All_motor_vehicles"
})
public class DfTEntry implements TimedObject<DfTEntry>, Comparable<DfTEntry> {
    private static final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @JsonProperty("Count_point_id")
    public int Count_point_id;

    @JsonProperty("Direction_of_travel")
    public String Direction_of_travel; //NSWE

    @JsonProperty("Year")
    public int Year;

    @JsonProperty("Count_date")
    public String Count_date;

    @JsonProperty("hour")
    public int hour;

    @JsonProperty("Region_id")
    public int Region_id;

    @JsonProperty("Region_name")
    public String Region_name;

    @JsonProperty("Region_ons_code")
    public String Region_ons_code;

    @JsonProperty("Local_authority_id")
    public int Local_authority_id;

    @JsonProperty("Local_authority_name")
    public String Local_authority_name;

    @JsonProperty("Local_authority_code")
    public String Local_authority_code;

    @JsonProperty("Road_name")
    public String Road_name;

    @JsonProperty("Road_category")
    public String Road_category;

    @JsonProperty("Road_type")
    public String Road_type;

    @JsonProperty("Start_junction_road_name")
    public String Start_junction_road_name;

    @JsonProperty("End_junction_road_name")
    public String End_junction_road_name;

    @JsonProperty("Easting")
    public long Easting;

    @JsonProperty("Northing")
    public long Northing;

    @JsonProperty("Latitude")
    public double Latitude;

    @JsonProperty("Longitude")
    public double Longitude;

    @JsonProperty("Link_length_km")
    public double Link_length_km;

    @JsonProperty("Link_length_miles")
    public double Link_length_miles;

    @JsonProperty("Pedal_cycles")
    public int Pedal_cycles;

    @JsonProperty("Two_wheeled_motor_vehicles")
    public int Two_wheeled_motor_vehicles;

    @JsonProperty("Cars_and_taxis")
    public int Cars_and_taxis;

    @JsonProperty("Buses_and_coaches")
    public int Buses_and_coaches;

    @JsonProperty("LGVs")
    public int LGVs;

    @JsonProperty("HGVs_2_rigid_axle")
    public int HGVs_2_rigid_axle;

    @JsonProperty("HGVs_3_rigid_axle")
    public int HGVs_3_rigid_axle;

    @JsonProperty("HGVs_4_or_more_rigid_axle")
    public int HGVs_4_or_more_rigid_axle;

    @JsonProperty("HGVs_3_or_4_articulated_axle")
    public int HGVs_3_or_4_articulated_axle;

    @JsonProperty("HGVs_5_articulated_axle")
    public int HGVs_5_articulated_axle;

    @JsonProperty("HGVs_6_articulated_axle")
    public int HGVs_6_articulated_axle;

    @JsonProperty("All_HGVs")
    public int All_HGVs;

    @JsonProperty("All_motor_vehicles")
    public int All_motor_vehicles;

    public DfTEntry() {}

    public int getCount_point_id() {
        return Count_point_id;
    }

    public void setCount_point_id(int count_point_id) {
        Count_point_id = count_point_id;
    }

    public String getDirection_of_travel() {
        return Direction_of_travel;
    }

    public void setDirection_of_travel(String direction_of_travel) {
        Direction_of_travel = direction_of_travel;
    }

    public int getYear() {
        return Year;
    }

    public void setYear(int year) {
        Year = year;
    }

    public String getCount_date() {
        return Count_date;
    }

    public void setCount_date(String count_date) {
        Count_date = count_date;
    }

    public int getHour() {
        return hour;
    }

    public void setHour(int hour) {
        this.hour = hour;
    }

    public int getRegion_id() {
        return Region_id;
    }

    public void setRegion_id(int region_id) {
        Region_id = region_id;
    }

    public String getRegion_name() {
        return Region_name;
    }

    public void setRegion_name(String region_name) {
        Region_name = region_name;
    }

    public String getRegion_ons_code() {
        return Region_ons_code;
    }

    public void setRegion_ons_code(String region_ons_code) {
        Region_ons_code = region_ons_code;
    }

    public int getLocal_authority_id() {
        return Local_authority_id;
    }

    public void setLocal_authority_id(int local_authority_id) {
        Local_authority_id = local_authority_id;
    }

    public String getLocal_authority_name() {
        return Local_authority_name;
    }

    public void setLocal_authority_name(String local_authority_name) {
        Local_authority_name = local_authority_name;
    }

    public String getLocal_authority_code() {
        return Local_authority_code;
    }

    public void setLocal_authority_code(String local_authority_code) {
        Local_authority_code = local_authority_code;
    }

    public String getRoad_name() {
        return Road_name;
    }

    public void setRoad_name(String road_name) {
        Road_name = road_name;
    }

    public String getRoad_category() {
        return Road_category;
    }

    public void setRoad_category(String road_category) {
        Road_category = road_category;
    }

    public String getRoad_type() {
        return Road_type;
    }

    public void setRoad_type(String road_type) {
        Road_type = road_type;
    }

    public String getStart_junction_road_name() {
        return Start_junction_road_name;
    }

    public void setStart_junction_road_name(String start_junction_road_name) {
        Start_junction_road_name = start_junction_road_name;
    }

    public String getEnd_junction_road_name() {
        return End_junction_road_name;
    }

    public void setEnd_junction_road_name(String end_junction_road_name) {
        End_junction_road_name = end_junction_road_name;
    }

    public long getEasting() {
        return Easting;
    }

    public void setEasting(long easting) {
        Easting = easting;
    }

    public long getNorthing() {
        return Northing;
    }

    public void setNorthing(long northing) {
        Northing = northing;
    }

    public double getLatitude() {
        return Latitude;
    }

    public void setLatitude(double latitude) {
        Latitude = latitude;
    }

    public double getLongitude() {
        return Longitude;
    }

    public void setLongitude(double longitude) {
        Longitude = longitude;
    }

    public double getLink_length_km() {
        return Link_length_km;
    }

    public void setLink_length_km(double link_length_km) {
        Link_length_km = link_length_km;
    }

    public double getLink_length_miles() {
        return Link_length_miles;
    }

    public void setLink_length_miles(double link_length_miles) {
        Link_length_miles = link_length_miles;
    }

    public int getPedal_cycles() {
        return Pedal_cycles;
    }

    public void setPedal_cycles(int pedal_cycles) {
        Pedal_cycles = pedal_cycles;
    }

    public int getTwo_wheeled_motor_vehicles() {
        return Two_wheeled_motor_vehicles;
    }

    public void setTwo_wheeled_motor_vehicles(int two_wheeled_motor_vehicles) {
        Two_wheeled_motor_vehicles = two_wheeled_motor_vehicles;
    }

    public int getCars_and_taxis() {
        return Cars_and_taxis;
    }

    public void setCars_and_taxis(int cars_and_taxis) {
        Cars_and_taxis = cars_and_taxis;
    }

    public int getBuses_and_coaches() {
        return Buses_and_coaches;
    }

    public void setBuses_and_coaches(int buses_and_coaches) {
        Buses_and_coaches = buses_and_coaches;
    }

    public int getLGVs() {
        return LGVs;
    }

    public void setLGVs(int LGVs) {
        this.LGVs = LGVs;
    }

    public int getHGVs_3_rigid_axle() {
        return HGVs_3_rigid_axle;
    }

    public void setHGVs_3_rigid_axle(int HGVs_3_rigid_axle) {
        this.HGVs_3_rigid_axle = HGVs_3_rigid_axle;
    }

    public int getHGVs_4_or_more_rigid_axle() {
        return HGVs_4_or_more_rigid_axle;
    }

    public void setHGVs_4_or_more_rigid_axle(int HGVs_4_or_more_rigid_axle) {
        this.HGVs_4_or_more_rigid_axle = HGVs_4_or_more_rigid_axle;
    }

    public int getHGVs_3_or_4_articulated_axle() {
        return HGVs_3_or_4_articulated_axle;
    }

    public void setHGVs_3_or_4_articulated_axle(int HGVs_3_or_4_articulated_axle) {
        this.HGVs_3_or_4_articulated_axle = HGVs_3_or_4_articulated_axle;
    }

    public int getHGVs_5_articulated_axle() {
        return HGVs_5_articulated_axle;
    }

    public void setHGVs_5_articulated_axle(int HGVs_5_articulated_axle) {
        this.HGVs_5_articulated_axle = HGVs_5_articulated_axle;
    }

    public int getHGVs_6_articulated_axle() {
        return HGVs_6_articulated_axle;
    }

    public void setHGVs_6_articulated_axle(int HGVs_6_articulated_axle) {
        this.HGVs_6_articulated_axle = HGVs_6_articulated_axle;
    }

    public int getAll_HGVs() {
        return All_HGVs;
    }

    public void setAll_HGVs(int all_HGVs) {
        All_HGVs = all_HGVs;
    }

    public int getAll_motor_vehicles() {
        return All_motor_vehicles;
    }

    public void setAll_motor_vehicles(int all_motor_vehicles) {
        All_motor_vehicles = all_motor_vehicles;
    }

    @Override
    public double getX() {
        return Easting;
    }

    @Override
    public double getY() {
        return Northing;
    }

    @Override
    public String getId() {
        return String.valueOf(getCount_point_id());
    }

    public LocalDateTime getFullDate() {
        return LocalDate.parse(getCount_date(), dateFormatter).atStartOfDay().withHour(hour);
    }

    @Override
    public double getSimtime() {
        LocalDateTime dateTime = LocalDateTime.parse(getCount_date(), dateFormatter);
        dateTime = dateTime.withHour(getHour()); // add the time in "hour" to the date
        return dateTime.toEpochSecond(ZoneOffset.UTC);
    }

    @Override
    public DfTEntry copy() {
        return this;
    }

    public ImmutablePair<LocalDateTime, Integer> comparablePair() {
        LocalDateTime dateTime = LocalDateTime.parse(getCount_date(), dateFormatter);
        // dateTime = LocalDate.parse(dateString, dateFormatter).atStartOfDay();
//        int hour = Integer.parseInt(hourString);
        dateTime = dateTime.withHour(hour); // add the time in "hour" to the date
        return new ImmutablePair<>(dateTime, getCount_point_id());
    }

    @Override
    public int compareTo(DfTEntry o) {
        return comparablePair().compareTo(o.comparablePair());
    }
}
