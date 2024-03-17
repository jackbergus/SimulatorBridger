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

package uk.ncl.giacomobergami.components.iot;


import org.cloudbus.agent.AgentBroker;
import org.cloudbus.cloudsim.core.MainEventManager;
import org.cloudbus.cloudsim.core.SimEntity;
import org.cloudbus.cloudsim.core.SimEvent;
import org.cloudbus.cloudsim.edge.core.edge.Battery;
import org.cloudbus.cloudsim.edge.core.edge.LegacyConfiguration;
import org.cloudbus.cloudsim.edge.core.edge.Mobility;
import org.cloudbus.cloudsim.edge.iot.network.EdgeNetwork;
import org.cloudbus.cloudsim.edge.iot.network.EdgeNetworkInfo;
import org.cloudbus.cloudsim.edge.utils.LogUtil;
import org.cloudbus.osmosis.core.*;
import uk.ncl.giacomobergami.components.iot_protocol.IoTProtocolGeneratorFactory;
import uk.ncl.giacomobergami.utils.data.YAML;
import uk.ncl.giacomobergami.utils.gir.CartesianPoint;
import uk.ncl.giacomobergami.components.network_type.NetworkTypingGeneratorFactory;
import uk.ncl.giacomobergami.utils.pipeline_confs.TrafficConfiguration;

import java.io.File;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;

/**
 * 
 * @author Khaled Alwasel
 * @contact kalwasel@gmail.com
 * @since IoTSim-Osmosis 1.0
 * 
**/

public abstract class IoTDevice extends SimEntity implements CartesianPoint {
	public static int cloudLetId = 0;
	public boolean transmit;
    private double runningTime = 0;
	protected Battery battery;
	private EdgeNetworkInfo networkModel;
	////////////////////////////////////////
	private String netType;
	private double netLatency;
	////////////////////////////////////////
	public Mobility mobility;
	int connectingEdgeDeviceId = -1;
	private boolean enabled;
	public abstract boolean updateBatteryBySensing();
	public abstract boolean updateBatteryByTransmission();
	private double bw;
	private double usedBw;
	private final AtomicInteger flowId;
	private long totalPacketsBeingSent = 0;
	private TreeMap<Double, Double> consumptionInTime = new TreeMap<>();
	private TreeMap<Double, Long> packetsSentInTime = new TreeMap<>();
	private TreeMap<Double, Integer> actionToFlowId = new TreeMap<>();
	private HashMap<Integer, Double> flowIdCreationTime = new HashMap<>();
	private HashSet<Integer> AppIDs = new HashSet<>();
	private transient File converter_file = new File("clean_example/converter.yaml");
	private transient Optional<TrafficConfiguration> time_conf = YAML.parse(TrafficConfiguration.class, converter_file);
	private transient double beginSUMO = time_conf.get().getBegin();
	private transient double endSUMO = time_conf.get().getEnd();

	public Map<Double, Double> getTrustworthyConsumption() { return consumptionInTime; }

	public Map<Double, Long> computeTrustworthyCommunication() { return packetsSentInTime; }

	public TreeMap<Double, Integer> getActionToFlowId() {
		return actionToFlowId;
	}

	@Override
	public double getX() {
		return mobility.location.x;
	}

	@Override
	public double getY() {
		return mobility.location.y;
	}

	String associatedEdge;

	private OsmoticRoutingTable routingTable = new OsmoticRoutingTable();
//	private DeviceAgent osmoticDeviceAgent;

	private List<Flow> flowList = new ArrayList<>(); 
	
	public IoTDevice(LegacyConfiguration.IotDeviceEntity onta,
					 AtomicInteger flowId) {
		super(onta.getName());
		this.flowId = flowId;
		this.battery = new Battery();
		this.networkModel = new EdgeNetworkInfo(
						new EdgeNetwork(onta.getNetworkModelEntity().getNetworkType()),
						IoTProtocolGeneratorFactory.generateFacade(onta.getNetworkModelEntity().getCommunicationProtocol())
				);
		this.enabled = true;
		this.netType = this.getNetworkModel().getNetWorkType().getNetworkType();

		this.bw = Objects.equals(this.netType, "custom") ? onta.getBw() : NetworkTypingGeneratorFactory.generateFacade(this.netType).getNTBW();
		this.netLatency = Objects.equals(this.netType, "custom") ? onta.getLatency() : NetworkTypingGeneratorFactory.generateFacade(this.netType).getNTLat();

		//Osmosis Agents
		AgentBroker.getInstance().createDeviceAgent(onta.getName(), this);

		// Battery Setting
		battery.setMaxCapacity(onta.getMax_battery_capacity());
		if (onta.getInitial_battery_capacity()==0.0){
			battery.initCapacity(onta.getMax_battery_capacity());
			battery.setMaxCapacity(onta.getMax_battery_capacity());
		} else {
			battery.setMaxCapacity(onta.getInitial_battery_capacity());
			battery.initCapacity(onta.getInitial_battery_capacity());
		}
		battery.setBatterySensingRate(onta.getBattery_sensing_rate());
		battery.setBatterySendingRate(onta.getBattery_sending_rate());
		battery.setResPowered(onta.isRes_powered());
		battery.setPeakSolarPower(onta.getSolar_peak_power());
		battery.setBatteryVoltage(onta.getBattery_voltage());
		battery.setMaxChargingCurrent(onta.getMax_charging_current());

		// Mobility Setting
		onta.getMobilityEntity().setSignalRange(Objects.equals(this.netType, "custom") ? onta.getMobilityEntity().getSignalRange() : NetworkTypingGeneratorFactory.generateFacade(this.netType).getNTSR());
		this.mobility = new Mobility(onta.getMobilityEntity());
	}
	
	@Override
	public void startEntity() {}

	public String getAssociatedEdge() {
		return associatedEdge;
	}

	public void setAssociatedEdge(String associatedEdge) {
		this.associatedEdge = associatedEdge;
	}

	@Override
	public void processEvent(SimEvent ev) {
		switch (ev.getTag()) {
			case OsmoticTags.SENSING:
				//if (getName().equals("XXI_Aprile_1_27")) System.out.println("XXI_Aprile_1_27: SENSING @"+ + MainEventManager.clock());
				this.sensing(ev);
				break;

			case  OsmoticTags.updateIoTBW:
				//if (getName().equals("XXI_Aprile_1_27")) System.out.println("XXI_Aprile_1_27: updateIoTBW @"+ + MainEventManager.clock());
				this.removeFlow(ev);
				break;

			case OsmoticTags.MOVING: {
				//if (getName().equals("XXI_Aprile_1_27")) System.out.println("XXI_Aprile_1_27: MOVING @"+ + MainEventManager.clock());
				updateEnergyConsumptionInformation(ev, -1);
			}
			break;
		}
	}
	
	public double getRunningTime() {
		return this.runningTime;
	}

	public void setRunningTime(double runningTime) {
		this.runningTime = runningTime;
	}

	
	public Mobility getMobility() {
		return this.mobility;
	}
	
	public void setMobility(Mobility location) {
		this.mobility = location;
	}
	
	public Battery getBattery() {
		return this.battery;
	}

	public void setEdgeDeviceId(int id) {
		this.connectingEdgeDeviceId = id;
	}

	public EdgeNetworkInfo getNetworkModel() {
		return this.networkModel;
	}

	public boolean isEnabled() {
		return this.enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}
	
	private void sensing(SimEvent ev) {
		if (ev == null) {
			this.updateBatteryBySensing();
			consumptionInTime.put(MainEventManager.clock(), this.battery.getBatteryTotalConsumption());
			return;
		}
		OsmoticAppDescription app = (OsmoticAppDescription) ev.getData();
		Flow flow = this.createFlow(app);
		
		WorkflowInfo workflowTag = new WorkflowInfo();
		workflowTag.setStartTime(MainEventManager.clock());
		workflowTag.setAppId(app.getAppID());
		workflowTag.setAppName(app.getAppName());
		workflowTag.setIotDeviceFlow(flow);
		workflowTag.setWorkflowId(app.addWorkflowId(1));
		workflowTag.setSourceDCName(app.getEdgeDatacenterName());
		workflowTag.setDestinationDCName(app.getCloudDatacenterName());
		flow.setWorkflowTag(workflowTag);
		//OsmoticBroker.workflowTag.add(workflowTag);
		flow.addPacketSize(app.getIoTDeviceOutputSize());			
		updateBandwidth();

		if (updateEnergyConsumptionInformation(ev, flow.getFlowId())) {
			app.setIoTDeviceDied(true);
			LogUtil.info(this.getClass().getSimpleName() + " running time is " + MainEventManager.clock());

			this.setEnabled(false);
			LogUtil.info(this.getClass().getSimpleName()+" " + this.getId() + "'s battery has been drained");
			this.runningTime = MainEventManager.clock();
			MainEventManager.cancelAll(getId(), MainEventManager.SIM_ANY);
			return;
		}

		//Adaptive Osmosis Flow Routing
		String finalMEL = routingTable.getRule(flow.getAppNameDest());
		flow.setAppNameDest(finalMEL);

		//MEL ID Resolution in Osmotic Broker
		sendNow(OsmoticBroker.brokerID, OsmoticTags.ROUTING_MEL_ID_RESOLUTION, flow); //necessary for osmotic flow routing - concept similar to ARP protocol
	}

	private Flow createFlow(OsmoticAppDescription app) {
		//melID will be set in the osmosis broker in the MEL_ID_RESOLUTION process.
		String networkType = this.networkModel.getNetWorkType().getNetworkType();
		int melId = -1;
		int datacenterId = -1;
		datacenterId = app.getEdgeDcId();					
		int id = flowId.getAndIncrement() ;
		Flow flow  = new Flow(this.getName(), app.getMELName(), this.getId(), melId, id, null, app);
		flow.setOsmesisAppId(app.getAppID());
		flow.setAppName(app.getAppName());		
		flow.addPacketSize(app.getIoTDeviceOutputSize());
		flow.setSubmitTime(MainEventManager.clock());
		flow.setDatacenterId(datacenterId);
		flow.setOsmesisEdgeletSize(app.getOsmesisEdgeletSize());
		flow.updateFlowLatency(this.netLatency);
		flowList.add(flow);
		return flow;
	}
	
	public void setBw(double bw) {
		this.bw = bw;
	}
	
	public double getBw() {
		return bw;
	}
	
	public double getUsedBw() {
		return usedBw;
	}
	
	public void removeFlow(SimEvent ev) {
		Flow flow  = (Flow) ev.getData();
		this.flowList.remove(flow);
		flowIdCreationTime.remove(flow.getFlowId());
		updateBandwidth();
		updateEnergyConsumptionInformation(ev, -1);
	}

	private boolean updateEnergyConsumptionInformation(SimEvent ev, int flowId) {
		boolean isDrained;
		boolean isCommunicating;
		int appId = -1;
		boolean doIncrementPacketSent = flowId != -1;
		int increment = 1;

		if(MainEventManager.clock() > endSUMO) {
			MainEventManager.cancelAll(getId(), MainEventManager.SIM_ANY);
			return true;
		}

		if (this.flowList.isEmpty()) {
			// If there is no flow, then the device is not communicating, and therefore the battery should be
			// updated as only in sensing

			isDrained = this.updateBatteryBySensing();
			isCommunicating = false;
		} else {
			if (doIncrementPacketSent) {
				flowIdCreationTime.put(flowId, MainEventManager.clock());
			} else {
				increment = 0;
				for (var flow : this.flowList)
					if (flowIdCreationTime.get(flow.getFlowId()) > OsmoticBroker.deltaVehUpdate) {
						increment++;
						flowIdCreationTime.put(flow.getFlowId(), MainEventManager.clock());
					}
				doIncrementPacketSent = increment > 0;
			}
			if(ev.getSource() == 2) { //source ID 2 is the ID for OsmesisBroker
				doIncrementPacketSent = OsmoticBroker.getInstance("OsmesisBroker", new AtomicInteger(), new AtomicInteger()).resolveEdgeDeviceFromId(((OsmoticAppDescription) ev.getData()).getMELName().substring(1)) == null ? false : doIncrementPacketSent;
			}
			OsmoticAppDescription app =null;
			if (ev != null) {
				var obj = ev.getData();
				if (obj instanceof OsmoticAppDescription) {
					app = ((OsmoticAppDescription)obj);
					appId = app.getAppID();
				} else if (obj instanceof Flow) {
					app = ((Flow)obj).getApp();
					appId = app.getAppID();
				}
			}
			isDrained = this.updateBatteryBySensing();
			if (!isDrained) {
				if (doIncrementPacketSent && (!AppIDs.contains((appId)))) {
					for (int i = 0; i<increment; i++)
						isDrained |= this.updateBatteryByTransmission();
				}
			}
			if (app != null)
				app.setIoTBatteryConsumption(this.battery.getBatteryTotalConsumption());
			isCommunicating = true;
		}
		double time = ev == null ? MainEventManager.clock() : ev.eventTime();

		if (doIncrementPacketSent && isCommunicating && (!isDrained) && (!AppIDs.contains((appId)))) {
			totalPacketsBeingSent += increment;
			consumptionInTime.put(time, this.battery.getBatteryTotalConsumption());
			actionToFlowId.put(time, appId);
		}
		packetsSentInTime.put(time, totalPacketsBeingSent);

		AppIDs.add(appId);
		return isDrained;
	}

	private void updateBandwidth(){			
		this.usedBw = this.getBw() / this.flowList.size(); // the updated bw 		
		for(Flow getFlow : this.flowList){
			getFlow.updateSourceBw(this.usedBw);
		}	
	}

	public OsmoticRoutingTable getRoutingTable() {
		return routingTable;
	}

	public HashSet<Integer> getAppIDs() {
		return AppIDs;
	}
}