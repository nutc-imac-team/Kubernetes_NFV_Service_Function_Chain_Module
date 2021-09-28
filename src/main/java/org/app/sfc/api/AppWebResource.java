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
package org.app.sfc.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.lang.StringUtils;
import org.app.sfc.AppComponent;
import org.app.sfc.util.sfc.*;
import org.app.sfc.util.sf.SFFeatures;
import org.app.sfc.util.sf.SFInfo;
import org.app.sfc.util.sf.SFKey;
import org.onlab.packet.IpAddress;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.packet.PacketService;
import org.onosproject.rest.AbstractWebResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

import static org.app.sfc.AppComponent.*;
import static org.onlab.util.Tools.readTreeFromStream;

/**
 * Sample web resource.
 */
@Path("sfc")
public class AppWebResource extends AbstractWebResource {
    private final Logger log = LoggerFactory.getLogger(getClass());

    /**
     * Get hello world greeting.
     *
     * @return 200 OK
     */
//    @GET
//    @Path("")
//    public Response getGreeting() {
//        ObjectNode node = mapper().createObjectNode().put("hello", "world");
//        return ok(node).build();
//    }
    @GET
    @Path("list")
    public Response listSF() {
        ObjectNode root = mapper().createObjectNode();
        ArrayNode arrayNode = root.putArray("sfInfo");
        for (Map.Entry<SFKey, SFFeatures> entry : getSfInfo().entrySet()) {
            ObjectNode sf = mapper().createObjectNode();
            sf.put("id", entry.getValue().domain());
            sf.put("rate", entry.getValue().rate());
            arrayNode.add(sf);
        }
        return ok(root).build();
    }

    @POST
    @Path("read")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response readSFC(InputStream stream) {
        ObjectNode root = mapper().createObjectNode();
        try {
            ObjectNode jsonTree = readTreeFromStream(mapper(), stream);
            JsonNode inputClassifier = jsonTree.get("classifier");
            String source = inputClassifier.get("source").asText();
            String destination = inputClassifier.get("destination").asText();

            SFCFeatures sfcFeatures = getSfc().get(SFCKey.key(source, destination));
            if (sfcFeatures != null) {
                ArrayNode arrayNode = root.putArray("rsp");
                sfcFeatures.rsp().forEach(arrayNode::add);
            } else {
                root.put("Status", "not found sfc");
                return ok(root).status(404).build();
            }
        } catch (IOException e) {
            root.put("Status", "Fail");
            root.put("Message", e.getMessage());
        }
        return ok(root).build();
    }

    @POST
    @Path("notification")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response notificationSf(InputStream stream) {
        ObjectNode root = mapper().createObjectNode();
        try {
            ObjectNode jsonTree = readTreeFromStream(mapper(), stream);
            JsonNode sfInfo = jsonTree.get("SfInfo");
            String domain = sfInfo.get("domain").asText();
            String rate = sfInfo.get("rate").asText();
            if (!StringUtils.isNumeric(rate)) {
                root.put("Status", "The rate is not long type");
                return ok(root).status(404).build();
            }

            JsonNode ipAddress = sfInfo.get("ipAddress");
            if (!ipAddress.isArray()) {
                root.put("Status", "The ipAddress is not list type");
                return ok(root).status(404).build();
            }
            if (AppComponent.getSfInfo().containsKey(SFKey.key(domain))) {
                SFFeatures sfFeatures = AppComponent.getSfInfo().get(SFKey.key(domain));
                sfFeatures.ipAddress().clear();
                for (JsonNode ip : ipAddress) {
                    IpAddress ipv4Address = IpAddress.valueOf(ip.asText());
                    sfFeatures.ipAddress().add(ipv4Address);
                    sfFeatures.setRate(Long.parseLong(rate));
                }
            } else {
                ArrayList<IpAddress> ipAddressArrayList = new ArrayList<>();
                for (JsonNode ip : ipAddress) {
                    IpAddress ipv4Address = IpAddress.valueOf(ip.asText());
                    ipAddressArrayList.add(ipv4Address);
                }
                SFFeatures sfFeatures = SFInfo.builder()
                        .withDomain(domain)
                        .withIpAddress(ipAddressArrayList)
                        .withRate(Long.parseLong(rate))
                        .build();
                AppComponent.getSfInfo().put(SFKey.key(domain), sfFeatures);
            }
            root.put("Status", "Successful");
        } catch (IOException e) {
            root.put("Status", "Fail");
            root.put("Message", e.getMessage());
        }
        return ok(root).build();
    }

    @POST
    @Path("delete")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response deleteSFC(InputStream stream) {
        ObjectNode root = mapper().createObjectNode();
        try {
            ObjectNode jsonTree = readTreeFromStream(mapper(), stream);
            JsonNode inputClassifier = jsonTree.get("classifier");
            String source = inputClassifier.get("source").asText();
            String destination = inputClassifier.get("destination").asText();
            if (getSfc().get(SFCKey.key(source, destination)) != null) {
                getSfc().remove(SFCKey.key(source, destination));
                root.put("Status", "Successful");
            } else {
                root.put("Status", "not found sfc");
                return ok(root).status(404).build();
            }
        } catch (IOException e) {
            root.put("Status", "Fail");
            root.put("Message", e.getMessage());
        }
        return ok(root).build();
    }

    @POST
    @Path("register")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response RegisterSFC(InputStream stream) {
        ObjectNode root = mapper().createObjectNode();
        try {
            ObjectNode jsonTree = readTreeFromStream(mapper(), stream);
            JsonNode inputClassifier = jsonTree.get("classifier");
            JsonNode inputRsp = jsonTree.get("rsp");
            if (inputRsp.isArray()) {
                ArrayList<String> rsp = new ArrayList<>();
                for (int position = 0; position < inputRsp.size(); position++) {
                    rsp.add(inputRsp.get(position).asText());
                }
                String source = inputClassifier.get("source").asText();
                String destination = inputClassifier.get("destination").asText();
                if (!isExistSFC(SFCKey.key(source, destination), rsp)) {
                    ClassifierFeatures classifierFeatures = Classifier.builder()
                            .withSourceDomain(source)
                            .withDestinationDomain(destination)
                            .build();
                    SFCFeatures sfcFeatures = SFCInfo.builder()
                            .withClassifierFeatures(classifierFeatures)
                            .withRsp(rsp)
                            .build();
                    getSfc().put(SFCKey.key(source, destination), sfcFeatures);
                    for (String domain : rsp) {
                        getSfInfo().get(SFKey.key(domain)).setChainId(sfcFeatures.sfcId());
                    }
                    root.put("Status", "Successful");
                } else {
                    root.put("Status", "already has same sfc");
                    return ok(root).status(404).build();
                }
            }
        } catch (IOException e) {
            root.put("Status", "Fail");
            root.put("Message", e.getMessage());
        }
        return ok(root).build();
    }

//    @POST
//    @Path("register")
//    @Consumes(MediaType.APPLICATION_JSON)
//    public Response Register(InputStream stream) {
//        ObjectNode root = mapper().createObjectNode();
//        try {
//            ObjectNode jsonTree = readTreeFromStream(mapper(), stream);
//            JsonNode inputClassifier = jsonTree.get("classifier");
//            JsonNode inputRsp = jsonTree.get("rsp");
//            if (inputRsp.isArray()) {
//                ArrayList<String> rsp = new ArrayList<>();
//                for (int position = 0; position < inputRsp.size(); position++) {
//                    rsp.add(inputRsp.get(position).asText());
//                }
//                String source = inputClassifier.get("source").asText();
//                String destination = inputClassifier.get("destination").asText();
//                log.info(source + "      " + destination);
//                if (!isExistSFC(source + ":" + destination, rsp)) {
//                    Classifier classifier = new Classifier(source, destination);
//                    SFCInfo sfcInfo = new SFCInfo(classifier, rsp);
//                    getRegisterSfc().put(source + ":" + destination, sfcInfo);
//                    root.put("Status", "Successful");
//                } else {
//                    root.put("Status", "already has same sfc");
//                    return ok(root).status(404).build();
//                }
//            }
//        } catch (IOException e) {
//            root.put("Status", "Fail");
//            root.put("Message", e.getMessage());
//        }
//        return ok(root).build();
//    }

//    @POST
//    @Path("notification")
//    @Consumes(MediaType.APPLICATION_JSON)
//    public Response Notification(InputStream stream) {
//        ObjectNode root = mapper().createObjectNode();
//        try {
//            ObjectNode jsonTree = readTreeFromStream(mapper(), stream);
//            JsonNode rsp = jsonTree.get("rsp");
//            if (!rsp.isArray()) {
//                root.put("Status", "The rsp is not list type");
//                return ok(root).status(404).build();
//            }
//            SFCInfo sfcInfo = getRegisterSfc().get(
//                    rsp.get(0).get("fqdn").asText() + ":" + rsp.get(rsp.size() - 1).get("fqdn").asText());
//            log.info(rsp.get(0).get("fqdn").asText() + "   notification  " + rsp.get(rsp.size() - 1).get("fqdn").asText());
//            if (null == sfcInfo || !(sfcInfo.getRsp().size() == rsp.size())) {
//                root.put("Status", "The sfc does not exist");
//                return ok(root).status(404).build();
//            }
//            for (int position = 0; position < rsp.size(); position++) {
//                if (!rsp.get(position).get("ip").isArray()) {
//                    root.put("Status", "ip address is not list type");
//                    return ok(root).status(404).build();
//                }
//                setSFInfo(rsp.get(position), sfcInfo, position);
//            }
//            if (!is_fist_notification) {
//                flowRuleService.removeFlowRulesById(AppComponent.appId);
//                for (GroupDescription description : AppComponent.getDescriptionArrayList()) {
//                    groupService.removeBucketsFromGroup(
//                            description.deviceId(),
//                            description.appCookie(),
//                            description.buckets(),
//                            description.appCookie(),
//                            AppComponent.appId
//                    );
//                }
//            }
//            root.put("Status", "successful");
//        } catch (IOException e) {
//            root.put("Status", "Fail");
//            root.put("Message", e.getMessage());
//        }
//        is_fist_notification = false;
//        return ok(root).build();
//    }

//    private void setSFInfo(JsonNode sf, SFCInfo sfcInfo, int position) {
//        SFFeatures sfFeatures = SFInfo.builder()
//                .withSfId(String.valueOf(position + 1))
//                .withChain(sfcInfo.getId().toString())
//                .withDomain(sf.get("fqdn").asText())
//                .build();
//
//        for (final JsonNode sfIpAddress : sf.get("ip")) {
//            IpAddress ipAddress = IpAddress.valueOf(sfIpAddress.asText());
//            sfFeatures.macAddress().put(ipAddress, null);
//            sfFeatures.ipAddress().add(ipAddress);
////            ARPHandler arpHandler = new ARPHandler();
////            for (Device device : deviceService.getAvailableDevices()) {
////                arpHandler.buildArpRequest(
////                        packetService, device.id(), ipAddress);
////            }
//        }
//    }

    private boolean isExistSFC(SFCKey sfcKey, ArrayList<String> rsp) {
        SFCFeatures sfcFeatures = getSfc().get(sfcKey);
        if (sfcFeatures == null) {
            return false;
        }
        ArrayList<String> sfcRsp = sfcFeatures.rsp();
        ArrayList<String> temp_record = new ArrayList<>(sfcRsp);
        temp_record.removeAll(rsp);
        if (!temp_record.isEmpty()) {
            return false;
        }
        // SF順序是否相同
        for (int i = 0; i < sfcRsp.size(); i++) {
            if (!sfcRsp.get(i).equals(rsp.get(i))) {
                return false;
            }
        }
        return true;
    }
}
