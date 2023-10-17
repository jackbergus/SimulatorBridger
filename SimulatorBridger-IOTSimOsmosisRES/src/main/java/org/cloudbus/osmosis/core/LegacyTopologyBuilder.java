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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cloudbus.cloudsim.DatacenterCharacteristics;
import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.Storage;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.edge.core.edge.EdgeDataCenter;
import org.cloudbus.cloudsim.edge.core.edge.EdgeDevice;
import org.cloudbus.cloudsim.edge.core.edge.LegacyConfiguration;
import org.cloudbus.cloudsim.edge.core.edge.LegacyConfiguration.CloudDataCenterEntity;
import org.cloudbus.cloudsim.edge.core.edge.LegacyConfiguration.EdgeDataCenterEntity;
import org.cloudbus.cloudsim.edge.core.edge.LegacyConfiguration.LogEntity;
import org.cloudbus.cloudsim.edge.core.edge.MEL;
import org.cloudbus.cloudsim.sdn.Switch;
import uk.ncl.giacomobergami.components.allocation_policy.VmAllocationPolicyGeneratorFactory;
import uk.ncl.giacomobergami.components.iot.IoTDevice;
import uk.ncl.giacomobergami.components.iot.IoTGeneratorFactory;
import uk.ncl.giacomobergami.components.loader.GlobalConfigurationSettings;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * 
 * @author Khaled Alwasel, Tomasz Szydlo
 * @contact kalwasel@gmail.com
 * @since IoTSim-Osmosis 1.0
 * 
**/
public class LegacyTopologyBuilder {
	private OsmoticBroker broker;
	private static AtomicInteger flowId = new AtomicInteger(1);
	private static AtomicInteger edgeLetId = new AtomicInteger(1);
	private SDNController sdWanController;
	
	private static AtomicInteger hostId = new AtomicInteger(1);
	private static AtomicInteger vmId = new AtomicInteger(1);

	private static final
	Logger logger = LogManager.getRootLogger();
	
	public SDNController getSdWanController() {
		return sdWanController;
	}
	private List<OsmoticDatacenter> osmesisDatacentres;
	  
    public LegacyTopologyBuilder(OsmoticBroker osmesisBroker) {
    	this.broker = osmesisBroker;
    	this.osmesisDatacentres = new ArrayList<>();
	}

	public static OsmoticBroker newBroker() {
		// TODO: make this as a singleton, so to return only the currently available instance, while ensuring uniqueness
        return OsmoticBroker.getInstance("OsmesisBroker", edgeLetId, flowId);
		//new OsmoticBroker("OsmesisBroker", edgeLetId, flowId);
	}

	public LegacyTopologyBuilder buildTopology(File filename) {
		return buildTopology(Objects.requireNonNull(LegacyConfiguration.fromFile(Objects.requireNonNull(filename))));
	}

	public List<OsmoticDatacenter> getOsmesisDatacentres() {
		return osmesisDatacentres;
	}

    public LegacyTopologyBuilder buildTopology(LegacyConfiguration topologyEntity) {
		new GlobalConfigurationSettings().fromLegacyConfiguration(topologyEntity);

		List<Switch> datacenterGateways = new ArrayList<>();
		for (var x : topologyEntity.getCloudDatacenter()) {
			var y = createCloudDatacenter(x);
			var controller = y.getSdnController();
			datacenterGateways.add(controller.getGateway());
			osmesisDatacentres.add(y);
		}
		for (var x : topologyEntity.getEdgeDatacenter()) {
			var y = buildEdgeDatacenter(x);
			var controller = y.getSdnController();
			datacenterGateways.add(controller.getGateway());
			osmesisDatacentres.add(y);
		}


        sdWanController = new SDWANController(topologyEntity.getSdwan().get(0), datacenterGateways);
		osmesisDatacentres.forEach(datacenter -> datacenter.getSdnController().setWanController(sdWanController));
        sdWanController.addAllDatacenters(osmesisDatacentres);
		return this;
    }

	private CloudDatacenter createCloudDatacenter(CloudDataCenterEntity datacentreEntity) {
		SDNController sdnController = new CloudSDNController(datacentreEntity.getControllers().get(0));
		List<Host> hostList = sdnController.getHostList();
		LinkedList<Storage> storageList = new LinkedList<>();
		DatacenterCharacteristics characteristics = new DatacenterCharacteristics(hostList);

		// Create Datacenter with previously set parameters
		try {
			// Why to use maxHostHandler!
			var loc_datacentre = new CloudDatacenter(datacentreEntity,
					                                 characteristics,
													 VmAllocationPolicyGeneratorFactory.generateFacade(datacentreEntity.getVmAllocationPolicy()),
					                                 storageList,
					                                 0,
					                                 sdnController,
					hostId);

			List<Vm> vmList = datacentreEntity
					.getVMs()
					.stream()
					.map(x -> {
						var vm = new Vm(x, this.broker, vmId);
						loc_datacentre.mapVmNameToID(vm.getId(), vm.getVmName());
						return vm;
					})
					.collect(Collectors.toList());

			this.broker.mapVmNameToId(loc_datacentre.getVmNameToIdList());
			loc_datacentre.setVmList(vmList);
			loc_datacentre.setDCAndAddVMsToSDNHosts();
			loc_datacentre.getVmAllocationPolicy().setUpVmTopology(loc_datacentre.getHosts());
			return loc_datacentre;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	private EdgeDataCenter buildEdgeDatacenter(EdgeDataCenterEntity edgeDCEntity) {
		if (edgeDCEntity.getControllers().size() > 1)
			throw new RuntimeException("Expected size 1 for "+edgeDCEntity.getControllers().size());

		var hostList = edgeDCEntity.getHosts()
				.stream()
				.map(x -> new EdgeDevice(hostId, x))
				.collect(Collectors.toList());

		LinkedList<Storage> storageList = new LinkedList<>();

		// 6. Finally, we need to create a PowerDatacenter object.
		EdgeDataCenter datacenter = new EdgeDataCenter(edgeDCEntity,
				hostList,
				storageList,
				edgeDCEntity.getSchedulingInterval());
		logger.trace("Edge SDN cotroller " + edgeDCEntity.getName() + "has been created");

		var MELList = edgeDCEntity.getMELEntities()
				.stream()
				.map(x -> {
					var mel = new MEL(datacenter.getId(),
							vmId, x, broker);
					datacenter.mapVmNameToID(mel.getId(), mel.getVmName());
					return mel;
				})
				.collect(Collectors.toList());
		datacenter.setVmList(MELList);

		broker.mapVmNameToId(datacenter.getVmNameToIdList());
		datacenter.getVmAllocationPolicy().setUpVmTopology(hostList);
		datacenter.getSdnController().addVmsToSDNhosts(MELList);
		var associatedEdge = edgeDCEntity.getName();

		edgeDCEntity.getIoTDevices()
				.forEach(x -> {
					IoTDevice newInstance = IoTGeneratorFactory.generateFacade(x, flowId);
					if ((associatedEdge != null) && (!associatedEdge.isEmpty()))
						newInstance.setAssociatedEdge(associatedEdge);
					broker.addIoTDevice(newInstance);
				});
		return datacenter;
	}
}
