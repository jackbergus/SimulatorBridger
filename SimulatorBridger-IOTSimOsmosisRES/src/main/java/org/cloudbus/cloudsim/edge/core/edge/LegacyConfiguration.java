/*
 * Title:        IoTSim-Osmosis 1.0
 * Description:  IoTSim-Osmosis enables the testing and validation of osmotic computing applications 
 * 			     over heterogeneous edge-cloud SDN-aware environments.
 * 
 * Licence:      GPL - http://www.gnu.org/copyleft/gpl.html
 *
 * Copyright (c) 2020, Newcastle University (UK) and Saudi Electronic University (Saudi Arabia) 
 * 
 */


package org.cloudbus.cloudsim.edge.core.edge;

import java.io.File;
import java.io.FileReader;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import org.cloudbus.cloudsim.edge.core.edge.Mobility.Location;
import lombok.Data;
import org.cloudbus.cloudsim.sdn.Switch;
import org.cloudbus.osmosis.core.Topology;

/**
 * 
 * @author Khaled Alwasel
 * @contact kalwasel@gmail.com
 * @since IoTSim-Osmosis 1.0
 * 
**/

@Data
public class LegacyConfiguration {
	private LogEntity logEntity;
	private boolean trace_flag;

	private final static Gson gson = new Gson();

	public static LegacyConfiguration fromFile(File path) {
		try (FileReader jsonFileReader = new FileReader(path)){
			return gson.fromJson(jsonFileReader, LegacyConfiguration.class);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	private List<EdgeDataCenterEntity> edgeDatacenter;
	private List<CloudDataCenterEntity> cloudDatacenter;
	private List<WanEntity> sdwan;
	
	@Data
	public static class LogEntity {
		private String logLevel;
		private boolean saveLogToFile;
		private String logFilePath;
		private boolean append;
	}
	
	@Data
	public static class EdgeDataCenterEntity {
		private String name;
		private String type;
		private VmAllcationPolicyEntity vmAllocationPolicy;

		private double schedulingInterval;
		private EdgeDatacenterCharacteristicsEntity characteristics;

		private List<IotDeviceEntity> ioTDevices;

		private List<EdgeDeviceEntity> hosts;
		private List<MELEntities> MELEntities;
		private List<ControllerEntity> controllers;
		private List<SwitchEntity> switches;
		private List<LinkEntity> links;
	}

	@Data
	public static class CloudDataCenterEntity {
	    private String name;		
		private String type;
	    private String vmAllocationPolicy;

		private List<HostEntity> hosts;
	    private List<VMEntity> VMs;
	    private List<ControllerEntity> controllers; // only one, what a waste!
	    private List<SwitchEntity> switches;

	    private List<LinkEntity> links;
	}
	
	@Data
	public class WanEntity {
	    private ControllerEntity controllers;
	    private List<LinkEntity> links;
	    private List<SwitchEntity> switches;
	}

	@Data
	public static class VmAllcationPolicyEntity {
		String className;
		List<HostEntity> hostEntities;
	}

	@Data
	public static class EdgeDeviceEntity {
		String name;		
		long storage;
		int pes;
		int ramSize;
		int mips;
		double bwSize;
		Location location;
		double signalRange;
		double max_vehicle_communication;
	}
	
	@Data
	public static class HostEntity{
	    private String name;
	    private long pes;
	    private long mips;
	    private Integer ram;
	    private Long storage;	  
	    private double bw;
		private double max_vehicle_communication;
	}
	
	@Data 
	public static class VMEntity{
		String name; 		
		int pes;
		double mips;
		int ram;
		double storage;
		private double bw;
		String cloudletPolicy; 
	}
	
	@Data
	public class ControllerEntity{
	    public String name;
	    public String trafficPolicy;
	    public String routingPolicy;
	}
	
	@Data
	public static class SwitchEntity{
	    private String type;  // enum
	    private String name;
	    private String controller;
	    private Long iops;	    

	    public boolean isGateway(){
	        return this.type.equals("gateway");
	    }

		public void initializeSwitch(Map<String, Integer> nameIdTable,
									 Topology topology,
									 List<Switch> switches) {
			long iops = getIops();
			String switchName = getName();
			String switchType = getType();
			Switch sw = new Switch(switchName, switchType, iops);
			nameIdTable.put(switchName, sw.getAddress());
			topology.addNode(sw);
			switches.add(sw);
		}
	}
	
	@Data
	public static class LinkEntity{
	    private String source;
	    private String destination;
	    private double bw;

		public void initializeLink(Map<String, Integer> nameIdTable,
								   Topology topology) {
			String src = getSource();
			String dst = getDestination();
			double bw = getBw();
			int srcAddress = nameIdTable.get(src);
			if(dst.equals("")){
				System.out.println("Null!");
			}
			if (!nameIdTable.containsKey(dst))
				throw new RuntimeException("ERROR!");
			int dstAddress = nameIdTable.get(dst);
			topology.addLink(srcAddress, dstAddress, bw);
		}
	}
	
	@Data
	public static class EdgeDatacenterCharacteristicsEntity {
		String architecture;
		String os;
		String vmm;
		double cost;
		double timeZone;
		double costPerSec;
		double costPerMem;
		double costPerStorage;
		double costPerBw;			
	}

	@Data
	public static class MELEntities {
		String name;
		String host;		
		int mips;
		int ram; // vm memory (MB)
		double bw;
		int pesNumber; // number of cpus
		String vmm; // VMM name
		String cloudletSchedulerClassName;		
		float datasizeShrinkFactor;

	}
	
	@Data
	public static class IotDeviceEntity {
		private MobilityEntity mobilityEntity;				
		public String ioTClassName;		
		String name;
		double data_frequency;
		double dataGenerationTime;
		int complexityOfDataPackage;
		int dataSize;
		NetworkModelEntity networkModelEntity; // e.g. Wifi and xmpp
		double max_battery_capacity;
		double initial_battery_capacity;
		double battery_sensing_rate;
		double battery_sending_rate;
		double battery_voltage;
		boolean res_powered;
		double solar_peak_power;
		double max_charging_current;
		double processingAbility;
		EdgeLetEntity dataTemplate;
		double bw;
		double latency;

	}
	
	@Data
	public static class EdgeLetEntity {
		int cloudletId;
		long cloudletLength;
		int pesNumber;
		long cloudletFileSize;
		long cloudletOutputSize;
		String utilizationModelCpu;
		String utilizationModelRam;
		String utilizationModelBw;
	}

	@Data
	public static class NetworkModelEntity {
		private String networkType;
		private String communicationProtocol;
	}

	@Data
	public static class MobilityEntity {
		private boolean movable;
		private double velocity;
		private MovingRangeEntity range;
		private double signalRange;
		private Location location;

		public MobilityEntity(Location location) {
			super();
			this.location = location;
		}

		public MobilityEntity() {
			super();
		}
	}

	public static class MovingRangeEntity {
		public int beginX;
		public int endX;
		public int beginY;
		public int endY;

		public MovingRangeEntity() {
			super();
		}

		public MovingRangeEntity(int beginX, int endX, int beginY, int endY) {
			super();
			this.beginX = beginX;
			this.endX = endX;
			this.beginY = beginY;
			this.endY = endY;

		}
	}

}
