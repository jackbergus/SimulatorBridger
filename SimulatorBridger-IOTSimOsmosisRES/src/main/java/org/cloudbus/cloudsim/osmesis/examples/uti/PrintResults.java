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

package org.cloudbus.cloudsim.osmesis.examples.uti;


import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.edge.core.edge.EdgeDevice;
import org.cloudbus.cloudsim.sdn.SDNHost;
import org.cloudbus.cloudsim.sdn.Switch;
import org.cloudbus.cloudsim.sdn.power.PowerUtilizationHistoryEntry;
import org.cloudbus.cloudsim.sdn.power.PowerUtilizationInterface;
import org.cloudbus.osmosis.core.OsmoticAppDescription;
import org.cloudbus.osmosis.core.OsmoticBroker;
import org.cloudbus.osmosis.core.WorkflowInfo;
import uk.ncl.giacomobergami.components.iot.IoTDevice;
import uk.ncl.giacomobergami.utils.data.CSVMediator;

import static uk.ncl.giacomobergami.utils.database.JavaPostGres.*;


/**
 *
 * @author Khaled Alwasel
 * @contact kalwasel@gmail.com
 * @since IoTSim-Osmosis 1.0
 *
**/

public class PrintResults {
	List<AccurateBatteryInformation> battInfo;
	List<OsmoticAppDescription> appList;
	List<PrintOsmosisAppFromTags> osmoticAppsStats;
	List<OsmesisOverallAppsResults> overallAppResults;
	List<EnergyConsumption> dataCenterEnergyConsumption;
	List<PowerConsumption> hpc;
	List<PowerConsumption> spc;
	List<ActualPowerUtilizationHistoryEntry> puhe;
	List<ActualHistoryEntry> ahe;
	private List<EdgeConnectionsPerSimulationTime> connectionPerSimTime;
	List<BandShareInfo> bsi;
	File ABIFile, ALFile, OASFile, OARFile, DCECFile, HPCFile, SPCFile, PUHFile, HEFile, CPSFile, BSIFile;
	String ABICSV, ALCSV, OASCSV, OARCSV, DCECCSV, HPCCSV, SPCCSV, PUHCSV, HECSV, CPSCSV, BSICSV;
//	TreeMap<String, List<String>> app_to_path;

	public void createCSVFiles(File folder) {
		String[] Paths = new String[11];
		ABIFile = new File(folder, "accurateBatteryInfo.csv");
		ALFile = new File(folder, "appList.csv");
		OASFile = new File(folder, "osmoticAppsStats.csv");
		OARFile = new File(folder, "overallAppResults.csv");
		DCECFile = new File(folder, "dataCenterEnergyConsumption.csv");
		HPCFile = new File(folder, "HostPowerConsumption.csv");
		SPCFile = new File(folder, "SwitchPowerConsumption.csv");
		PUHFile = new File(folder, "PowerUtilisationHistory.csv");
		HEFile = new File(folder, "HistoryEntry.csv");
		CPSFile = new File(folder, "connectionPerSimTime.csv");
		BSIFile = new File(folder, "bandwidthShareInfo.csv");

		ABICSV = ABIFile.getAbsolutePath();
		ALCSV = ALFile.getAbsolutePath();
		BSICSV = BSIFile.getAbsolutePath();
		CPSCSV = CPSFile.getAbsolutePath();
		DCECCSV = DCECFile.getAbsolutePath();
		HECSV = HEFile.getAbsolutePath();
		HPCCSV = HPCFile.getAbsolutePath();
		OASCSV = OASFile.getAbsolutePath();
		OARCSV = OARFile.getAbsolutePath();
		PUHCSV = PUHFile.getAbsolutePath();
		SPCCSV = SPCFile.getAbsolutePath();
	}

	public void dumpCSV(File folder) {
		if (!folder.exists()) {
			folder.mkdirs();
		}
		createCSVFiles(folder);
		new CSVMediator<>(AccurateBatteryInformation.class).writeAll(ABIFile, battInfo);
		new CSVMediator<>(OsmoticAppDescription.class).writeAll(ALFile, appList);
		new CSVMediator<>(PrintOsmosisAppFromTags.class).writeAll(OASFile, osmoticAppsStats);
		new CSVMediator<>(OsmesisOverallAppsResults.class).writeAll(OARFile, overallAppResults);
		new CSVMediator<>(EnergyConsumption.class).writeAll(DCECFile, dataCenterEnergyConsumption);
		new CSVMediator<>(PowerConsumption.class).writeAll(HPCFile, hpc);
		new CSVMediator<>(PowerConsumption.class).writeAll(SPCFile, spc);
		new CSVMediator<>(ActualPowerUtilizationHistoryEntry.class).writeAll(PUHFile, puhe);
		new CSVMediator<>(ActualHistoryEntry.class).writeAll(HEFile, ahe);
		new CSVMediator<>(EdgeConnectionsPerSimulationTime.class).writeAll(CPSFile, connectionPerSimTime);
		new CSVMediator<>(BandShareInfo.class).writeAll(BSIFile, bsi);
//		try {
//			Files.writeString(new File(folder, "paths.json").toPath(), new Gson().toJson(app_to_path));
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
	}

	public void write_to_SQL(Connection conn) throws SQLException {
		emptyTABLE(conn, "accurateBatteryInfo");
		INSERTAccurateBatteryInfo(conn);
		emptyTABLE(conn, "accurateBatteryInfo_import");
		emptyTABLE(conn, "appList");
		INSERTAppList(conn);
		emptyTABLE(conn, "appList_import");
		emptyTABLE(conn, "osmoticAppsStats");
		INSERTOsmoticAppsStats(conn);
		emptyTABLE(conn, "osmoticAppsStats_import");
		emptyTABLE(conn, "overallAppResults");
		INSERTOverallAppResults(conn);
		emptyTABLE(conn, "overallAppResults_import");
		emptyTABLE(conn, "dataCenterEnergyConsumption");
		INSERTDataCenterEnergyConsumption(conn);
		emptyTABLE(conn, "dataCenterEnergyConsumption_import");
		emptyTABLE(conn, "HostPowerConsumption");
		INSERTHostPowerConsumption(conn);
		emptyTABLE(conn, "HostPowerConsumption_import");
		emptyTABLE(conn, "SwitchPowerConsumption");
		INSERTSwitchPowerConsumption(conn);
		emptyTABLE(conn, "SwitchPowerConsumption_import");
		emptyTABLE(conn, "PowerUtilisationHistory");
		INSERTPowerUtilisationHistory(conn);
		emptyTABLE(conn, "PowerUtilisationHistory_import");
		emptyTABLE(conn, "HistoryEntry");
		INSERTHistoryEntry(conn);
		emptyTABLE(conn, "HistoryEntry_import");
		emptyTABLE(conn, "connectionPerSimTime");
		INSERTConnectionPerSimTime(conn);
		emptyTABLE(conn, "connectionPerSimTime_import");
		emptyTABLE(conn, "bandwidthShareInfo");
		INSERTBandwidthShareInfo(conn);
		emptyTABLE(conn, "bandwidthShareInfo_import");
	}

	protected void INSERTAccurateBatteryInfo(Connection conn) {
		String targetTABLE = "accurateBatteryInfo";
		if (TABLEsize(conn, targetTABLE) != 0) {
			return;
		}

		System.out.print("Organising accurateBatteryInfo Data...\n");
		long startTime = System.nanoTime();
		copyCSVDATA(conn, ABICSV, targetTABLE);
		transferDATABetweenTables(conn, "accurateBatteryInfo(iotdevicename, consumption, flowid, nopackets, time)",
				"iotdevicename, consumption, flowid, nopackets, time", targetTABLE);
		long endTime = System.nanoTime();
		long executionTime = (endTime - startTime) / 1000000;
		System.out.print("Sending accurateBatteryInfo data to SQL Database\n");
		System.out.println("This takes " + executionTime + "ms");
	}

	protected void INSERTAppList(Connection conn) {
		String targetTABLE = "appList";
		if (TABLEsize(conn, targetTABLE) != 0) {
			return;
		}

		System.out.print("Organising appList Data...\n");
		long startTime = System.nanoTime();
		copyCSVDATA(conn, ALCSV, targetTABLE);
		transferDATABetweenTables(conn, "appList(appid,appname,appstarttime,clouddatacentername,clouddcid,datarate," +
						"edgedatacentername,edgedcid,endtime,iotdevicebatteryconsumption,iotdevicebatterystatus,iotdeviceid,iotdevicename,iotdeviceoutputsize," +
						"isiotdevicedied,melid,melname,meloutputsize,osmesiscloudletsize,osmesisedgeletsize,startdatagenerationtime,stopdatagenerationtime," +
						"vmcloudid,vmname,workflowid)",
				"appid,appname,appstarttime,clouddatacentername,clouddcid,datarate,edgedatacentername,edgedcid,endtime,iotdevicebatteryconsumption,iotdevicebatterystatus,iotdeviceid,iotdevicename,iotdeviceoutputsize,isiotdevicedied,melid,melname,meloutputsize,osmesiscloudletsize,osmesisedgeletsize,startdatagenerationtime,stopdatagenerationtime,vmcloudid,vmname,workflowid"
				,targetTABLE);
		long endTime = System.nanoTime();
		long executionTime = (endTime - startTime) / 1000000;
		System.out.print("Sending appList data to SQL Database\n");
		System.out.println("This takes " + executionTime + "ms");
	}

	protected void INSERTOsmoticAppsStats(Connection conn) {
		String targetTABLE = "osmoticAppsStats";
		if (TABLEsize(conn, targetTABLE) != 0) {
			return;
		}

		System.out.print("Organising osmoticAppsStats Data...\n");
		long startTime = System.nanoTime();
		copyCSVDATA(conn, OASCSV, targetTABLE);
		transferDATABetweenTables(conn, "osmoticAppsStats(appid,appname,cloudletmisize,cloudletproccessingtimebyvm" +
						",datasizeiotdevicetomel_mb,datasizemeltovm_mb,destinationvmname,edgeletmisize,edgeletproccessingtimebymel" +
						",edgelet_mel_finishtime,edgelet_mel_starttime,finishtime,iotdevicename,melname,melendtransmissiontime,melstarttransmissiontime,starttime," +
						"oas_transaction,transactiontotaltime,transmissiontimeiotdevicetomel,transmissiontimemeltovm,flowiotmelappid,flowmelcloudappid,path_dst,path_src,edgetowanbw)",
				"appid,appname,cloudletmisize,cloudletproccessingtimebyvm,datasizeiotdevicetomel_mb,datasizemeltovm_mb,destinationvmname,edgeletmisize,edgeletproccessingtimebymel,edgelet_mel_finishtime,edgelet_mel_starttime,finishtime,iotdevicename,melname,melendtransmissiontime,melstarttransmissiontime,starttime,oas_transaction,transactiontotaltime,transmissiontimeiotdevicetomel,transmissiontimemeltovm,flowiotmelappid,flowmelcloudappid,path_dst,path_src,edgetowanbw"
				,targetTABLE);
		long endTime = System.nanoTime();
		long executionTime = (endTime - startTime) / 1000000;
		System.out.print("Sending osmoticAppsStats data to SQL Database\n");
		System.out.println("This takes " + executionTime + "ms");
	}

	protected void INSERTOverallAppResults(Connection conn) {
		String targetTABLE = "overallAppResults";
		if (TABLEsize(conn, targetTABLE) != 0) {
			return;
		}

		System.out.print("Organising overallAppResults Data...\n");
		long startTime = System.nanoTime();
		copyCSVDATA(conn, OARCSV, targetTABLE);
		transferDATABetweenTables(conn, "overallAppResults(appname,endtime,iotdevicebatteryconsumption," +
						"iotdevicedrained,simluationtime,starttime,totalcloudletsizes,totaledgeletsizes,totaliotgenerateddata,totalmelgenerateddata,apptotalrunningtime)",
				"appname,endtime,iotdevicebatteryconsumption,iotdevicedrained,simluationtime,starttime,totalcloudletsizes,totaledgeletsizes,totaliotgenerateddata,totalmelgenerateddata,apptotalrunningtime"
				,targetTABLE);
		long endTime = System.nanoTime();
		long executionTime = (endTime - startTime) / 1000000;
		System.out.print("Sending overallAppResults data to SQL Database\n");
		System.out.println("This takes " + executionTime + "ms");
	}

	protected void INSERTDataCenterEnergyConsumption(Connection conn) {

		String targetTABLE = "dataCenterEnergyConsumption";
		if (TABLEsize(conn, targetTABLE) != 0) {
			return;
		}

		System.out.print("Organising dataCenterEnergyConsumption Data...\n");
		long startTime = System.nanoTime();
		copyCSVDATA(conn, DCECCSV, targetTABLE);
		transferDATABetweenTables(conn, "dataCenterEnergyConsumption(hostenergyconsumed,switchenergyconsumed,totalenergyconsumed,dcname,finishtime)",
				"hostenergyconsumed,switchenergyconsumed,totalenergyconsumed,dcname,finishtime"
				,targetTABLE);
		long endTime = System.nanoTime();
		long executionTime = (endTime - startTime) / 1000000;
		System.out.print("Sending dataCenterEnergyConsumption data to SQL Database\n");
		System.out.println("This takes " + executionTime + "ms");
	}

	protected void INSERTHostPowerConsumption(Connection conn) {

		String targetTABLE = "HostPowerConsumption";
		if (TABLEsize(conn, targetTABLE) != 0) {
			return;
		}

		System.out.print("Organising HostPowerConsumption Data...\n");
		long startTime = System.nanoTime();
		copyCSVDATA(conn, HPCCSV, targetTABLE);
		transferDATABetweenTables(conn, "HostPowerConsumption(dcname,energy,hpc_name)",
				"dcname,energy,hpc_name"
				,targetTABLE);
		long endTime = System.nanoTime();
		long executionTime = (endTime - startTime) / 1000000;
		System.out.print("Sending HostPowerConsumption data to SQL Database\n");
		System.out.println("This takes " + executionTime + "ms");
	}

	protected void INSERTSwitchPowerConsumption(Connection conn) {

		String targetTABLE = "SwitchPowerConsumption";
		if (TABLEsize(conn, targetTABLE) != 0) {
			return;
		}

		System.out.print("Organising SwitchPowerConsumption Data...\n");
		long startTime = System.nanoTime();
		copyCSVDATA(conn, SPCCSV, targetTABLE);
		transferDATABetweenTables(conn, "SwitchPowerConsumption(dcname,energy,spc_name)",
				"dcname,energy,spc_name"
				,targetTABLE);
		long endTime = System.nanoTime();
		long executionTime = (endTime - startTime) / 1000000;
		System.out.print("Sending SwitchPowerConsumption data to SQL Database\n");
		System.out.println("This takes " + executionTime + "ms");
	}

	protected void INSERTPowerUtilisationHistory(Connection conn) {

		String targetTABLE = "PowerUtilisationHistory";
		if (TABLEsize(conn, targetTABLE) != 0) {
			return;
		}

		System.out.print("Organising PowerUtilisationHistory Data...\n");
		long startTime = System.nanoTime();
		copyCSVDATA(conn, PUHCSV, targetTABLE);
		transferDATABetweenTables(conn, "PowerUtilisationHistory(dcname, puh_name, starttime, usedmips)",
				"dcname, puh_name, starttime, usedmips"
				,targetTABLE);
		long endTime = System.nanoTime();
		long executionTime = (endTime - startTime) / 1000000;
		System.out.print("Sending PowerUtilisationHistory data to SQL Database\n");
		System.out.println("This takes " + executionTime + "ms");
	}

	protected void INSERTHistoryEntry(Connection conn) {

		String targetTABLE = "HistoryEntry";
		if (TABLEsize(conn, targetTABLE) != 0) {
			return;
		}

		System.out.print("Organising HistoryEntry Data...\n");
		long startTime = System.nanoTime();
		copyCSVDATA(conn, HECSV, targetTABLE);
		transferDATABetweenTables(conn, "HistoryEntry(numactiveports, starttime)",
				"numactiveports, starttime"
				,targetTABLE);
		long endTime = System.nanoTime();
		long executionTime = (endTime - startTime) / 1000000;
		System.out.print("Sending HistoryEntry data to SQL Database\n");
		System.out.println("This takes " + executionTime + "ms");
	}

	protected void INSERTConnectionPerSimTime(Connection conn) {

		String targetTABLE = "ConnectionPerSimTime";
		if (TABLEsize(conn, targetTABLE) != 0) {
			return;
		}

		System.out.print("Organising ConnectionPerSimTime Data...\n");
		long startTime = System.nanoTime();
		copyCSVDATA(conn, CPSCSV, targetTABLE);
		transferDATABetweenTables(conn, "ConnectionPerSimTime(iotdevices, edgehost, cps_time)",
				"iotdevices, edgehost, cps_time"
				,targetTABLE);
		long endTime = System.nanoTime();
		long executionTime = (endTime - startTime) / 1000000;
		System.out.print("Sending ConnectionPerSimTime data to SQL Database\n");
		System.out.println("This takes " + executionTime + "ms");
	}

	protected void INSERTBandwidthShareInfo(Connection conn) {

		String targetTABLE = "bandwidthShareInfo";
		if (TABLEsize(conn, targetTABLE) != 0) {
			return;
		}

		System.out.print("Organising bandwidthShareInfo Data...\n");
		long startTime = System.nanoTime();
		copyCSVDATA(conn, BSICSV, targetTABLE);
		transferDATABetweenTables(conn, "bandwidthShareInfo(bandwidthshare, channelid, edgename, melname, timestamp)",
				"bandwidthshare, channelid, edgename, melname, timestamp"
				,targetTABLE);
		long endTime = System.nanoTime();
		long executionTime = (endTime - startTime) / 1000000;
		System.out.print("Sending bandwidthShareInfo data to SQL Database\n");
		System.out.println("This takes " + executionTime + "ms");
	}

	public void addHostPowerConsumption(String dcName, String name, double energy) {
		if (hpc == null) hpc = new ArrayList<>();
		hpc.add(new PowerConsumption(dcName, name, energy));
	}
	public void addSwitchPowerConsumption(String dcName, String name, double energy) {
		if (spc == null) spc = new ArrayList<>();
		spc.add(new PowerConsumption(dcName, name, energy));
	}

	public void addHostUtilizationHistory(String dcName, String name, List<PowerUtilizationHistoryEntry> utilizationHisotry) {
		if (puhe == null) puhe = new ArrayList<>();
		if (utilizationHisotry == null) return;
		utilizationHisotry.forEach(x -> puhe.add(new ActualPowerUtilizationHistoryEntry(dcName, name, x)));
	}

	public void addSwitchUtilizationHistory(String dcName, String name, List<Switch.HistoryEntry> utilizationHisotry) {
		if (ahe == null) ahe = new ArrayList<>();
		if (utilizationHisotry == null) return;
		utilizationHisotry.forEach(x -> ahe.add(new ActualHistoryEntry(dcName, name, x)));
	}

	public void collectTrustworthyBatteryData(Map<String, IoTDevice> devices) {
		battInfo = new ArrayList<>();
		for (var nameToIoT : devices.entrySet()) {
			var actualDevice = nameToIoT.getValue();
			var deviceMemory = actualDevice.computeTrustworthyCommunication();
			var flowInfo = actualDevice.getActionToFlowId();
			for (var entry : actualDevice.getTrustworthyConsumption().entrySet()) {
				battInfo.add(new AccurateBatteryInformation(nameToIoT.getKey(),
						entry.getKey(),
						entry.getValue(),
						deviceMemory.get(entry.getKey()),
						flowInfo.get(entry.getKey())));
			}
		}
	}

	public static class EdgeConnectionsPerSimulationTime {
		public double time;
		public String edge_host;
		public int    IoTDevices;

		public EdgeConnectionsPerSimulationTime(double time, String edge_host, int ioTDevices) {
			this.time = time;
			this.edge_host = edge_host;
			IoTDevices = ioTDevices;
		}
	}
		
	public void collectNetworkData(List<OsmoticAppDescription> appList,
								   OsmoticBroker osmoticBroker) {
		osmoticAppsStats = new ArrayList<>();
		overallAppResults = new ArrayList<>();
		TreeMap<Double, HashMultimap<String, String>> tm = new TreeMap<>();
		List<WorkflowInfo> tags = new ArrayList<>();
		Multimap<Integer, OsmoticAppDescription> leftTable = HashMultimap.create();
		Multimap<Integer, WorkflowInfo> rightTable = HashMultimap.create();
		Set<Integer> allIds = new HashSet<>();

		for(OsmoticAppDescription app : appList){
			leftTable.put(app.getAppID(), app);
			allIds.add(app.getAppID());
		}
		for(WorkflowInfo app : OsmoticBroker.workflowTag){
			rightTable.put(app.getAppId(), app);
		}
		allIds.retainAll(OsmoticBroker.workflowTag.stream().map(x->x.getAppId()).collect(Collectors.toSet()));

		for(Integer appId : allIds){
			tags.clear();
			for(WorkflowInfo workflowTag : rightTable.get(appId)){
				tags.add(workflowTag);
			}
			tags.forEach(x -> this.generateAppTag(x, osmoticAppsStats, osmoticBroker, tm));
			if (!tags.isEmpty()) {
				for (var x : leftTable.get(appId)) {
					printAppStat(x, tags, overallAppResults);
				}
			}
			tags.clear();
		}

		Set<String> allActiveNodes = tm.entrySet()
						.stream()
								.flatMap(kv -> kv.getValue().keys().stream())
										.collect(Collectors.toUnmodifiableSet());

		this.connectionPerSimTime = tm.entrySet()
				.stream()
				.flatMap(kv -> allActiveNodes.stream()
						.map(x -> {
							var s = kv.getValue().get(x);
							var n = s == null ? 0 : s.size();
							return new EdgeConnectionsPerSimulationTime(kv.getKey(), x, n);
						}))
				.collect(Collectors.toList());

//		for(OsmoticAppDescription app : appList){
//			for(WorkflowInfo workflowTag : OsmoticBroker.workflowTag){
//				workflowTag.getAppId();
//				if(app.getAppID() == workflowTag.getAppId()){
//					tags.add(workflowTag);
//				}
//			}
//			if (!tags.isEmpty())
//				printAppStat(app, tags, overallAppResults);
//			tags.clear();
//		}
		this.appList = appList;
	}

	public void collectDataCenterData(String dcName,
									  List<SDNHost> hostList,
									  List<Switch> switchList,
									  double finishTime) {
		EnergyConsumption ec = new EnergyConsumption();
		ec.dcName = dcName;
		ec.finishTime = finishTime;
		if(hostList != null){
			for(SDNHost sdnHost:hostList) {
				Host host = sdnHost.getHost();
				PowerUtilizationInterface scheduler =  (PowerUtilizationInterface) host.getVmScheduler();
				scheduler.addUtilizationEntryTermination(finishTime);
				double energy = scheduler.getUtilizationEnergyConsumption();
				ec.addHostPowerConsumption(energy);
				addHostPowerConsumption(dcName, sdnHost.getName(), energy);
				addHostUtilizationHistory(dcName, sdnHost.getName(), scheduler.getUtilizationHisotry());
			}
		}
		for(Switch sw:switchList) {
			sw.addUtilizationEntryTermination(finishTime);
			double energy = sw.getUtilizationEnergyConsumption();
			ec.addSwitchPowerConsumption(energy);
			addSwitchPowerConsumption(dcName, sw.getName(), energy);
			addSwitchUtilizationHistory(dcName, sw.getName(), sw.getUtilizationHisotry());
		}

		ec.finalise();
		if (dataCenterEnergyConsumption == null) dataCenterEnergyConsumption = new ArrayList<>();
		dataCenterEnergyConsumption.add(ec);
	}

	public static class OsmesisOverallAppsResults {
		public String App_Name;
		public String IoTDeviceDrained;
		public double IoTDeviceBatteryConsumption;
		public long TotalIoTGeneratedData;
		public long TotalEdgeLetSizes;
		public long TotalMELGeneratedData;
		public long TotalCloudLetSizes;
		public double StartTime;
		public double EndTime;
		public double SimluationTime;
		public double appTotalRunningTime;
	}

	private void printAppStat(OsmoticAppDescription app,
							  List<WorkflowInfo> tags,
							  List<OsmesisOverallAppsResults> list) {
		String appName = app.getAppName();
		String isIoTDeviceDrained = app.getIoTDeviceBatteryStatus();
		double iotDeviceTotalConsumption = app.getIoTDeviceBatteryConsumption();
		long TotalIoTGeneratedData = 0;
		long TotalEdgeLetSizes = 0;
		long TotalMELGeneratedData = 0;
		long TotalCloudLetSizes = 0;
		double appTotalRunningTmie = 0;
		OsmesisOverallAppsResults fromTag = new OsmesisOverallAppsResults();


		double StartTime = app.getAppStartTime();
		var tmp = tags.get(tags.size()-1).getCloudLet();
		if (tmp == null) return;
		double EndTime = tmp.getFinishTime();
		double SimluationTime = EndTime - StartTime;
		
		WorkflowInfo firstWorkflow = tags.get(0);
		WorkflowInfo secondWorkflow = tags.size() > 1 ? tags.get(1) : null;
		
		if((secondWorkflow != null) && (firstWorkflow.getFinishTime() > secondWorkflow.getStartTime())) {
			appTotalRunningTmie = EndTime - StartTime;			
		} else {
			for(WorkflowInfo workflowTag : tags){
				appTotalRunningTmie += workflowTag.getFinishTime() - workflowTag.getStartTime();
			}
		}
		if (StartTime < 0.0) {
			StartTime = EndTime - appTotalRunningTmie;
		}
		
		for(WorkflowInfo workflowTag : tags){
			TotalIoTGeneratedData += workflowTag.getIotDeviceFlow().getSize(); 
			TotalEdgeLetSizes += workflowTag.getEdgeLet().getCloudletLength(); 
			TotalMELGeneratedData += workflowTag.getEdgeToCloudFlow().getSize();
			TotalCloudLetSizes += workflowTag.getCloudLet().getCloudletLength();			   
		}
		
		fromTag.App_Name = appName;
		fromTag.IoTDeviceDrained = isIoTDeviceDrained;
		fromTag.IoTDeviceBatteryConsumption = iotDeviceTotalConsumption;
		fromTag.TotalIoTGeneratedData = TotalIoTGeneratedData;
		fromTag.TotalEdgeLetSizes = TotalEdgeLetSizes;
		fromTag.TotalMELGeneratedData = TotalMELGeneratedData;
		fromTag.TotalCloudLetSizes = TotalCloudLetSizes;
		fromTag.StartTime = StartTime;
		fromTag.EndTime = EndTime;
		fromTag.SimluationTime = SimluationTime;
		fromTag.appTotalRunningTime = appTotalRunningTmie;
		list.add(fromTag);
	}

	public static class PrintOsmosisAppFromTags {
		public int APP_ID;
		public String AppName;
		public int Transaction;
		public double StartTime;
		public double FinishTime;
		public String IoTDeviceName;
		public String MELName;
		public long DataSizeIoTDeviceToMEL_Mb;
		public double TransmissionTimeIoTDeviceToMEL;
		public double EdgeLetMISize;
		public double EdgeLet_MEL_StartTime;
		public double EdgeLet_MEL_FinishTime;
		public double EdgeLetProccessingTimeByMEL;
		public String DestinationVmName;
		public long DataSizeMELToVM_Mb;
		public double TransmissionTimeMELToVM;
		public double CloudLetMISize;
		public double CloudLetProccessingTimeByVM;
		public double TransactionTotalTime;
		public String path_src, path_dst;
		public double MelStartTransmissionTime;
		public double MelEndTransmissionTime;
		public int flowMELCloudAppId;
		public int flowIoTMelAppId;
		public double zEdgeToWANBW;
	}

	public void generateAppTag(WorkflowInfo workflowTag,
							   List<PrintOsmosisAppFromTags> list,
							   OsmoticBroker MELResolverToHostingHost,
							   TreeMap<Double, HashMultimap<String, String>> countingMapPerSimTime) {
//			ArrayList<Link> ls1 = new ArrayList<>();
//			var sx = workflowTag.getEdgeToCloudFlow();
//			if ((sx != null) && (sx.getNodeOnRouteList() != null)) ls1.addAll(sx.getLinkList());
	//		Collections.reverse(ls1);
//			var dx = workflowTag.getIotDeviceFlow();
//			if ((dx != null) && (dx.getNodeOnRouteList() != null)) ls1.addAll(dx.getLinkList());
//			if (app_to_path == null) app_to_path = new TreeMap<>();
//			var res = sortLinks(new ArrayList<>(ls1));
//			app_to_path.put(workflowTag.getAppName(), res);


			PrintOsmosisAppFromTags fromTag = new PrintOsmosisAppFromTags();
			if(workflowTag.getCloudLet() != null) {
				fromTag.APP_ID = workflowTag.getAppId();
				fromTag.AppName = workflowTag.getAppName();
				fromTag.Transaction = workflowTag.getWorkflowId();
				fromTag.StartTime = workflowTag.getStartTime();
				countingMapPerSimTime.putIfAbsent(fromTag.StartTime, HashMultimap.create());
				fromTag.FinishTime = workflowTag.getFinishTime();
				fromTag.IoTDeviceName = workflowTag.getIotDeviceFlow().getAppNameSrc();
				fromTag.MELName = workflowTag.getIotDeviceFlow().getAppNameDest() + " (" + workflowTag.getSourceDCName() + ")";
				var srcHost = MELResolverToHostingHost.resolveHostFromMELId(workflowTag.getIotDeviceFlow().getAppNameDest());
				if (srcHost == null) return; // skipping the communications that never happened
				if (!(srcHost instanceof EdgeDevice))
					throw new RuntimeException("ERROR: wrong assumption");
				fromTag.path_src = ((EdgeDevice) srcHost).getDeviceName();
				countingMapPerSimTime.get(fromTag.StartTime).put(fromTag.path_src, fromTag.IoTDeviceName);
				fromTag.DataSizeIoTDeviceToMEL_Mb = workflowTag.getIotDeviceFlow().getSize();
				fromTag.TransmissionTimeIoTDeviceToMEL = workflowTag.getIotDeviceFlow().getTransmissionTime();
				fromTag.EdgeLetMISize = workflowTag.getEdgeLet().getCloudletLength();
				fromTag.EdgeLet_MEL_StartTime = workflowTag.getEdgeLet().getExecStartTime();
				fromTag.EdgeLet_MEL_FinishTime = workflowTag.getEdgeLet().getFinishTime();
				fromTag.EdgeLetProccessingTimeByMEL = workflowTag.getEdgeLet().getActualCPUTime();
				fromTag.DestinationVmName = workflowTag.getEdgeToCloudFlow().getAppNameDest() + " (" + workflowTag.getDestinationDCName() + ")";
				var dstHost = MELResolverToHostingHost.resolveHostFromMELId(workflowTag.getEdgeToCloudFlow().getAppNameDest());
				fromTag.path_dst = "Host#" + dstHost.getId() + "@" + workflowTag.getDestinationDCName();
				fromTag.DataSizeMELToVM_Mb = workflowTag.getEdgeToCloudFlow().getSize();
				fromTag.flowMELCloudAppId = workflowTag.getEdgeToCloudFlow().getApp().getAppID();
				fromTag.flowIoTMelAppId = workflowTag.getIotDeviceFlow().getApp().getAppID();
				fromTag.MelStartTransmissionTime = workflowTag.getEdgeToCloudFlow().getStartTime();
				fromTag.TransmissionTimeMELToVM = workflowTag.getEdgeToCloudFlow().getTransmissionTime();
				fromTag.MelEndTransmissionTime = fromTag.TransmissionTimeMELToVM + fromTag.MelStartTransmissionTime;
				fromTag.CloudLetMISize = workflowTag.getCloudLet().getCloudletLength();
				fromTag.CloudLetProccessingTimeByVM = workflowTag.getCloudLet().getActualCPUTime();
				fromTag.TransactionTotalTime = workflowTag.getIotDeviceFlow().getTransmissionTime() + workflowTag.getEdgeLet().getActualCPUTime()
						+ workflowTag.getEdgeToCloudFlow().getTransmissionTime() + workflowTag.getCloudLet().getActualCPUTime();
				list.add(fromTag);
				fromTag.zEdgeToWANBW = workflowTag.getEdgeToCloudFlow().getEdgeToWANBW();
			}
	}

	public static class PowerConsumption {
		public String dcName;
		public String name;
		public double energy;
		public PowerConsumption() {}
		public PowerConsumption(String dcName, String name, double energy) {
			this.dcName = dcName;
			this.name = name;
			this.energy = energy;
		}
	}

	public static class ActualPowerUtilizationHistoryEntry {
		public String dcName;
		public String name;
		public double startTime;
		public double usedMips;

		public ActualPowerUtilizationHistoryEntry(String dcName, String name, PowerUtilizationHistoryEntry entry) {
			this.dcName = dcName;
			this.name = name;
			this.startTime = entry.startTime;
			this.usedMips = entry.usedMips;
		}
	}

	public static class ActualHistoryEntry {
		private String dcName;
		private String name;
		public double startTime;
		public double numActivePorts;

		public ActualHistoryEntry(String dcName, String name, Switch.HistoryEntry entry) {
			this.dcName = dcName;
			this.name = name;
			this.startTime = entry.startTime;
			this.numActivePorts = entry.numActivePorts;
		}
	}
	public static class EnergyConsumption {
		public String dcName;
		public double finishTime;
		public double HostEnergyConsumed;
		public double SwitchEnergyConsumed;
		public double TotalEnergyConsumed;

		public void finalise() {
			TotalEnergyConsumed = HostEnergyConsumed + SwitchEnergyConsumed;
		}

		public void addHostPowerConsumption(double energy) {
			HostEnergyConsumed += energy;
		}
		public void addSwitchPowerConsumption(double energy) {
			SwitchEnergyConsumed += energy;
		}
	}

	public static class BandwidthInfo {
		public String edgeName;
		public String melName;
		public String channelID;
		public List<Double> bandWidthShare = new ArrayList<>();
		public List<Double> timeStamp = new ArrayList<>();
		//public Map<Double, Double> bwMap;
		public BandwidthInfo(String edgeName, String melName, String channelID, Map<Double, Double> bwMap) {
			this.edgeName = edgeName;
			this.melName =	melName;
			this.channelID = channelID;
			//this.bwMap = bwMap;
			for(int i = 0; i < bwMap.values().size(); i++) {
                bandWidthShare.add((Double) bwMap.values().toArray()[i]);
				timeStamp.add((Double) bwMap.keySet().toArray()[i]);
			}
		}
	}

	public static class BandShareInfo{
		public String edgeName;
		public String melName;
		public String channelID;
		public double bandWidthShare;
		public double timeStamp;

		public BandShareInfo(String edgeName, String melName, String channelID, double bandWidthShare, double timeStamp) {
			this.edgeName = edgeName;
			this.melName = melName;
			this.channelID = channelID;
			this.bandWidthShare = bandWidthShare;
			this.timeStamp = timeStamp;
		}
	}
	public void collectBandwidthInfo(List<BandwidthInfo> bandwidthInfoList) {
		bsi = new ArrayList<>();
		for(int i = 0; i < bandwidthInfoList.size(); i++) {
			for (int j = 0; j < bandwidthInfoList.get(i).bandWidthShare.toArray().length; j++) {
				String Edge = ((BandwidthInfo) bandwidthInfoList.toArray()[i]).edgeName;
				String MEL = ((BandwidthInfo) bandwidthInfoList.toArray()[i]).melName;
				String chID = ((BandwidthInfo) bandwidthInfoList.toArray()[i]).channelID;
				double bw = (double) bandwidthInfoList.get(i).bandWidthShare.toArray()[j];
				double ts = (double) bandwidthInfoList.get(i).timeStamp.toArray()[j];
				bsi.add(new BandShareInfo(Edge, MEL, chID, bw, ts));
			}
		}
	}
}
