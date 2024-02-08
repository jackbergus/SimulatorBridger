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

package org.cloudbus.osmosis.core;

import java.io.File;
import java.sql.Connection;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import com.google.common.collect.HashMultimap;
import me.tongfei.progressbar.ProgressBar;
import org.cloudbus.agent.AgentBroker;
import org.cloudbus.agent.CentralAgent;
import org.cloudbus.cloudsim.*;
import org.cloudbus.cloudsim.core.MainEventManager;
import org.cloudbus.cloudsim.core.CloudSimTags;
import org.cloudbus.cloudsim.core.SimEvent;
import org.cloudbus.cloudsim.edge.core.edge.EdgeDevice;
import org.cloudbus.cloudsim.edge.core.edge.EdgeLet;
import org.jooq.DSLContext;
import org.jooq.Result;
import uk.ncl.giacomobergami.components.iot.IoTDevice;
import uk.ncl.giacomobergami.components.iot.IoTEntityGenerator;
import uk.ncl.giacomobergami.components.loader.GlobalConfigurationSettings;
import uk.ncl.giacomobergami.components.mel_routing.MELSwitchPolicy;
import uk.ncl.giacomobergami.components.networking.DataCenterWithController;
import uk.ncl.giacomobergami.utils.asthmatic.WorkloadCSV;
import uk.ncl.giacomobergami.utils.data.YAML;
import uk.ncl.giacomobergami.utils.database.jooq.tables.Vehinformation;
import uk.ncl.giacomobergami.utils.database.jooq.tables.records.VehinformationRecord;
import uk.ncl.giacomobergami.utils.pipeline_confs.TrafficConfiguration;

import static org.cloudbus.cloudsim.core.CloudSimTags.MAPE_WAKEUP_FOR_COMMUNICATION;
import static org.cloudbus.osmosis.core.OsmoticTags.GENERATE_OSMESIS_WITH_RESOLUTION;
import static org.jooq.impl.DSL.field;

/**
 * 
 * @author Khaled Alwasel
 * @contact kalwasel@gmail.com
 * @since IoTSim-Osmosis 1.0
 * 
**/

public class OsmoticBroker extends DatacenterBroker {
//	public EdgeSDNController edgeController;
	public List<Cloudlet> edgeletList = new ArrayList<>();
	public List<OsmoticAppDescription> appList;
	public Map<String, Integer> iotDeviceNameToId = new HashMap<>();
	public Map<String, IoTDevice> iotDeviceNameToObject = new HashMap<>();
	public Map<Integer, List<? extends Vm>> mapVmsToDatacenter  = new HashMap<>();
	public static int brokerID;
	public Map<String, Integer> iotVmIdByName = new HashMap<>();
	public static List<WorkflowInfo> workflowTag = new ArrayList<>();
	public List<OsmoticDatacenter> datacenters = new ArrayList<>();
	private final AtomicInteger edgeLetId;
	public boolean isWakeupStartSet;

	public Map<String, IoTDevice> getDevices() {
		return iotDeviceNameToObject;
	}
	protected static int activeCount = 0;
	protected static int comCount = 0;
	public static int getActiveCount() {
		return activeCount;
	}
	public static int getComCount() {
		return comCount;
	}
	public static void setActiveCount(int newCount) {
		activeCount = newCount;
	}
	//private Map<String, Integer> roundRobinMelMap = new HashMap<>();
	/////////////////////////////////////////////////////////////////////////////////////
	protected static TreeSet<SimEvent> eventQueue = new TreeSet<>(Collections.reverseOrder());
	public static int getEventQueueSize() {
		return eventQueue.size();
	}
	public static TreeSet<SimEvent> getEventQueue() {
		return eventQueue;
	}
	public static void setEventQueue(TreeSet<SimEvent> eQ) {
		eventQueue = eQ;
	}
	protected static TreeMap<SimEvent, String> eventMap = new TreeMap<>(Collections.reverseOrder());
	public static  int getEventMapSize(){return eventMap.size();}
	public static TreeMap<SimEvent, String> getEventMap() {return eventMap; }
	public static HashMap<String, Integer> activePerSource = new HashMap<>();
	private static TreeSet<SimEvent> waitQueue = new TreeSet<>();
	protected static HashMap<String, Float> edgeToCloudBandwidth = new HashMap<>();
	public static HashMap<String, Float> getEdgeToCloudBandwidth() { return edgeToCloudBandwidth; }
	public static void updateEdgeTOCloudBandwidth(String id, float bw) {
		edgeToCloudBandwidth.replace(id, bw);
	}
	public static String choice = DataCenterWithController.getLimiting();
	protected static String change;
	private final File converter_file = new File("clean_example/converter.yaml");
	private final Optional<TrafficConfiguration> time_conf = YAML.parse(TrafficConfiguration.class, converter_file);
	double beginSUMO = time_conf.get().getBegin();
	double endSUMO = time_conf.get().getEnd();
	//////////////////////////////////////////////////////////////////////////////////////////////
	public CentralAgent osmoticCentralAgent;
	private AtomicInteger flowId;
	private IoTEntityGenerator ioTEntityGenerator;
	public static double deltaVehUpdate;
	private double startTime = time_conf.get().getIsBatch() ? time_conf.get().getBatchStart() : time_conf.get().getBegin();
	private double endTime = time_conf.get().getIsBatch() ? time_conf.get().getBatchEnd() : time_conf.get().getEnd();
	private final int collectionInterval = (int) Math.min(1, endTime);
	private int collectSQLInfo = (int) startTime / collectionInterval;
	private int intervalStart = (int) Math.floor(startTime);
	private int intervalEnd = intervalStart + collectionInterval;
	private Result<VehinformationRecord> dataRange = null;
	//private Result<Record4<String, Double, Double, Double>> dataNowRange = null;
	//private Result<Record4<String, Double, Double, Double>> dataFutureRange = null;
	private ProgressBar pb = null;
	private double lastTime = 0;
	private final double[] notUpdated = new double[]{-1.0, -1.0};
	private List<String> vehsToUpdate = null;
	private final float maxEdgeBW = 100;


	private static OsmoticBroker OBINSTANCE;

	private OsmoticBroker(String name,
						 AtomicInteger edgeLetId,
						 AtomicInteger flowId) {
		super(name);
		this.edgeLetId = edgeLetId;
		this.flowId = flowId;
		this.appList = new ArrayList<>();
		brokerID = this.getId();
		isWakeupStartSet = false;
		if(time_conf.get().getIsBatch() && !time_conf.get().getIsFirstBatch()) {
			String queuePath = time_conf.get().getQueueFilePath() + "eventQ.ser";
			eventQueue = MainEventManager.deserializeEventQueue(queuePath);
		}
	}

	public static OsmoticBroker getInstance(String name,
											AtomicInteger edgeLetId,
											AtomicInteger flowId) {
		if (OBINSTANCE == null) {
			OBINSTANCE = new OsmoticBroker(name, edgeLetId, flowId);
		}
		return OBINSTANCE;
	}

	@Override
	public void startEntity() {
		super.startEntity();
	}

	@Override
	public void processEvent(SimEvent ev, Connection conn, DSLContext context) {
		double chron = MainEventManager.clock();

		// Setting up the forced times when the simulator has to wake up, as new messages have to be sent
		if (!isWakeupStartSet) {
			for (Double forcedWakeUpTime :
					ioTEntityGenerator.collectionOfWakeUpTimes()) {
				double time = forcedWakeUpTime - chron;
				if (time > 0.0 && chron + getDeltaVehUpdate() <= endTime) {
					schedule(OsmoticBroker.brokerID, time, MAPE_WAKEUP_FOR_COMMUNICATION, null);
				}
			}
			isWakeupStartSet = true;
		}

		if (ev.getTag() == MAPE_WAKEUP_FOR_COMMUNICATION) {
			logger.trace("WakeUp Call @"+ chron);
		}

		if(chron <= endTime && chron > lastTime && chron >= startTime) {
			var ab = AgentBroker.getInstance();
			//info used to update IoT devices' positions
			double now = (double) Math.round((chron / IoTEntityGenerator.lat) * IoTEntityGenerator.lat * 1000) / 1000;
			double future = now + (2 * deltaVehUpdate);
			lastTime = now;

			if (collectSQLInfo == (int) now / collectionInterval) {
				//System.out.print("Collecting new batch of vehicle information from SQL table...\n");
				//dataNowRange = context.select(Vehinformation.VEHINFORMATION.VEHICLE_ID, Vehinformation.VEHINFORMATION.X, Vehinformation.VEHINFORMATION.Y, Vehinformation.VEHINFORMATION.SIMTIME).from(Vehinformation.VEHINFORMATION).where("simtime BETWEEN '" + (double) intervalStart + "' AND '" + Math.min((double) intervalEnd, endSUMO) + "'").orderBy(field("simtime")).fetch();
				//dataFutureRange = context.select(Vehinformation.VEHINFORMATION.VEHICLE_ID, Vehinformation.VEHINFORMATION.X, Vehinformation.VEHINFORMATION.Y, Vehinformation.VEHINFORMATION.SIMTIME).from(Vehinformation.VEHINFORMATION).where("simtime BETWEEN '" + ((double) intervalStart + (2 * deltaVehUpdate)) + "' AND '" + Math.min(((double) intervalEnd + (2 * deltaVehUpdate)), endSUMO) + "'").orderBy(field("simtime")).fetch();
				dataRange = context.select(Vehinformation.VEHINFORMATION.VEHICLE_ID, Vehinformation.VEHINFORMATION.X, Vehinformation.VEHINFORMATION.Y, Vehinformation.VEHINFORMATION.SIMTIME).from(Vehinformation.VEHINFORMATION).where("simtime BETWEEN '" + (double) intervalStart + "' AND '" + Math.min(((double) intervalEnd + (2 * deltaVehUpdate)), endTime) + "'").orderBy(field("simtime")).fetchInto(Vehinformation.VEHINFORMATION);
				vehsToUpdate = dataRange.getValues(Vehinformation.VEHINFORMATION.VEHICLE_ID);
				collectSQLInfo++;
				intervalStart += collectionInterval;
				intervalEnd += collectionInterval;
				//System.out.print("Batch collected from SQL table\n");
			}

            var Times = dataRange.getValues(Vehinformation.VEHINFORMATION.SIMTIME);//dataRange.getValues(3);
			//var nowTimes = Times; //dataNowRange.getValues(3);
			HashMap<String, double[]> nowData = new HashMap<>();
			var nowFirst = Times.indexOf(now);
			var nowLast = Times.lastIndexOf(now);
			if (nowFirst != -1) {
				for (int i = nowFirst; i <= nowLast; i++) {
					String name = dataRange.get(i).getValue(Vehinformation.VEHINFORMATION.VEHICLE_ID);
					double[] nowPos = {dataRange.get(i).getValue(Vehinformation.VEHINFORMATION.X), dataRange.get(i).getValue(Vehinformation.VEHINFORMATION.Y)};
					nowData.put(name, nowPos);
				}
				//var futureTimes = Times;//
				HashMap<String, double[]> futureData = new HashMap<>();
				var futureFirst = Times.indexOf(future);
				var futureLast = Times.lastIndexOf(future);
				for (int i = futureFirst; i < futureLast; i++) {
					String name = dataRange.get(i).getValue(Vehinformation.VEHINFORMATION.VEHICLE_ID);
					double[] futurePos = nowData.containsKey(name) ? new double[]{dataRange.get(i).getValue(Vehinformation.VEHINFORMATION.X), dataRange.get(i).getValue(Vehinformation.VEHINFORMATION.Y)} : notUpdated;
					futureData.put(name, futurePos);
				}

				//var dataNow = context.select(field("vehicle_id"), field("x"), field("y")).from(Vehinformation.VEHINFORMATION).where("simtime = '" + now + "'").fetch();
				//var dataFuture = context.select(field("vehicle_id"), field("x"), field("y")).from(Vehinformation.VEHINFORMATION).where("simtime = '" + future + "'").fetch();

				// Updates the IoT Device with the geo-location information
				for (int i = 0; i < nowData.keySet().size(); i++) {
					String id = (String) nowData.keySet().toArray()[i];
					IoTDevice obj = iotDeviceNameToObject.get(id);
					double[] nowDouble = nowData.get(id);
					double[] futureDouble = futureData.getOrDefault(id, notUpdated);
					ioTEntityGenerator.updateIoTDevice(obj, nowDouble, futureDouble);
				}
			}

			/*iotDeviceNameToObject.forEach((id, obj) -> {
				double[] nowDouble = nowData.containsKey(id) ? nowData.get(id) : notUpdated;				//double[] nowDouble = dataNow.getValues(0).indexOf(id) == -1 ? new double[]{-1.0, -1.0} : new double[]{(double) dataNow.getValue(dataNow.getValues(0).indexOf(id), 1), (double) dataNow.getValue(dataNow.getValues(0).indexOf(id), 2)};
				double[] futureDouble = futureData.containsKey(id) ? futureData.get(id) : notUpdated;
				//double[] futureDouble = dataFuture.getValues(0).indexOf(id) == -1 ? new double[]{-1.0, -1.0} : new double[]{(double) dataFuture.getValue(dataFuture.getValues(0).indexOf(id), 1), (double) dataFuture.getValue(dataFuture.getValues(0).indexOf(id), 2)};
				ioTEntityGenerator.updateIoTDevice(obj, nowDouble, futureDouble);
			});*/

			if (deltaVehUpdate == 0.001 && now >= 1.0) {
				if (now % 1 == 0) {
					if (pb != null) {
						pb.stepTo(1000L);
					}
					pb = new ProgressBar("Progress to second " + Math.ceil(now + 1) + " of simtime", 1000L);
				}
				pb.step();
			}
			//Update simulation time in the AgentBroker
			ab.updateTime(chron, vehsToUpdate);
			//Execute MAPE loop at time interval
			ab.executeMAPE(chron);
		}

		switch (ev.getTag()) {
			case CloudSimTags.RESOURCE_CHARACTERISTICS_REQUEST:
				this.processResourceCharacteristicsRequest(ev);
				break;

			case CloudSimTags.RESOURCE_CHARACTERISTICS:
				this.processResourceCharacteristics(ev);
				break;

			case CloudSimTags.VM_CREATE_ACK:
				this.processVmCreate(ev);
				break;

			case GENERATE_OSMESIS_WITH_RESOLUTION: {
				// Registering an app that was determined dynamically
				OsmoticAppDescription app = (OsmoticAppDescription) ev.getData();
				int melId=-1;
				if (!melRouting.test(app.getMELName())){
					melId = getVmIdByName(app.getMELName());
				}
				if(app.getAppStartTime() == -1){
					app.setAppStartTime(chron);
				}
				app.setMelId(melId);
				int vmIdInCloud = this.getVmIdByName(app.getVmName());
				int edgeDatacenterId = this.getDatacenterIdByVmId(melId);
				app.setEdgeDcId(edgeDatacenterId);
				app.setEdgeDatacenterName(this.getDatacenterNameById(edgeDatacenterId));
				int cloudDatacenterId = this.getDatacenterIdByVmId(vmIdInCloud);
				app.setCloudDcId(cloudDatacenterId);
				app.setCloudDatacenterName(this.getDatacenterNameById(cloudDatacenterId));
				this.appList.add(app);
				// After this set up. then we can generate the osmesis!
				if((chron >= app.getStartDataGenerationTime()) &&
						(chron < app.getStopDataGenerationTime()) &&
						!app.getIsIoTDeviceDied()){
					logger.info(app.getIoTDeviceName()+" starts sending via "+app.getMELName()+" at "+ chron);
					sendNow(app.getIoTDeviceId(), OsmoticTags.SENSING, app);
					// Not sending a new event again, as the communication now is one-shot
				}
				break;
			}
			case OsmoticTags.GENERATE_OSMESIS:
				generateIoTData(ev);
				break;

			case OsmoticTags.Transmission_ACK:
				askMelToProccessData(ev);
				break;

			case CloudSimTags.CLOUDLET_RETURN:
				processCloudletReturn(ev);
				break;

			case OsmoticTags.Transmission_SDWAN_ACK:
				askCloudVmToProccessData(ev);
				break;

			case CloudSimTags.END_OF_SIMULATION: // just printing
				this.shutdownEntity();
				break;

			case OsmoticTags.ROUTING_MEL_ID_RESOLUTION:
				this.melResolution(ev);

			default:
				break;
		}
	}

	MELSwitchPolicy melRouting;
	public MELSwitchPolicy getMelRouting() {
		return melRouting;
	}
	public void setMelRouting(MELSwitchPolicy melRouting) {
		this.melRouting = melRouting;
	}

	private void melResolution(SimEvent ev) {
		Flow flow = (Flow) ev.getData();
		String melName = flow.getAppNameDest();
		String IoTDevice = flow.getAppNameSrc();
		var actualIoT = iotDeviceNameToObject.get(IoTDevice);
		int mel_id = -1;

		flow.setActualEdgeDevice(melName);
		if (melRouting.test(melName)){
			// Using a policy for determining the next MEL
			String melInstanceName = melRouting.apply(actualIoT, melName, this);
			if (melInstanceName == null) return; // Ignoring the communication if no alternative is given
			flow.setAppNameDest(melInstanceName);
			mel_id = getVmIdByName(melInstanceName); //name of VM
			//dynamic mapping to datacenter
			int edgeDatacenterId = this.getDatacenterIdByVmId(mel_id);
			flow.setDatacenterId(edgeDatacenterId);
			flow.setDatacenterName(this.getDatacenterNameById(edgeDatacenterId));
			flow.getWorkflowTag().setSourceDCName(this.getDatacenterNameById(edgeDatacenterId));
		} else {
			mel_id = getVmIdByName(melName); //name of VM

			//dynamic mapping to datacenter
			int edgeDatacenterId = this.getDatacenterIdByVmId(mel_id);
			flow.setDatacenterId(edgeDatacenterId);
			flow.setDatacenterName(this.getDatacenterNameById(edgeDatacenterId));
			flow.getWorkflowTag().setSourceDCName(this.getDatacenterNameById(edgeDatacenterId));
		}

		flow.setDestination(mel_id);
		sendNow(flow.getDatacenterId(), OsmoticTags.TRANSMIT_IOT_DATA, flow);
	}

	protected void processCloudletReturn(SimEvent ev)
	{
		Cloudlet cloudlet = (Cloudlet) ev.getData();
		getCloudletReceivedList().add(cloudlet);
		EdgeLet edgeLet = (EdgeLet) ev.getData();
		if(!edgeLet.getIsFinal()){
			transferEvents(ev);
			return;
		}
		edgeLet.getWorkflowTag(). setFinishTime(MainEventManager.clock());
	}
	public void transferEvents(SimEvent ev) {

		/*RocksDB.loadLibrary();

		try (final Options options = new Options().setCreateIfMissing(true)) {
			try (final RocksDB db = RocksDB.open(options, "C:/Users/rohin/SimulatorBridger/SimulatorBridger/RocksDB-SimulatorBridger")) {
				byte[] key1 = new byte[0];
				byte[] key2 = new byte[1];
				try {
					final byte[] value = db.get(key1);
					if (value != null) {  // value == null if key1 does not exist in db.
						db.put(key2, value);
					}
					db.delete(key1);
				} catch (RocksDBException e) {
					throw new RuntimeException(e);
				}
			}
		} catch (RocksDBException e) {
			throw new RuntimeException(e);
		}*/

		float maxEdgeBW = 100;
		int messageSize = (int) this.getAppById(1).getMELOutputSize();

		change = choice.equals("MEL") ? ((EdgeLet) ev.getData()).getWorkflowTag().getIotDeviceFlow().getAppNameDest() : getAppById(((EdgeLet) ev.getData()).getOsmesisAppId()).getMELName();

		edgeToCloudBandwidth.putIfAbsent(change, maxEdgeBW);
		activePerSource.putIfAbsent(change, 0);
		eventQueue.add(ev);

		float bw = edgeToCloudBandwidth.get(change);
		int limit = DataCenterWithController.getCommunication_limit() == 0 ? Integer.MAX_VALUE : DataCenterWithController.getCommunication_limit();
		/*if(activePerSource.get(((EdgeLet) ev.getData()).getWorkflowTag().getSourceDCName()) < limit) {
			limit = bw >= messageSize ? Integer.MAX_VALUE : 10;
		}*/

		var toDelete = new TreeSet<SimEvent>();
		for (var x : eventQueue) {
			var streamMEL = choice.equals("MEL") ? ((EdgeLet) x.getData()).getWorkflowTag().getIotDeviceFlow().getAppNameDest() : getAppById(((EdgeLet) x.getData()).getOsmesisAppId()).getMELName();
			if (eventMap.values().stream().filter(v -> v.equals(streamMEL)).count() < limit) {
				eventMap.put(x, streamMEL);
				toDelete.add(x);
			}
		}
		eventQueue.removeAll(toDelete);
		toDelete.clear();

		while(!eventMap.isEmpty()) {
			SimEvent newEv = eventMap.entrySet().iterator().next().getKey();
			var dest = ((EdgeLet) newEv.getData()).getWorkflowTag().getEdgeLet().getWorkflowTag().getIotDeviceFlow().getAppNameDest();
			//bw = edgeToCloudBandwidth.get(dest);
			/*if(bw > (float) messageSize / 2) {
				limit = 1;
			}*/
			change = choice.equals("MEL") ? ((EdgeLet) newEv.getData()).getWorkflowTag().getIotDeviceFlow().getAppNameDest() : getAppById(((EdgeLet) newEv.getData()).getOsmesisAppId()).getMELName();
			eventMap.remove(newEv, change);
			if (activePerSource.get(change) < limit) {
				askMelToSendDataToCloud(newEv);
			} else {
				waitQueue.add(newEv);
			}
		}

		for (var x : waitQueue) {
			eventQueue.add(x);
			toDelete.add(x);
		}
		waitQueue.removeAll(toDelete);
	}

	private void askMelToProccessData(SimEvent ev) {
		Flow flow = (Flow) ev.getData();
		EdgeLet edgeLet = generateEdgeLet(flow.getOsmesisEdgeletSize());
		edgeLet.setVmId(flow.getDestination());
		edgeLet.setCloudletLength(flow.getOsmesisEdgeletSize());
		edgeLet.isFinal(false);
		edgeletList.add(edgeLet);
		int appId = flow.getOsmesisAppId();
		edgeLet.setOsmesisAppId(appId);
		edgeLet.setWorkflowTag(flow.getWorkflowTag());
		edgeLet.getWorkflowTag().setEdgeLet(edgeLet);
		this.setCloudletSubmittedList(edgeletList);
		sendNow(flow.getDatacenterId(), CloudSimTags.CLOUDLET_SUBMIT, edgeLet);
	}

	private EdgeLet generateEdgeLet(long length) {
		long fileSize = 30;
		long outputSize = 1;
		EdgeLet edgeLet = new EdgeLet(edgeLetId.getAndIncrement(), length, 1, fileSize, outputSize, new UtilizationModelFull(), new UtilizationModelFull(),
				new UtilizationModelFull());
		edgeLet.setUserId(this.getId());
//		LegacyTopologyBuilder.edgeLetId++;
		return edgeLet;
	}

	protected void askCloudVmToProccessData(SimEvent ev) {
		Flow flow = (Flow) ev.getData();
		int appId = flow.getOsmesisAppId();
		int dest = flow.getDestination();
		OsmoticAppDescription app = getAppById(appId);
		long length = app.getOsmesisCloudletSize();
		EdgeLet cloudLet =	generateEdgeLet(length);
		cloudLet.setVmId(dest);
		cloudLet.isFinal(true);
		edgeletList.add(cloudLet);
		cloudLet.setOsmesisAppId(appId);
		cloudLet.setWorkflowTag(flow.getWorkflowTag());
		cloudLet.getWorkflowTag().setCloudLet(cloudLet);
		this.setCloudletSubmittedList(edgeletList);
		cloudLet.setUserId(OsmoticBroker.brokerID);
		this.setCloudletSubmittedList(edgeletList);
		int dcId = getDatacenterIdByVmId(dest);
		sendNow(dcId, CloudSimTags.CLOUDLET_SUBMIT, cloudLet);
	}

	private void askMelToSendDataToCloud(SimEvent ev){
			EdgeLet edgeLet = (EdgeLet) ev.getData();
			int osmesisAppId = edgeLet.getOsmesisAppId();
			OsmoticAppDescription app = getAppById(osmesisAppId);
			int sourceId = edgeLet.getVmId(); // MEL or VM
			int destId = this.getVmIdByName(app.getVmName()); // MEL or VM
			int id = flowId.getAndIncrement();
			int melDatacenter = this.getDatacenterIdByVmId(sourceId);
			int thisSource = ev.getSource();

			change = choice.equals("MEL") ? ((EdgeLet) ev.getData()).getWorkflowTag().getIotDeviceFlow().getAppNameDest() : getAppById(((EdgeLet) ev.getData()).getOsmesisAppId()).getMELName();

			int thisActive = activePerSource.get(change);
			thisActive++;
			String curMEl = ((EdgeLet) ev.getData()).getWorkflowTag().getSourceDCName();
			activePerSource.put(change, thisActive);
			activeCount++;
			comCount++;

			Flow flow = new Flow(app.getMELName(), app.getVmName(), sourceId, destId, id, null, app);
			flow.setAppName(app.getAppName());
			flow.addPacketSize(app.getMELOutputSize());
			flow.setSubmitTime(MainEventManager.clock());
			flow.setOsmesisAppId(osmesisAppId);
			flow.setWorkflowTag(edgeLet.getWorkflowTag());
			flow.getWorkflowTag().setEdgeToCloudFlow(flow);
			workflowTag.add(flow.getWorkflowTag());
			sendNow(melDatacenter, OsmoticTags.BUILD_ROUTE, flow);
	}

	private OsmoticAppDescription getAppById(int osmesisAppId) {
		OsmoticAppDescription osmesis = null;
		for(OsmoticAppDescription app : this.appList){
			if(app.getAppID() == osmesisAppId){
				osmesis = app;
			}
		}
		return osmesis;
	}

	HashMultimap<String, String> map = null;
	public Set<String> selectVMFromHostPredicate(String melId) {
		if (map == null) {
			map = HashMultimap.create();
		}
		for (var cp : mapVmsToDatacenter.entrySet()) {
			for (var vmOrMel : cp.getValue()) {
				var host = vmOrMel.getHost();
				if (host instanceof EdgeDevice) {
					String toStartRegex = vmOrMel.getVmName();
					toStartRegex = toStartRegex.substring(0, toStartRegex.lastIndexOf('.'))+".*";
					map.put(toStartRegex, ((EdgeDevice) host).getDeviceName());
				}
			}
		}
		return map.get(melId);
	}
	public Collection<String> selectVMFromHostPredicate() {
		if (map == null) {
			map = HashMultimap.create();
		}
		for (var cp : mapVmsToDatacenter.entrySet()) {
			for (var vmOrMel : cp.getValue()) {
				var host = vmOrMel.getHost();
				if (host instanceof EdgeDevice) {
					String toStartRegex = vmOrMel.getVmName();
					toStartRegex = toStartRegex.substring(0, toStartRegex.lastIndexOf('.'))+".*";
					map.put(toStartRegex, ((EdgeDevice) host).getDeviceName());
				}
			}
		}
		return map.values();
	}

	public Host resolveHostFromMELId(String melId) {
		for (var cp : mapVmsToDatacenter.entrySet()) {
			for (var vmOrMel : cp.getValue()) {
				if (vmOrMel.getVmName().equals(melId)) {
					return vmOrMel.getHost();
				}
			}
		}
		return null;
	}

	public EdgeDevice resolveEdgeDeviceFromId(String hostId) {
		for (var cp : mapVmsToDatacenter.entrySet()) {
			for (var vmOrMel : cp.getValue()) {
				var host = vmOrMel.getHost();
				if (host instanceof EdgeDevice) {
					if (((EdgeDevice)host).getDeviceName().equals(hostId))
						return (EdgeDevice) host;
				}
			}
		}
		return null;
	}

	public void submitVmList(List<? extends Vm> list, int datacenterId) {
		mapVmsToDatacenter.put(datacenterId, list);
		getVmList().addAll(list);
	}

	protected void createVmsInDatacenter(int datacenterId) {
		int requestedVms = 0;
		List<? extends Vm> vmList = mapVmsToDatacenter.get(datacenterId);
		if(vmList != null){
			for (int i = 0; i < vmList.size(); i++) {
				Vm vm = vmList.get(i);
					sendNow(datacenterId, CloudSimTags.VM_CREATE_ACK, vm);
					requestedVms++;
			}
		}
		getDatacenterRequestedIdsList().add(datacenterId);
		setVmsRequested(requestedVms);
		setVmsAcks(0);
	}

	@Override
	protected void processOtherEvent(SimEvent ev) {

	}

	@Override
	public void processVmCreate(SimEvent ev) {
		super.processVmCreate(ev);
		if (allRequestedVmsCreated()) {
			for(OsmoticAppDescription app : this.appList){
				int iotDeviceID = getiotDeviceIdByName(app.getIoTDeviceName());

				//This is necessary for osmotic flow abstract routing.
				int melId=-1;
				if (!melRouting.test(app.getMELName())){
					melId = getVmIdByName(app.getMELName());
				}
				app.setMelId(melId);
				int vmIdInCloud = this.getVmIdByName(app.getVmName());
				app.setIoTDeviceId(iotDeviceID);
				int edgeDatacenterId = this.getDatacenterIdByVmId(melId);
				app.setEdgeDcId(edgeDatacenterId);
				app.setEdgeDatacenterName(this.getDatacenterNameById(edgeDatacenterId));
				int cloudDatacenterId = this.getDatacenterIdByVmId(vmIdInCloud);
				app.setCloudDcId(cloudDatacenterId);
				app.setCloudDatacenterName(this.getDatacenterNameById(cloudDatacenterId));
				if(app.getAppStartTime() == -1){
					app.setAppStartTime(MainEventManager.clock());
				}
				double delay = app.getDataRate()+app.getStartDataGenerationTime();
				send(this.getId(), delay, OsmoticTags.GENERATE_OSMESIS, app);
			}
		}
	}

	private void generateIoTData(SimEvent ev){
		OsmoticAppDescription app = (OsmoticAppDescription) ev.getData();
		if((MainEventManager.clock() >= app.getStartDataGenerationTime()) &&
				(MainEventManager.clock() < app.getStopDataGenerationTime()) &&
				!app.getIsIoTDeviceDied()){
			sendNow(app.getIoTDeviceId(), OsmoticTags.SENSING, app);
			send(this.getId(), app.getDataRate(), OsmoticTags.GENERATE_OSMESIS, app);
		}
	}

	private boolean allRequestedVmsCreated() {
		return this.getVmsCreatedList().size() == getVmList().size() - getVmsDestroyed();
	}

	public void submitOsmesisApps(List<OsmoticAppDescription> appList) {
		this.appList = appList;
	}

	public List<OsmoticAppDescription> submitWorkloadCSVApps(List<WorkloadCSV> appList) {
		this.appList = appList.stream().map(GlobalConfigurationSettings::asLegacyApp).collect(Collectors.toList());
		return this.appList;
	}

	public int getiotDeviceIdByName(String melName){
		return this.iotDeviceNameToId.get(melName);
	}
	public IoTDevice getiotDeviceByName(String melName){
		return this.iotDeviceNameToObject.get(melName);
	}

	public void mapVmNameToId(Map<String, Integer> melNameToIdList) {
		this.iotVmIdByName.putAll(melNameToIdList);
	}

	public int getVmIdByName(String name){
		Integer val = this.iotVmIdByName.get(name);
		if (val == null)
			throw new RuntimeException("ERROR ON: "+name);
		return val;
	}

	public void setDatacenters(List<OsmoticDatacenter> osmesisDatacentres) {
		this.datacenters = osmesisDatacentres;
	}

	private int getDatacenterIdByVmId(int vmId){
		int dcId = 0;
		for(OsmoticDatacenter dc :datacenters){
			for(Vm vm : dc.getVmList()){
				if(vm.getId() == vmId){
					dcId = dc.getId();
				}
			}
		}
		return dcId;
	}

	private String getDatacenterNameById(int id){
		String name = "";
		for(OsmoticDatacenter dc :datacenters){
			if(dc.getId() == id){
				name = dc.getName();
			}
		}
		return name;
	}

	public void addIoTDevice(IoTDevice device) {
		iotDeviceNameToId.put(device.getName(), device.getId());
		iotDeviceNameToObject.put(device.getName(), device);
	}



	public void setIoTTraces(IoTEntityGenerator ioTEntityGenerator) {
		this.ioTEntityGenerator = ioTEntityGenerator;
	}

	public void setDeltaVehUpdate(double deltaVehUpdate) {
		this.deltaVehUpdate = deltaVehUpdate;
	}

	public static double getDeltaVehUpdate() {
		return deltaVehUpdate;
	}
}
