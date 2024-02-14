package uk.ncl.giacomobergami.components.loader;

import org.cloudbus.cloudsim.Storage;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.edge.core.edge.EdgeDataCenter;
import org.cloudbus.cloudsim.edge.core.edge.EdgeDevice;
import org.cloudbus.cloudsim.edge.core.edge.LegacyConfiguration;
import org.cloudbus.cloudsim.edge.core.edge.MEL;
import org.cloudbus.osmosis.core.CloudDatacenter;
import org.cloudbus.osmosis.core.OsmoticBroker;
import org.cloudbus.osmosis.core.SDNController;
import uk.ncl.giacomobergami.components.allocation_policy.VmAllocationPolicyGeneratorFactory;
import uk.ncl.giacomobergami.components.networking.DataCenterWithController;
import uk.ncl.giacomobergami.components.networking.Host;
import uk.ncl.giacomobergami.components.networking.Switch;
import uk.ncl.giacomobergami.components.networking.VM;
import uk.ncl.giacomobergami.utils.data.YAML;

import java.io.File;
import java.io.Serializable;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.cloudbus.cloudsim.edge.utils.LogUtil.logger;

public class SubNetworkConfiguration implements Serializable {
    public List<Host> hosts;
    public List<VM> vms_or_mels;
    public List<Switch> switches;
    public DataCenterWithController conf;
    public final String name;

    public SubNetworkConfiguration(List<Host> hosts,
                                   List<VM> vms_or_mels,
                                   List<Switch> switches,
                                   DataCenterWithController conf,
                                   String name) {
        this.hosts = hosts;
        this.vms_or_mels = vms_or_mels;
        this.switches = switches;
        this.conf = conf;
        this.name = name;
    }

    public SubNetworkConfiguration(File folder) {
        this.name = folder.getName();
        hosts = new ArrayList<>();
        vms_or_mels = new ArrayList<>();
        switches = new ArrayList<>();
        Host.csvReader().readAll(new File(folder, "hosts.csv").getAbsoluteFile(), hosts);
        VM.csvReader().readAll(new File(folder, "vms.csv").getAbsoluteFile(), vms_or_mels);
        Switch.csvReader().readAll(new File(folder, "switches.csv").getAbsoluteFile(), switches);
        conf = YAML.parse(DataCenterWithController.class, new File(folder, "conf.yaml").getAbsoluteFile()).orElseThrow();
    }

    public CloudDatacenter createCloudDatacenter(OsmoticBroker broker,
                                                 AtomicInteger hostId,
                                                 AtomicInteger vmId,
                                                 Map<String, Collection<LegacyConfiguration.LinkEntity>> linkMap) {
        if (conf.scheduling_interval != 0.0)
            throw new RuntimeException("0.0 expected scheduling interval: "+conf.scheduling_interval);
        SDNController sdnController = conf.asCloudController();
        List<org.cloudbus.cloudsim.Host> hostList = sdnController.getHostList();
        LinkedList<Storage> storageList = new LinkedList<>();
        try {
            var loc_datacentre = new CloudDatacenter(conf.datacenter_name,
                    conf.asDatacenterCharacteristics(hostList),
                    VmAllocationPolicyGeneratorFactory.generateFacade(conf.datacenter_vmAllocationPolicy),
                    storageList,
                    conf.scheduling_interval,
                    sdnController);

            var s_host = hosts
                    .stream()
                    .map(Host::asLegacyHostEntity);
            var s_switch = switches
                    .stream()
                    .map(x -> x.asLegacySwitchEntity(conf.controller_name));
            var s_links = linkMap.get(conf.datacenter_name);

            loc_datacentre.initCloudTopology(s_host, s_switch, s_links, hostId);
            loc_datacentre.feedSDNWithTopology();
            loc_datacentre.setGateway(loc_datacentre.getSdnController().getGateway());
            loc_datacentre.setDcType(conf.datacenter_type);

            List<Vm> vmList = vms_or_mels
                    .stream()
                    .map(x -> {
                        var vm = new Vm(x.asLegacyVMEntity(), broker, vmId);
                        loc_datacentre.mapVmNameToID(vm.getId(), vm.getVmName());
                        return vm;
                    })
                    .collect(Collectors.toList());
            broker.mapVmNameToId(loc_datacentre.getVmNameToIdList());
            loc_datacentre.setVmList(vmList);
            loc_datacentre.setDCAndAddVMsToSDNHosts();
            loc_datacentre.getVmAllocationPolicy().setUpVmTopology(loc_datacentre.getHosts());
            loc_datacentre.setNetworkInformation(this);
            return loc_datacentre;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public EdgeDataCenter createEdgeDatacenter(OsmoticBroker broker,
                                                AtomicInteger hostId,
                                               AtomicInteger vmId,
                                               Map<String, Collection<LegacyConfiguration.LinkEntity>> linkMap) {
        var hostList = hosts
                .stream()
                .map(x-> new EdgeDevice(hostId, x.asLegacyEdgeDeviceEntity()))
                .collect(Collectors.toList());
        var s_switch = switches
                .stream()
                .map(x -> x.asLegacySwitchEntity(conf.controller_name));
        var s_links = linkMap.get(conf.datacenter_name);
        var characteristics = conf.asDatacenterCharacteristics(hostList);
        LinkedList<Storage> storageList = new LinkedList<>();

        // 6. Finally, we need to create a PowerDatacenter object.
        EdgeDataCenter datacenter = new EdgeDataCenter(conf.datacenter_name,
                characteristics,
                VmAllocationPolicyGeneratorFactory.generateFacade(conf.datacenter_vmAllocationPolicy),
                storageList,
                conf.scheduling_interval);
        datacenter.setSdnController(conf.asEdgeSDNController(datacenter));
        datacenter.initEdgeTopology(hostList, s_switch, s_links);

        logger.info("Edge SDN cotroller has been created: "+conf.datacenter_name);

        var MELList = vms_or_mels
                .stream()
                .map(x -> {
                    var mel = new MEL(datacenter.getId(),
                            vmId, x.asLegacyMELEntity(), broker);
                    datacenter.mapVmNameToID(mel.getId(), mel.getVmName());
                    return mel;
                })
                .collect(Collectors.toList());
        datacenter.setVmList(MELList);

        broker.mapVmNameToId(datacenter.getVmNameToIdList());
        datacenter.getVmAllocationPolicy().setUpVmTopology(hostList);
        datacenter.getSdnController().addVmsToSDNhosts(MELList);
        datacenter.setNetworkInformation(this);
        return datacenter;
    }



    public void serializeToFolder(File folder) {
        Host.csvReader().writeAll(new File(folder, "hosts.csv").getAbsoluteFile(), hosts);
        VM.csvReader().writeAll(new File(folder, "vms.csv").getAbsoluteFile(), vms_or_mels);
        Switch.csvReader().writeAll(new File(folder, "switches.csv").getAbsoluteFile(), switches);
        YAML.serialize(conf, new File(folder, "conf.yaml").getAbsoluteFile());
    }
}
