package org.app.sfc;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.app.sfc.util.sfc.SFCFeatures;
import org.app.sfc.util.sf.SFFeatures;
import org.app.sfc.util.sf.SFKey;
import org.app.sfc.util.sfc.SFCKey;
import org.onlab.packet.*;
import org.onlab.util.KryoNamespace;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.GroupId;
import org.onosproject.net.*;
import org.onosproject.net.behaviour.ExtensionTreatmentResolver;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.flow.*;
import org.onosproject.net.flow.instructions.ExtensionPropertyException;
import org.onosproject.net.flow.instructions.ExtensionTreatment;
import org.onosproject.net.flow.instructions.ExtensionTreatmentType;
import org.onosproject.net.group.*;
import org.onosproject.net.host.HostService;
import org.onosproject.net.meter.*;
import org.onosproject.net.packet.*;
import java.util.*;
import org.onosproject.net.topology.TopologyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.app.sfc.AppComponent.getSfc;

public class SFForwarder implements PacketProcessor {
    private FlowRuleService flowRuleService;
    private HostService hostService;
    private DeviceService deviceService;
    private TopologyService topologyService;
    private GroupService groupService;
    private ApplicationId appId;
    private MeterService meterService;
    private MeterStore meterStore;
    private Map<DeviceId, Map<MacAddress, PortNumber>> macTables = Maps.newConcurrentMap();
    private int Temporary = 5;
    private final Logger log = LoggerFactory.getLogger(getClass());
    private static KryoNamespace appKryo = new KryoNamespace.Builder()
            .register(Integer.class)
            .register(DeviceId.class)
            .build("group-fwd-app");


    public SFForwarder(FlowRuleService flowRuleService, ApplicationId appId, HostService hostService,
                       DeviceService deviceService, TopologyService topologyService,
                       GroupService groupService, MeterService meterService, MeterStore meterStore) {
        this.flowRuleService = flowRuleService;
        this.hostService = hostService;
        this.deviceService = deviceService;
        this.topologyService = topologyService;
        this.groupService = groupService;
        this.meterService = meterService;
        this.meterStore = meterStore;
        this.appId = appId;
    }

    @Override
    public void process(PacketContext packetContext) {
        initMacTable(packetContext.inPacket().receivedFrom());
        addExistHost();
        registerMeter(packetContext);
        actLikeSwitch(packetContext);
        processSFC(packetContext);
    }

    private void addExistHost() {
        for (Host host : hostService.getHosts()) {
            MacAddress macAddress = host.mac();
            String[] host_location = host.location().toString().split("/");
            if (host.ipAddresses().toArray().length < 1) {
                continue;
            }

            IpAddress ipv4Address = (IpAddress) (host.ipAddresses().toArray()[0]);
            String deviceId = host_location[0];
            PortNumber port = PortNumber.fromString(host_location[1]);
            macTables.putIfAbsent(DeviceId.deviceId(deviceId), Maps.newConcurrentMap());
            Map<MacAddress, PortNumber> macTable = macTables.get(DeviceId.deviceId(deviceId));
            macTable.put(macAddress, port);

            for (Map.Entry<SFKey, SFFeatures> entry : AppComponent.getSfInfo().entrySet()) {
                for (IpAddress ip : entry.getValue().ipAddress()) {
                    if (ip.equals(ipv4Address)) {
                        entry.getValue().macAddress().put(ipv4Address, macAddress);
                        entry.getValue().setDeviceId(DeviceId.deviceId(deviceId));
                        entry.getValue().setPortNumber(port);
                    }
                }
            }
        }
    }

    private void registerMeter(PacketContext packetContext) {
        DeviceId deviceId = packetContext.inPacket().receivedFrom().deviceId();
        long maxMeters = meterStore.getMaxMeters(MeterFeaturesKey.key(deviceId));
        if (0L == maxMeters) {
            meterStore.storeMeterFeatures(DefaultMeterFeatures.builder()
                    .forDevice(packetContext.inPacket().receivedFrom().deviceId())
                    .withMaxMeters(1000L)
                    .build());
        }
    }

    private void processSFC(PacketContext packetContext) {
        if (packetContext.inPacket().parsed().getEtherType() != Ethernet.TYPE_IPV4) return;
        Ethernet ethernet = packetContext.inPacket().parsed();

        IPv4 iPv4 = (IPv4) ethernet.getPayload();

        if (iPv4.getProtocol() == IPv4.PROTOCOL_UDP) {
            UDP udp = (UDP) iPv4.getPayload();
            if (5053 == udp.getDestinationPort()) return;
        }
        for (Map.Entry<SFCKey, SFCFeatures> entry : getSfc().entrySet()) {
            boolean hasSrcChain = false;
            boolean hasDstChain = false;
            SFFeatures srcSfFeatures = AppComponent.getSfInfo().get(SFKey.key(entry.getValue().classifierFeatures().sourceDomain()));
            SFFeatures dstSfFeatures = AppComponent.getSfInfo().get(SFKey.key(entry.getValue().classifierFeatures().destinationDomain()));
            for (IpAddress ipAddress : srcSfFeatures.ipAddress()) {
                if (IpAddress.valueOf(iPv4.getSourceAddress()).equals(ipAddress)) {
                    srcSfFeatures.macAddress().put(ipAddress, ethernet.getSourceMAC());
                    hasSrcChain = true;
                    break;
                }
            }

            for (IpAddress ipAddress : dstSfFeatures.ipAddress()) {
                if (IpAddress.valueOf(iPv4.getDestinationAddress()).equals(ipAddress)) {
                    dstSfFeatures.macAddress().put(ipAddress, ethernet.getDestinationMAC());
                    hasDstChain = true;
                    break;
                }
            }
            if (!hasSrcChain || !hasDstChain) {
                break;
            }
            ArrayList<SFFeatures> usefulSfInfo = new ArrayList<>();
            boolean isExistSfInfo = true;
            for (String domain : entry.getValue().rsp()) {
                SFFeatures sfFeatures = AppComponent.getSfInfo().get(SFKey.key(domain));
                if (null == sfFeatures) {
                    isExistSfInfo = false;
                    break;
                } else {
                    for (Map.Entry<IpAddress, MacAddress> macAddressEntry : sfFeatures.macAddress().entrySet()) {
                        if (null == macAddressEntry.getValue()) {
                            isExistSfInfo = false;
                            break;
                        }
                    }
                }
                usefulSfInfo.add(sfFeatures);
            }
            if (!isExistSfInfo) {
                usefulSfInfo.clear();
                break;
            }
            addTag(flowRuleService, packetContext, usefulSfInfo, false);
            ArrayList<SFFeatures> reverseSfInfo = new ArrayList<>(usefulSfInfo);
            Collections.reverse(reverseSfInfo);
            addTag(flowRuleService, packetContext, reverseSfInfo, true);
        }
    }

    private MeterId processMeterTable(DeviceId deviceId, long rate) {
        MeterId id = checkExistMeter(rate);
        if (null == id) {
            Set<Band> bands = new HashSet<>();
            bands.add(DefaultBand.builder()
                    .ofType(Band.Type.DROP)
                    .withRate(rate)
                    .burstSize(0)
                    .build());

            MeterRequest meterRequest = DefaultMeterRequest.builder()
                    .forDevice(deviceId)
                    .fromApp(appId)
                    .withUnit(Meter.Unit.KB_PER_SEC)
                    .withBands(bands)
                    .add();

            return meterService.submit(meterRequest).id();
        } else {
            return id;
        }
    }

    private MeterId checkExistMeter(long rate) {
        boolean hasMeter = false;
        for (Meter meter : meterService.getAllMeters()) {
            for (Band band : meter.bands()) {
                if (rate == band.rate()) {
                    hasMeter = true;
                    break;
                }
            }
            if (hasMeter) {
                return meter.id();
            }
        }
        return null;
    }

    private void addTag(FlowRuleService flowRuleService, PacketContext packetContext, ArrayList<SFFeatures> sfFeatures, boolean isReverse) {
        new Thread(() -> {
            Ethernet ethernet = packetContext.inPacket().parsed();
            IPv4 iPv4 = (IPv4) ethernet.getPayload();

            for (int position = 0; position < sfFeatures.size() - 1; position++) {
                SFFeatures currentSfFeatures = sfFeatures.get(position);
                SFFeatures nextSfFeatures = sfFeatures.get(position + 1);

                if (currentSfFeatures.macAddress().size() < 1 || nextSfFeatures.macAddress().size() < 1) {
                    break;
                }

                MplsLabel mplsLabel;
                MacAddress ethDst;
                IpPrefix iPDst;
                if (isReverse) {
                    mplsLabel = MplsLabel.mplsLabel(1048575 - currentSfFeatures.sfId());
                    ethDst = ethernet.getSourceMAC();
                    IpAddress dstIpAddress = IpAddress.valueOf(iPv4.getSourceAddress());
                    iPDst = dstIpAddress.toIpPrefix();
                } else {
                    mplsLabel = MplsLabel.mplsLabel(currentSfFeatures.sfId());
                    ethDst = ethernet.getDestinationMAC();
                    IpAddress dstIpAddress = IpAddress.valueOf(iPv4.getDestinationAddress());
                    iPDst = dstIpAddress.toIpPrefix();
                }

                for (int ipPosition = 0; ipPosition < currentSfFeatures.ipAddress().size(); ipPosition++) {
                    IpAddress ipAddress = currentSfFeatures.ipAddress().get(ipPosition);

                    TrafficTreatment trafficTreatment = DefaultTrafficTreatment.builder()
                            .pushVlan()
                            .pushMpls()
                            .setVlanId(currentSfFeatures.chainId())
                            .setMpls(mplsLabel)
                            .transition(1)
                            .build();

//                    if (position == 0 && null != currentSfFeatures.meterId()) {
//                        trafficTreatment = DefaultTrafficTreatment.builder()
//                                .pushVlan()
//                                .pushMpls()
//                                .setVlanId(currentSfFeatures.chainId())
//                                .setMpls(mplsLabel)
//                                .transition(1)
//                                .meter(currentSfFeatures.meterId())
//                                .build();
//                    }

                    FlowRule flowRule = DefaultFlowRule.builder()
                            .withSelector(DefaultTrafficSelector.builder()
                                    .matchEthType(Ethernet.TYPE_IPV4)
                                    .matchEthDst(ethDst)
                                    .matchEthSrc(currentSfFeatures.macAddress().get(ipAddress))
                                    .matchIPDst(iPDst)
                                    .build())
                            .withTreatment(trafficTreatment)
                            .forDevice(packetContext.inPacket().receivedFrom().deviceId())
                            .fromApp(AppComponent.appId)
                            .makeTemporary(Temporary)
                            .withPriority(50)
                            .forTable(0)
                            .build();
                    log.info("-----addTagaddTagaddTag------" + flowRule.toString());
                    flowRuleService.applyFlowRules(flowRule);
                }
                processSfcFlowRule(packetContext, currentSfFeatures, nextSfFeatures, mplsLabel, isReverse);
            }
        }).start();
    }

    /**
     * 如果輸入封包符合就根據SFC轉發
     * 否則以一般轉發方式
     * <p>
     * 產生SFC假資料
     * 格式{sf1,sf2,sf3}
     **/
    /**
     * 處理SFC邏輯
     **/

    private void processSfcFlowRule(PacketContext context, SFFeatures currentSfFeatures, SFFeatures nextSfFeatures,
                                    MplsLabel mplsLabel, boolean isReverse) {
        DeviceId currentDeviceId = context.inPacket().receivedFrom().deviceId();
        //取得DNS對應domain、chainId、sfInfo
        int groupId = generateGroupId(currentSfFeatures, isReverse);

        processDeviceGroups(groupId, nextSfFeatures);

        FlowRule flowRule = DefaultFlowRule.builder()
                .withSelector(DefaultTrafficSelector.builder()
                        .matchMplsLabel(mplsLabel)
                        .matchVlanId(currentSfFeatures.chainId())
                        .matchEthType(Ethernet.MPLS_UNICAST)
                        .build())
                .withTreatment(DefaultTrafficTreatment.builder()
                        .group(GroupId.valueOf(groupId))
                        .build())
                .forDevice(currentDeviceId)
                .fromApp(appId)
                .makeTemporary(Temporary)
                .withPriority(500)
                .forTable(1)
                .build();
        flowRuleService.applyFlowRules(flowRule);
        log.info("-----applyFlowRulesapplyFlowRules------" + flowRule.toString());

        //取得全部nextDomain
        for (IpAddress ipAddress : nextSfFeatures.ipAddress()) {
            MacAddress macAddress = nextSfFeatures.macAddress().get(ipAddress);
            Map<MacAddress, PortNumber> macTable = macTables.get(currentDeviceId);
            PortNumber outPort = macTable.get(macAddress);

//            log.info("mac {}\ntargetDeviceId  {} \ntargetPortnumber {} \ncurrentDeviceid {}\npaths {}\nhostDst.location().port() {}\n",
//                    macAddress,
//                    AppComponent.getIpAddressHostInfoMap().get(pickIpaddress(hostDst.ipAddresses(), macAddress)).getDeviceId(),
//                    AppComponent.getIpAddressHostInfoMap().get(pickIpaddress(hostDst.ipAddresses(), macAddress)).getPortNumber(),
//                    currentDeviceId,
//                    paths,
//                    hostDst.location().port());

            TrafficTreatment trafficTreatment = DefaultTrafficTreatment.builder()
                    .popMpls()
                    .popVlan()
                    .setOutput(outPort)
                    .build();

            FlowRule outputFlowRule = DefaultFlowRule.builder()
                    .forTable(2)
                    .makeTemporary(Temporary)
                    .withPriority(500)
                    .fromApp(appId)
                    .forDevice(currentDeviceId)
                    .withSelector(DefaultTrafficSelector.builder()
                            .matchMplsLabel(mplsLabel)
                            .matchVlanId(currentSfFeatures.chainId())
                            .matchEthType(Ethernet.MPLS_UNICAST)
                            .matchEthDst(macAddress)
                            .build())
                    .withTreatment(trafficTreatment)
                    .build();
            flowRuleService.applyFlowRules(outputFlowRule);
        }
    }

    /**
     * 處理 Group Table
     * 傳入 domain 以及device ID
     **/

    private void processDeviceGroups(int finalGroupId, SFFeatures sfFeatures) {
        Set<Device> devices = Sets.newHashSet(deviceService.getAvailableDevices());
        devices.forEach(targetDevice -> {
            ArrayList<GroupBucket> deviceBucket = createBucketForDevice(sfFeatures, targetDevice);
            GroupKey targetDeviceGroupKey = generateGroupKey(targetDevice.id(), finalGroupId);
            if (!groupExist(targetDevice, targetDeviceGroupKey)) {
                // 建立 GroupDescription 用來建立Group Table Action Bucket
                GroupDescription groupDescription = new DefaultGroupDescription(
                        targetDevice.id(),
                        GroupDescription.Type.SELECT,
                        new GroupBuckets(deviceBucket),
                        targetDeviceGroupKey,
                        finalGroupId,
                        appId);
                AppComponent.getDescriptionArrayList().add(groupDescription);
                // 建立Group Table Action Bucket
                groupService.addGroup(groupDescription);
            }
        });
    }

    private int generateGroupId(SFFeatures sfFeatures, boolean isReverse) {
        return (String.valueOf(sfFeatures.sfId()) + sfFeatures.chainId() + isReverse).hashCode();
    }

    // 建立Action Buckets mod eht_dst,send table 2
    private ArrayList<GroupBucket> createBucketForDevice(SFFeatures sfFeatures, Device device) {
        ArrayList<GroupBucket> bucketArrayList = new ArrayList<>();
        for (IpAddress ipAddress : sfFeatures.ipAddress()) {
            ExtensionTreatmentResolver resolver = device.as(ExtensionTreatmentResolver.class);
            TrafficTreatment.Builder treatmentBuilder = DefaultTrafficTreatment.builder();
            ExtensionTreatment extension = resolver.getExtensionInstruction(ExtensionTreatmentType.ExtensionTreatmentTypes.NICIRA_RESUBMIT_TABLE.type());
            try {
                extension.setPropertyValue("table", (short) 2);
                treatmentBuilder
                        .setEthDst(sfFeatures.macAddress().get(ipAddress))
                        .extension(extension, device.id());
                bucketArrayList.add(DefaultGroupBucket.createSelectGroupBucket(treatmentBuilder.build()));

            } catch (ExtensionPropertyException e) {
                e.printStackTrace();
            }
        }
        return bucketArrayList;
    }

    private GroupKey generateGroupKey(DeviceId deviceId, Integer groupId) {
        int hashed = Objects.hash(deviceId, groupId);
        return new DefaultGroupKey(appKryo.serialize(hashed));
    }

    private boolean groupExist(Device device, GroupKey groupKey) {
        return groupService.getGroup(device.id(), groupKey) != null;
    }

    private void actLikeSwitch(PacketContext packetContext) {
        Ethernet ethernet = packetContext.inPacket().parsed();
        short type = ethernet.getEtherType();
        if (type != Ethernet.TYPE_IPV4 && type != Ethernet.TYPE_ARP) {
            return;
        }

        ConnectPoint connectPoint = packetContext.inPacket().receivedFrom();
        Map<MacAddress, PortNumber> macTable = macTables.get(connectPoint.deviceId());
        MacAddress srcMac = ethernet.getSourceMAC();
        MacAddress dstMac = ethernet.getDestinationMAC();
        macTable.put(srcMac, connectPoint.port());
        PortNumber outPort = macTable.get(dstMac);

        if (outPort != null) {
            if (type == Ethernet.TYPE_IPV4) {
                IpAddress srcIpv4Address = IpAddress.valueOf(((IPv4) ethernet.getPayload()).getSourceAddress());
                TrafficSelector trafficSelector = DefaultTrafficSelector.builder()
                        .matchEthSrc(srcMac)
                        .matchEthDst(dstMac)
                        .build();

                MeterId meterId = checkMeter(srcIpv4Address);
                if (null == meterId) {
                    createFlowRule(trafficSelector,
                            createTrafficTreatment(null, outPort),
                            connectPoint.deviceId());
                } else {
                    createFlowRule(trafficSelector,
                            createTrafficTreatment(meterId, outPort),
                            connectPoint.deviceId());
                }
            } else {
                packetContext.treatmentBuilder().setOutput(outPort);
                packetContext.send();
            }
        } else {
            actLikeHub(packetContext);
        }
    }

    private MeterId checkMeter(IpAddress ipv4Address) {
        for (Map.Entry<SFKey, SFFeatures> entry : AppComponent.getSfInfo().entrySet()) {
            for (IpAddress ip : entry.getValue().ipAddress()) {
                if (ip.equals(ipv4Address) && 0L != entry.getValue().rate()) {
                    MeterId meterId = processMeterTable(entry.getValue().deviceId(), entry.getValue().rate());
                    entry.getValue().setMeterId(meterId);
                    return meterId;
                }
            }
        }
        return null;
    }

    private TrafficTreatment createTrafficTreatment(MeterId meterId, PortNumber outPort) {
        TrafficTreatment.Builder trafficTreatment = DefaultTrafficTreatment.builder();
        if (null != meterId) trafficTreatment.meter(meterId);
        if (null != outPort) trafficTreatment.setOutput(outPort);
        return trafficTreatment.build();
    }

    private void createFlowRule(TrafficSelector trafficSelector, TrafficTreatment trafficTreatment, DeviceId deviceId) {
        FlowRule flowRule = DefaultFlowRule.builder()
                .fromApp(appId)
                .withSelector(trafficSelector)
                .withTreatment(trafficTreatment)
                .forDevice(deviceId)
                .withPriority(10)
                .makeTemporary(Temporary)
                .build();
        flowRuleService.applyFlowRules(flowRule);
    }

    private void actLikeHub(PacketContext packetContext) {
        packetContext.treatmentBuilder().setOutput(PortNumber.FLOOD);
        packetContext.send();
    }

    private void initMacTable(ConnectPoint connectPoint) {
        macTables.putIfAbsent(connectPoint.deviceId(), Maps.newConcurrentMap());
    }
}
