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



import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.Pe;
import org.cloudbus.cloudsim.provisioners.*;
import uk.ncl.giacomobergami.components.allocation_policy.VmSchedulerTimeSharedEnergy;
import uk.ncl.giacomobergami.components.simulator.OsmoticWrapper;
import uk.ncl.giacomobergami.utils.gir.CartesianPoint;

/**
 * 
 * @author Khaled Alwasel
 * @contact kalwasel@gmail.com
 * @since IoTSim-Osmosis 1.0
 * 
**/

public class EdgeDevice extends Host implements CartesianPoint {
	private String deviceName;	
	public Mobility.Location location;
	public double signalRange;
	private boolean enabled;
	public double max_vehicle_communication;
	
	EdgeDevice(int id, String deviceName, RamProvisioner ramProvisioner, BwProvisioner bwProvisioner,
			long storage, List<? extends Pe> peList) {
		super(id, ramProvisioner, bwProvisioner, storage, peList,
				new VmSchedulerTimeSharedEnergy(peList));
		this.deviceName = deviceName;
		this.enabled = true;
		location = new Mobility.Location(0,0,0);
		signalRange = Double.MAX_VALUE;
	}

	public static List<Pe> generatePEList( LegacyConfiguration.EdgeDeviceEntity hostEntity) {
		return IntStream.range(0, hostEntity.getPes())
				.mapToObj(i -> new Pe(i, new PeProvisionerSimple(hostEntity.getMips())))
				.collect(Collectors.toList());
	}

    public EdgeDevice(AtomicInteger idGen,
					  LegacyConfiguration.EdgeDeviceEntity hostEntity) {
        this(idGen.getAndIncrement(),
				hostEntity.getName(),
				new RamProvisionerSimple(hostEntity.getRamSize()),
				new BwProvisionerSimple(hostEntity.getBwSize()),
				hostEntity.getStorage(),
				generatePEList(hostEntity));

		location = hostEntity.location;
		signalRange = hostEntity.signalRange;
		max_vehicle_communication = hostEntity.max_vehicle_communication;

		OsmoticWrapper.melList.putIfAbsent(hostEntity.name, (double)hostEntity.getMips());
    }
    public String getDeviceName() {
		return deviceName;
	}

	public boolean isEnabled() {
		return this.enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	@Override
	public double getX() {
		return location.x;
	}

	@Override
	public double getY() {
		return location.y;
	}
}
