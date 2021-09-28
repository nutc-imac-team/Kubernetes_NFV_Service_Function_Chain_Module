/*
 * Copyright 2019-present Open Networking Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.app.sfc;

import com.google.common.collect.Maps;
import org.app.sfc.util.sf.SFFeatures;
import org.app.sfc.util.sf.SFKey;
import org.app.sfc.util.sfc.SFCFeatures;
import org.app.sfc.util.sfc.SFCKey;
import org.onlab.packet.Ethernet;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.net.group.GroupDescription;
import org.onosproject.net.group.GroupService;
import org.onosproject.net.host.HostService;
import org.onosproject.net.meter.MeterService;
import org.onosproject.net.meter.MeterStore;
import org.onosproject.net.packet.PacketPriority;
import org.onosproject.net.packet.PacketProcessor;
import org.onosproject.net.packet.PacketService;
import org.onosproject.net.topology.TopologyService;
import org.osgi.service.component.annotations.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Skeletal ONOS application component.
 */
@Component(immediate = true)
public class AppComponent {

    private final Logger log = LoggerFactory.getLogger(getClass());
    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected DeviceService deviceService;
    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected HostService hostService;
    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected FlowRuleService flowRuleService;
    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected PacketService packetService;
    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected CoreService coreService;
    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected TopologyService topologyService;
    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected GroupService groupService;
    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected MeterService meterService;
    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected MeterStore meterStore;

    private static Map<SFCKey, SFCFeatures> sfcInfo = Maps.newConcurrentMap();
    private static Map<SFKey, SFFeatures> sfInfo = Maps.newConcurrentMap();
    private static ArrayList<GroupDescription> descriptionArrayList = new ArrayList<>();
    private static SFForwarder SFForwarder;
    public static ApplicationId appId;

    @Activate
    protected void activate() {
        log.info("Started");
        // app id
        appId = coreService.getAppId("org.foo.app");
        SFForwarder = new SFForwarder(flowRuleService, appId, hostService,
                deviceService, topologyService, groupService, meterService, meterStore);
        // add packet processor
        packetService.addProcessor(SFForwarder, PacketProcessor.director(3));
        packetService.requestPackets(DefaultTrafficSelector.builder()
                .matchEthType(Ethernet.TYPE_IPV4).build(), PacketPriority.REACTIVE, appId, Optional.empty());
    }

    @Deactivate
    protected void deactivate() {
        log.info("Stopped");
        for (GroupDescription description : descriptionArrayList) {
            log.info("remove {} ", description);
            groupService.removeGroup(description.deviceId(), description.appCookie(), appId);
        }
        flowRuleService.removeFlowRulesById(AppComponent.appId);
        packetService.removeProcessor(SFForwarder);
    }

    public static Map<SFKey, SFFeatures> getSfInfo() {
        return sfInfo;
    }

    public static Map<SFCKey, SFCFeatures> getSfc() {
        return sfcInfo;
    }

    public static ArrayList<GroupDescription> getDescriptionArrayList() {
        return descriptionArrayList;
    }
}