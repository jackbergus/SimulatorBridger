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

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import com.google.common.collect.HashMultimap;
import org.cloudbus.agent.AgentBroker;
import org.cloudbus.agent.CentralAgent;
import org.cloudbus.cloudsim.*;
import org.cloudbus.cloudsim.core.MainEventManager;
import org.cloudbus.cloudsim.core.CloudSimTags;
import org.cloudbus.cloudsim.core.SimEvent;
import org.cloudbus.cloudsim.edge.core.edge.EdgeDevice;
import org.cloudbus.cloudsim.edge.core.edge.EdgeLet;
import uk.ncl.giacomobergami.components.iot.IoTDevice;
import uk.ncl.giacomobergami.components.iot.IoTEntityGenerator;
import uk.ncl.giacomobergami.components.loader.GlobalConfigurationSettings;
import uk.ncl.giacomobergami.components.mel_routing.MELSwitchPolicy;
import uk.ncl.giacomobergami.utils.asthmatic.WorkloadCSV;

import static org.cloudbus.cloudsim.core.CloudSimTags.MAPE_WAKEUP_FOR_COMMUNICATION;
import static org.cloudbus.osmosis.core.OsmoticTags.GENERATE_OSMESIS_WITH_RESOLUTION;

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
	public static int getQueueSize() {
		return eventQueue.size();
	}
	public static TreeSet<SimEvent> getQueue() {
		return eventQueue;
	}
	protected static TreeMap<SimEvent, String> eventHashMap = new TreeMap<>(Collections.reverseOrder());
	public static  int getEventHashMapSize(){return eventHashMap.size();}
	public static TreeMap<SimEvent, String> getEventHashMap() {return eventHashMap; }
	public static HashMap<String, Integer> activePerSource = new HashMap<>();
	private static TreeSet<SimEvent> waitQueue = new TreeSet<>();
	protected static HashMap<String, Float> edgeToCloudBandwidth = new HashMap<>();
	public static HashMap<String, Float> getEdgeToCloudBandwidth() { return edgeToCloudBandwidth; }
	public static void updateEdgeTOCloudBandwidth(String id, float bw) {
		edgeToCloudBandwidth.replace(id, bw);
	}
	//////////////////////////////////////////////////////////////////////////////////////////////
	public CentralAgent osmoticCentralAgent;
	private AtomicInteger flowId;
	private IoTEntityGenerator ioTEntityGenerator;
	public static double deltaVehUpdate;

	public OsmoticBroker(String name,
						 AtomicInteger edgeLetId,
						 AtomicInteger flowId) {
		super(name);
		this.edgeLetId = edgeLetId;
		this.flowId = flowId;
		this.appList = new ArrayList<>();
		brokerID = this.getId();
		isWakeupStartSet = false;
	}

	@Override
	public void startEntity() {
		super.startEntity();
	}

	@Override
	public void processEvent(SimEvent ev) {
		double chron = MainEventManager.clock();
		var ab = AgentBroker.getInstance();

		// Setting up the forced times when the simulator has to wake up, as new messages have to be sent
		if (!isWakeupStartSet) {
			for (Double forcedWakeUpTime :
					ioTEntityGenerator.collectionOfWakeUpTimes()) {
				double time = forcedWakeUpTime - chron;
				if (time > 0.0) {
					schedule(OsmoticBroker.brokerID, time, MAPE_WAKEUP_FOR_COMMUNICATION, null);
				}
			}
			isWakeupStartSet = true;
		}

		if (ev.getTag() == MAPE_WAKEUP_FOR_COMMUNICATION) {
			logger.trace("WakeUp Call @"+chron);
		}

		// Updates the IoT Device with the geo-location information
		iotDeviceNameToObject.forEach((id, obj) -> {
			ioTEntityGenerator.updateIoTDevice(obj, chron, chron+deltaVehUpdate);
		});

		//Update simulation time in the AgentBroker
		ab.updateTime(chron);

		//Execute MAPE loop at time interval
		ab.executeMAPE(chron);

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
					app.setAppStartTime(MainEventManager.clock());
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
				if((MainEventManager.clock() >= app.getStartDataGenerationTime()) &&
						(MainEventManager.clock() < app.getStopDataGenerationTime()) &&
						!app.getIsIoTDeviceDied()){
					logger.info(app.getIoTDeviceName()+" starts sending via "+app.getMELName()+" at "+MainEventManager.clock());
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
		float maxEdgeBW = (float) ((EdgeLet) ev.getData()).getWorkflowTag().getIotDeviceFlow().getFlowBandwidth();
		int messageSize = (int) this.getAppById(1).getMELOutputSize();

		var choice = "MEL";
		var change = choice.equals("MEL") ? ((EdgeLet) ev.getData()).getWorkflowTag().getIotDeviceFlow().getAppNameDest() : getAppById(((EdgeLet) ev.getData()).getOsmesisAppId()).getMELName();

		edgeToCloudBandwidth.putIfAbsent(change, maxEdgeBW);
		activePerSource.putIfAbsent(((EdgeLet) ev.getData()).getWorkflowTag().getSourceDCName(), 0);
		eventQueue.add(ev);

		float bw = edgeToCloudBandwidth.get(change);
		int limit = Integer.MAX_VALUE;
		/*if(activePerSource.get(((EdgeLet) ev.getData()).getWorkflowTag().getSourceDCName()) < limit) {
			limit = bw >= messageSize ? Integer.MAX_VALUE : 10;
		}*/

		var toDelete = new TreeSet<SimEvent>();
		for (var x : eventQueue) {
			if (eventHashMap.values().stream().filter(v -> v.equals(((EdgeLet) x.getData()).getWorkflowTag().getSourceDCName())).count() < limit) {
				eventHashMap.put(x, ((EdgeLet) x.getData()).getWorkflowTag().getSourceDCName());
				toDelete.add(x);
			}
		}
		eventQueue.removeAll(toDelete);
		toDelete.clear();

		while(eventHashMap.size() > 0) {
			SimEvent newEv = eventHashMap.entrySet().iterator().next().getKey();
			/*bw = edgeToCloudBandwidth.get(getAppById(((EdgeLet) newEv.getData()).getOsmesisAppId()).getMELName());
			limit = 3;
			if(activePerSource.get(((EdgeLet) newEv.getData()).getWorkflowTag().getSourceDCName()) < limit) {
				limit = bw >= messageSize ? Integer.MAX_VALUE : 3;
			}*/
			eventHashMap.remove(newEv, ((EdgeLet) newEv.getData()).getWorkflowTag().getSourceDCName());
			if (activePerSource.get(((EdgeLet) newEv.getData()).getWorkflowTag().getSourceDCName()) < limit) {
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
			int melDataceneter = this.getDatacenterIdByVmId(sourceId);
			int thisSource = ev.getSource();

			int thisActive = activePerSource.get(((EdgeLet) ev.getData()).getWorkflowTag().getSourceDCName());
			thisActive++;
			String curMEl = ((EdgeLet) ev.getData()).getWorkflowTag().getSourceDCName();
			activePerSource.put(curMEl, thisActive);
			activeCount++;
			comCount++;

			Flow flow = new Flow(app.getMELName(), app.getVmName(), sourceId, destId, id, null, app);
			flow.setAppName(app.getAppName());
			flow.addPacketSize(app.getMELOutputSize());
			flow.setSubmitTime(MainEventManager.clock());
			flow.setOsmesisAppId(osmesisAppId);
			flow.setWorkflowTag(edgeLet.getWorkflowTag());
			flow.getWorkflowTag().setEdgeToCloudFlow(flow);
			sendNow(melDataceneter, OsmoticTags.BUILD_ROUTE, flow);
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
