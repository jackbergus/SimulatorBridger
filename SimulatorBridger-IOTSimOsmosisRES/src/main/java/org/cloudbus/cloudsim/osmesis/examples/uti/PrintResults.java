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
import java.sql.PreparedStatement;
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

import javax.sql.DataSource;

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
//	TreeMap<String, List<String>> app_to_path;

	public void dumpCSV(File folder) {
		if (!folder.exists()) {
			folder.mkdirs();
		}
		new CSVMediator<>(AccurateBatteryInformation.class).writeAll(new File(folder, "accurateBatteryInfo.csv"), battInfo);
		new CSVMediator<>(OsmoticAppDescription.class).writeAll(new File(folder, "appList.csv"), appList);
		new CSVMediator<>(PrintOsmosisAppFromTags.class).writeAll(new File(folder, "osmoticAppsStats.csv"), osmoticAppsStats);
		new CSVMediator<>(OsmesisOverallAppsResults.class).writeAll(new File(folder, "overallAppResults.csv"), overallAppResults);
		new CSVMediator<>(EnergyConsumption.class).writeAll(new File(folder, "dataCenterEnergyConsumption.csv"), dataCenterEnergyConsumption);
		new CSVMediator<>(PowerConsumption.class).writeAll(new File(folder, "HostPowerConsumption.csv"), hpc);
		new CSVMediator<>(PowerConsumption.class).writeAll(new File(folder, "SwitchPowerConsumption.csv"), spc);
		new CSVMediator<>(ActualPowerUtilizationHistoryEntry.class).writeAll(new File(folder, "PowerUtilisationHistory.csv"), puhe);
		new CSVMediator<>(ActualHistoryEntry.class).writeAll(new File(folder, "HistoryEntry.csv"), ahe);
		new CSVMediator<>(EdgeConnectionsPerSimulationTime.class).writeAll(new File(folder, "connectionPerSimTime.csv"), connectionPerSimTime);
		new CSVMediator<>(BandShareInfo.class).writeAll(new File(folder, "bandwidthShareInfo.csv"), bsi);
//		try {
//			Files.writeString(new File(folder, "paths.json").toPath(), new Gson().toJson(app_to_path));
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
	}

	public void write_to_SQL(Connection conn) throws SQLException {
		emptyTABLE(conn, "accurateBatteryInfo");
		INSERTAccurateBatteryInfo(conn, battInfo);
		emptyTABLE(conn, "appList");
		INSERTAppList(conn, appList);
		emptyTABLE(conn, "osmoticAppsStats");
		INSERTOsmoticAppsStats(conn, osmoticAppsStats);
		emptyTABLE(conn, "overallAppResults");
		INSERTOverallAppResults(conn, overallAppResults);
		emptyTABLE(conn, "dataCenterEnergyConsumption");
		INSERTDataCenterEnergyConsumption(conn, dataCenterEnergyConsumption);
		emptyTABLE(conn, "HostPowerConsumption");
		INSERTHostPowerConsumption(conn, hpc);
		emptyTABLE(conn, "SwitchPowerConsumption");
		INSERTSwitchPowerConsumption(conn, spc);
		emptyTABLE(conn, "PowerUtilisationHistory");
		INSERTPowerUtilisationHistory(conn, puhe);
		emptyTABLE(conn, "HistoryEntry");
		INSERTHistoryEntry(conn, ahe);
		emptyTABLE(conn, "connectionPerSimTime");
		INSERTConnectionPerSimTime(conn, connectionPerSimTime);
		emptyTABLE(conn, "bandwidthShareInfo");
		INSERTBandwidthShareInfo(conn, bsi);
	}

	protected void INSERTAccurateBatteryInfo(Connection conn, Object writable) throws SQLException {
		if (TABLEsize(conn, "accurateBatteryInfo") != 0) {
			return;
		}

		int start_ID = 1;

		int noEntries = ((ArrayList) writable).size();
		for (int i = 0; i < noEntries; i++) {
			AccurateBatteryInformation entry = (AccurateBatteryInformation) ((ArrayList) writable).get(i);
			PreparedStatement insertStmt = StartINSERTtoTable(conn, "accurateBatteryInfo(unique_entry_id, iotdevicename, consumption, flowid, nopackets, time)");
			int unique_entry_ID = INSERTInt(insertStmt, 1, start_ID);
			int iotdevicename = INSERTString(insertStmt, 2, entry.getIoTDeviceName());
			int consumption = INSERTDouble(insertStmt, 3, entry.getConsumption());
			int flowid = INSERTInt(insertStmt, 4, entry.getFlowId());
			int nopackets = INSERTDouble(insertStmt, 5, entry.getNoPackets());
			int time = INSERTDouble(insertStmt, 6, entry.getTime());
			boolean finishedInsert = EndINSERTtoTable(insertStmt);

			start_ID++;
		}
	}

	protected void INSERTAppList(Connection conn, Object writable) throws SQLException {
		if (TABLEsize(conn, "appList") != 0) {
			return;
		}

		int start_ID = 1;

		int noEntries = ((ArrayList) writable).size();
		for (int i = 0; i < noEntries; i++) {
			OsmoticAppDescription entry = (OsmoticAppDescription) ((ArrayList) writable).get(i);
			String EdgeLetsfromList = "";
			for (int j = 0; j < entry.getEdgeLetList().size(); j++) {
				if(j != 0) EdgeLetsfromList += ", ";
				EdgeLetsfromList += entry.getEdgeLetList().get(j);
			}
			PreparedStatement insertStmt = StartINSERTtoTable(conn, "appList(unique_entry_id,appid,appname,appstarttime,clouddatacentername," +
					"clouddcid,datarate,edgedatacentername,edgedcid,edgeletlist,endtime,iotdevicebatteryconsumption,\n" +
					"\tiotdevicebatterystatus,iotdeviceid,iotdevicename,iotdeviceoutputsize,isiotdevicedied,melid,melname,meloutputsize,osmesiscloudletsize,osmesisedgeletsize,\n" +
					"\tstartdatagenerationtime,stopdatagenerationtime,vmcloudid,vmname,workflowid)");
			int unique_entry_ID = INSERTInt(insertStmt, 1, start_ID);
			int appID = INSERTInt(insertStmt, 2, entry.getAppID());
			int appName = INSERTString(insertStmt, 3, entry.getAppName());
			int appStartTime = INSERTDouble(insertStmt, 4, entry.getAppStartTime());
			int cloudDatacenterName = INSERTString(insertStmt, 5, entry.getCloudDatacenterName());
			int cloudDcId = INSERTInt(insertStmt, 6, entry.getCloudDcId());
			int dataRate = INSERTDouble(insertStmt, 7, entry.getDataRate());
			int edgeDatacenterName = INSERTString(insertStmt, 8, entry.getEdgeDatacenterName());
			int edgeDcId = INSERTInt(insertStmt, 9, entry.getEdgeDcId());
			int edgeLetList = entry.getEdgeLetList().size() > 0 ? INSERTString(insertStmt, 10, EdgeLetsfromList) : INSERTString(insertStmt, 10, "null");
			int endTime = INSERTDouble(insertStmt, 11, entry.getEndTime());
			int ioTDeviceBatteryConsumption = INSERTDouble(insertStmt, 12, entry.getIoTDeviceBatteryConsumption());
			int ioTDeviceBatteryStatus = INSERTString(insertStmt, 13, entry.getIoTDeviceBatteryStatus());
			int ioTDeviceId = INSERTInt(insertStmt, 14, entry.getIoTDeviceId());
			int ioTDeviceName = INSERTString(insertStmt, 15, entry.getIoTDeviceName());
			int ioTDeviceOutputSize = INSERTInt(insertStmt, 16, (int) entry.getIoTDeviceOutputSize());
			int isIoTDeviceDied = INSERTString(insertStmt, 17, String.valueOf(entry.getIsIoTDeviceDied()));
			int melId = INSERTInt(insertStmt, 18, entry.getMelId());
			int melname = INSERTString(insertStmt, 19, entry.getMELName());
			int meloutputSize = INSERTInt(insertStmt, 20, (int) entry.getMELOutputSize());
			int osmesisCloudletSize = INSERTInt(insertStmt, 21, (int) entry.getOsmesisCloudletSize());
			int osmesisEdgeletSize = INSERTInt(insertStmt, 22, (int) entry.getOsmesisEdgeletSize());
			int startDataGenerationTime = INSERTDouble(insertStmt, 23, entry.getStartDataGenerationTime());
			int stopDataGenerationTime = INSERTDouble(insertStmt, 24, entry.getStopDataGenerationTime());
			int vmCloudId = INSERTInt(insertStmt, 25, entry.getVmCloudId());
			int vmName = INSERTString(insertStmt, 26, entry.getVmName());
			int workflowId = INSERTInt(insertStmt, 27, entry.getWorkflowId());
			boolean finishedInsert = EndINSERTtoTable(insertStmt);

			start_ID++;
		}
	}

	protected void INSERTOsmoticAppsStats(Connection conn, Object writable) throws SQLException {
		if (TABLEsize(conn, "osmoticAppsStats") != 0) {
			return;
		}

		int start_ID = 1;

		int noEntries = ((ArrayList) writable).size();
		for (int i = 0; i < noEntries; i++) {
			PrintOsmosisAppFromTags entry = (PrintOsmosisAppFromTags) ((ArrayList) writable).get(i);
			PreparedStatement insertStmt = StartINSERTtoTable(conn, "osmoticAppsStats(unique_entry_id,appid,appname,cloudletmisize,cloudletproccessingtimebyvm" +
					",datasizeiotdevicetomel_mb,datasizemeltovm_mb,destinationvmname,edgeletmisize,edgeletproccessingtimebymel" +
					",edgelet_mel_finishtime,edgelet_mel_starttime,finishtime,iotdevicename,melname,melendtransmissiontime,melstarttransmissiontime,starttime," +
					"oas_transaction,transactiontotaltime,transmissiontimeiotdevicetomel,transmissiontimemeltovm,flowiotmelappid,flowmelcloudappid,path_dst,path_src,edgetowanbw)");
			int unique_entry_ID = INSERTInt(insertStmt, 1, start_ID);
			int appID = INSERTInt(insertStmt, 2, entry.APP_ID);
			int appname = INSERTString(insertStmt, 3, entry.AppName);
			int cloudletmisize = INSERTDouble(insertStmt, 4, entry.CloudLetMISize);
			int cloudletproccessingtimebyvm = INSERTDouble(insertStmt, 5, entry.CloudLetProccessingTimeByVM);
			int datasizeiotdevicetomel_mb = INSERTInt(insertStmt, 6, (int) entry.DataSizeIoTDeviceToMEL_Mb);
			int datasizemeltovm_mb = INSERTInt(insertStmt, 7, (int) entry.DataSizeMELToVM_Mb);
			int destinationvmname = INSERTString(insertStmt, 8, entry.DestinationVmName);
			int edgeletmisize = INSERTDouble(insertStmt, 9, entry.EdgeLetMISize);
			int edgeletproccessingtimebymel = INSERTDouble(insertStmt, 10, entry.EdgeLetProccessingTimeByMEL);
			int edgelet_mel_finishtime = INSERTDouble(insertStmt, 11, entry.EdgeLet_MEL_FinishTime);
			int edgelet_mel_starttime = INSERTDouble(insertStmt, 12, entry.EdgeLet_MEL_StartTime);
			int finishtime = INSERTDouble(insertStmt, 13, entry.FinishTime);
			int iotdevicename = INSERTString(insertStmt, 14, entry.IoTDeviceName);
			int melname = INSERTString(insertStmt, 15, entry.MELName);
			int melendtransmissiontime = INSERTDouble(insertStmt, 16, entry.MelEndTransmissionTime);
			int melstarttransmissiontime = INSERTDouble(insertStmt,  17, entry.MelStartTransmissionTime);
			int starttime = INSERTDouble(insertStmt, 18, entry.StartTime);
			int oas_transaction = INSERTInt(insertStmt, 19, entry.Transaction);
			int transactiontotaltime = INSERTDouble(insertStmt, 20, entry.TransactionTotalTime);
			int transmissiontimeiotdevicetomel = INSERTDouble(insertStmt, 21, entry.TransmissionTimeIoTDeviceToMEL);
			int transmissiontimemeltovm = INSERTDouble(insertStmt, 22, entry.TransmissionTimeMELToVM);
			int flowiotmelappid = INSERTInt(insertStmt, 23, entry.flowIoTMelAppId);
			int flowmelcloudappid = INSERTInt(insertStmt, 24, entry.flowMELCloudAppId);
			int path_dst = INSERTString(insertStmt, 25, entry.path_dst);
			int path_src = INSERTString(insertStmt, 26, entry.path_src);
			int edgetowanbw = INSERTDouble(insertStmt, 27, entry.zEdgeToWANBW);
			boolean finishedInsert = EndINSERTtoTable(insertStmt);

			start_ID++;
		}
	}

	protected void INSERTOverallAppResults(Connection conn, Object writable) throws SQLException {
		if (TABLEsize(conn, "overallAppResults") != 0) {
			return;
		}

		int start_ID = 1;

		int noEntries = ((ArrayList) writable).size();
		for (int i = 0; i < noEntries; i++) {
			OsmesisOverallAppsResults entry = (OsmesisOverallAppsResults) ((ArrayList) writable).get(i);
			PreparedStatement insertStmt = StartINSERTtoTable(conn, "overallAppResults(unique_entry_id,appname,endtime," +
					"iotdevicebatteryconsumption,iotdevicedrained,simluationtime,starttime,totalcloudletsizes,totaledgeletsizes," +
					"totaliotgenerateddata,totalmelgenerateddata,apptotalrunningtime)");
			int unique_entry_ID = INSERTInt(insertStmt, 1, start_ID);
			int appname = INSERTString(insertStmt, 2, entry.App_Name);
			int endtime = INSERTDouble(insertStmt, 3, entry.EndTime);
			int iotdevicebatteryconsumption = INSERTDouble(insertStmt, 4, entry.IoTDeviceBatteryConsumption);
			int iotdevicedrained = INSERTString(insertStmt, 5, entry.IoTDeviceDrained);
			int simluationtime = INSERTDouble(insertStmt, 6, entry.SimluationTime);
			int starttime = INSERTDouble(insertStmt, 7, entry.StartTime);
			int totalcloudletsizes = INSERTInt(insertStmt, 8, (int) entry.TotalCloudLetSizes);
			int totaledgeletsizes = INSERTInt(insertStmt, 9, (int) entry.TotalEdgeLetSizes);
			int totaliotgenerateddata = INSERTInt(insertStmt, 10, (int) entry.TotalIoTGeneratedData);
			int totalmelgenerateddata = INSERTInt(insertStmt, 11, (int) entry.TotalMELGeneratedData);
			int apptotalrunningtime = INSERTDouble(insertStmt, 12, entry.appTotalRunningTime);
			boolean finishedInsert = EndINSERTtoTable(insertStmt);

			start_ID++;
		}
	}

	protected void INSERTDataCenterEnergyConsumption(Connection conn, Object writable) throws SQLException {
		if (TABLEsize(conn, "dataCenterEnergyConsumption") != 0) {
			return;
		}

		int start_ID = 1;

		int noEntries = ((ArrayList) writable).size();
		for (int i = 0; i < noEntries; i++) {
			EnergyConsumption entry = (EnergyConsumption) ((ArrayList) writable).get(i);
			PreparedStatement insertStmt = StartINSERTtoTable(conn, "dataCenterEnergyConsumption(unique_entry_id,hostenergyconsumed,switchenergyconsumed,totalenergyconsumed,dcname,finishtime)");
			int unique_entry_ID = INSERTInt(insertStmt, 1, start_ID);
			int hostenergyconsumed = INSERTDouble(insertStmt, 2, entry.HostEnergyConsumed);
			int switchenergyconsumed = INSERTDouble(insertStmt, 3, entry.SwitchEnergyConsumed);
			int totalenergyconsumed = INSERTDouble(insertStmt, 4, entry.TotalEnergyConsumed);
			int dcname = INSERTString(insertStmt, 5, entry.dcName);
			int finishtime = INSERTDouble(insertStmt, 6, entry.finishTime);
			boolean finishedInsert = EndINSERTtoTable(insertStmt);

			start_ID++;
		}
	}

	protected void INSERTHostPowerConsumption(Connection conn, Object writable) throws SQLException {
		if (TABLEsize(conn, "HostPowerConsumption") != 0) {
			return;
		}

		int start_ID = 1;

		int noEntries = ((ArrayList) writable).size();
		for (int i = 0; i < noEntries; i++) {
			PowerConsumption entry = (PowerConsumption) ((ArrayList) writable).get(i);
			PreparedStatement insertStmt = StartINSERTtoTable(conn, "HostPowerConsumption(unique_entry_id,dcname,energy,hpc_name)");
			int unique_entry_ID = INSERTInt(insertStmt, 1, start_ID);
			int dcname = INSERTString(insertStmt, 2, entry.dcName);
			int energy = INSERTDouble(insertStmt, 3, entry.energy);
			int hpc_name = INSERTString(insertStmt, 4, entry.name);
			boolean finishedInsert = EndINSERTtoTable(insertStmt);

			start_ID++;
		}
	}

	protected void INSERTSwitchPowerConsumption(Connection conn, Object writable) throws SQLException {
		if (TABLEsize(conn, "SwitchPowerConsumption") != 0) {
			return;
		}

		int start_ID = 1;

		int noEntries = ((ArrayList) writable).size();
		for (int i = 0; i < noEntries; i++) {
			PowerConsumption entry = (PowerConsumption) ((ArrayList) writable).get(i);
			PreparedStatement insertStmt = StartINSERTtoTable(conn, "SwitchPowerConsumption(unique_entry_id,dcname,energy,spc_name)");
			int unique_entry_ID = INSERTInt(insertStmt, 1, start_ID);
			int dcname = INSERTString(insertStmt, 2, entry.dcName);
			int energy = INSERTDouble(insertStmt, 3, entry.energy);
			int spc_name = INSERTString(insertStmt, 4, entry.name);
			boolean finishedInsert = EndINSERTtoTable(insertStmt);

			start_ID++;
		}
	}

	protected void INSERTPowerUtilisationHistory(Connection conn, Object writable) throws SQLException {
		if (TABLEsize(conn, "PowerUtilisationHistory") != 0) {
			return;
		}

		int start_ID = 1;

		int noEntries = ((ArrayList) writable).size();
		for (int i = 0; i < noEntries; i++) {
			ActualPowerUtilizationHistoryEntry entry = (ActualPowerUtilizationHistoryEntry) ((ArrayList) writable).get(i);
			PreparedStatement insertStmt = StartINSERTtoTable(conn, "PowerUtilisationHistory(unique_entry_id,dcname, puh_name, starttime, usedmips)");
			int unique_entry_ID = INSERTInt(insertStmt, 1, start_ID);
			int dcname = INSERTString(insertStmt, 2, entry.dcName);
			int puh_name = INSERTString(insertStmt, 3, entry.name);
			int starttime = INSERTDouble(insertStmt, 4, entry.startTime);
			int usedmips = INSERTDouble(insertStmt, 5, entry.usedMips);
			boolean finishedInsert = EndINSERTtoTable(insertStmt);

			start_ID++;
		}
	}

	protected void INSERTHistoryEntry(Connection conn, Object writable) throws SQLException {
		if (TABLEsize(conn, "HistoryEntry") != 0) {
			return;
		}

		int start_ID = 1;

		int noEntries = ((ArrayList) writable).size();
		for (int i = 0; i < noEntries; i++) {
			ActualHistoryEntry entry = (ActualHistoryEntry) ((ArrayList) writable).get(i);
			PreparedStatement insertStmt = StartINSERTtoTable(conn, "HistoryEntry(unique_entry_id,numactiveports, starttime)");
			int unique_entry_ID = INSERTInt(insertStmt, 1, start_ID);
			int numactiveports = INSERTInt(insertStmt, 2, (int) entry.numActivePorts);
			int starttime = INSERTDouble(insertStmt, 3, entry.startTime);

			boolean finishedInsert = EndINSERTtoTable(insertStmt);

			start_ID++;
		}
	}

	protected void INSERTConnectionPerSimTime(Connection conn, Object writable) throws SQLException {
		if (TABLEsize(conn, "ConnectionPerSimTime") != 0) {
			return;
		}

		int start_ID = 1;

		int noEntries = ((ArrayList) writable).size();
		for (int i = 0; i < noEntries; i++) {
			EdgeConnectionsPerSimulationTime entry = (EdgeConnectionsPerSimulationTime) ((ArrayList) writable).get(i);
			PreparedStatement insertStmt = StartINSERTtoTable(conn, "ConnectionPerSimTime(unique_entry_id,iotdevices, edgehost, cps_time)");
			int unique_entry_ID = INSERTInt(insertStmt, 1, start_ID);
			int iotdevices = INSERTInt(insertStmt, 2, entry.IoTDevices);
			int edgehost = INSERTString(insertStmt, 3, entry.edge_host);
			int cps_time = INSERTDouble(insertStmt, 4, entry.time);

			boolean finishedInsert = EndINSERTtoTable(insertStmt);

			start_ID++;
		}
	}

	protected void INSERTBandwidthShareInfo(Connection conn, Object writable) throws SQLException {
		if (TABLEsize(conn, "bandwidthShareInfo") != 0) {
			return;
		}

		int start_ID = 1;

		int noEntries = ((ArrayList) writable).size();
		for (int i = 0; i < noEntries; i++) {
			BandShareInfo entry = (BandShareInfo) ((ArrayList) writable).get(i);
			PreparedStatement insertStmt = StartINSERTtoTable(conn, "bandwidthShareInfo(unique_entry_id,bandwidthshare, channelid, edgename, melname, timestamp)");
			int unique_entry_ID = INSERTInt(insertStmt, 1, start_ID);
			int bandwidthshare = INSERTDouble(insertStmt, 2, entry.bandWidthShare);
			int channelid = INSERTString(insertStmt, 3, entry.channelID);
			int edgename = INSERTString(insertStmt, 4, entry.edgeName);
			int melname = INSERTString(insertStmt, 5, entry.melName);
			int timestamp = INSERTDouble(insertStmt, 6, entry.timeStamp);

			boolean finishedInsert = EndINSERTtoTable(insertStmt);

			start_ID++;
		}
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
		
		if((secondWorkflow != null) && (firstWorkflow.getFinishTime() > secondWorkflow.getSartTime())) {
			appTotalRunningTmie = EndTime - StartTime;			
		} else {
			for(WorkflowInfo workflowTag : tags){
				appTotalRunningTmie += workflowTag.getFinishTime() - workflowTag.getSartTime(); 
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
			fromTag.APP_ID = workflowTag.getAppId();
			fromTag.AppName = workflowTag.getAppName();
			fromTag.Transaction = workflowTag.getWorkflowId();
			fromTag.StartTime = workflowTag.getSartTime();
		countingMapPerSimTime.putIfAbsent(fromTag.StartTime, HashMultimap.create());
			fromTag.FinishTime = workflowTag.getFinishTime();
			fromTag.IoTDeviceName = workflowTag.getIotDeviceFlow().getAppNameSrc();
			fromTag.MELName = workflowTag.getIotDeviceFlow().getAppNameDest() + " (" +workflowTag.getSourceDCName() + ")";
			var srcHost = MELResolverToHostingHost.resolveHostFromMELId(workflowTag.getIotDeviceFlow().getAppNameDest());
			if (srcHost == null) return; // skipping the communications that never happened
			if (!(srcHost instanceof EdgeDevice))
				throw new RuntimeException("ERROR: wrong assumption");
			fromTag.path_src = ((EdgeDevice)srcHost).getDeviceName();
			countingMapPerSimTime.get(fromTag.StartTime).put(fromTag.path_src, fromTag.IoTDeviceName);
			fromTag.DataSizeIoTDeviceToMEL_Mb = workflowTag.getIotDeviceFlow().getSize();
			fromTag.TransmissionTimeIoTDeviceToMEL = workflowTag.getIotDeviceFlow().getTransmissionTime();
			fromTag.EdgeLetMISize = workflowTag.getEdgeLet().getCloudletLength();
			fromTag.EdgeLet_MEL_StartTime = workflowTag.getEdgeLet().getExecStartTime();
			fromTag.EdgeLet_MEL_FinishTime = workflowTag.getEdgeLet().getFinishTime();
			fromTag.EdgeLetProccessingTimeByMEL = workflowTag.getEdgeLet().getActualCPUTime();
			fromTag.DestinationVmName = workflowTag.getEdgeToCloudFlow().getAppNameDest() + " (" +workflowTag.getDestinationDCName() + ")";
			var dstHost = MELResolverToHostingHost.resolveHostFromMELId(workflowTag.getEdgeToCloudFlow().getAppNameDest());
			fromTag.path_dst = "Host#"+dstHost.getId()+"@"+workflowTag.getDestinationDCName();
			fromTag.DataSizeMELToVM_Mb = workflowTag.getEdgeToCloudFlow().getSize();
			fromTag.flowMELCloudAppId = workflowTag.getEdgeToCloudFlow().getApp().getAppID();
			fromTag.flowIoTMelAppId = workflowTag.getIotDeviceFlow().getApp().getAppID();
			fromTag.MelStartTransmissionTime =  workflowTag.getEdgeToCloudFlow().getStartTime();
			fromTag.TransmissionTimeMELToVM = workflowTag.getEdgeToCloudFlow().getTransmissionTime();
			fromTag.MelEndTransmissionTime = fromTag.TransmissionTimeMELToVM + fromTag.MelStartTransmissionTime;
			fromTag.CloudLetMISize = workflowTag.getCloudLet().getCloudletLength();
			fromTag.CloudLetProccessingTimeByVM = workflowTag.getCloudLet().getActualCPUTime();
			fromTag.TransactionTotalTime =  workflowTag.getIotDeviceFlow().getTransmissionTime() + workflowTag.getEdgeLet().getActualCPUTime()
					+ workflowTag.getEdgeToCloudFlow().getTransmissionTime() + workflowTag.getCloudLet().getActualCPUTime();
			list.add(fromTag);
			fromTag.zEdgeToWANBW = workflowTag.getEdgeToCloudFlow().getEdgeToWANBW();
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
