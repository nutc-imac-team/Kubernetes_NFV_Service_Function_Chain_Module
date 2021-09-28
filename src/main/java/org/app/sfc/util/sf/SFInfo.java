package org.app.sfc.util.sf;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.meter.MeterCellId;
import org.onosproject.net.meter.MeterId;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class SFInfo implements SFFeatures {
    private static int counter = 0;
    private int sfId;
    private String domain;
    private VlanId chainId;
    private long rate;
    private MeterId meterId;
    private ArrayList<IpAddress> ipAddress;
    private Map<IpAddress, MacAddress> macAddress;
    private DeviceId deviceId;
    private PortNumber portNumber;

    public SFInfo(String domain, VlanId chainId, long rate, MeterId meterId,
                  ArrayList<IpAddress> ipAddress, Map<IpAddress, MacAddress> macAddress) {
        this.sfId = counter++;
        this.domain = domain;
        this.chainId = chainId;
        this.rate = rate;
        this.meterId = meterId;
        this.ipAddress = ipAddress;
        this.macAddress = macAddress;
    }

    @Override
    public int sfId() {
        return this.sfId;
    }

    @Override
    public String domain() {
        return this.domain;
    }

    @Override
    public VlanId chainId() {
        return this.chainId;
    }

    @Override
    public long rate() {
        return this.rate;
    }

    @Override
    public MeterId meterId() {
        return this.meterId;
    }

    @Override
    public ArrayList<IpAddress> ipAddress() {
        return this.ipAddress;
    }

    @Override
    public Map<IpAddress, MacAddress> macAddress() {
        return this.macAddress;
    }

    @Override
    public DeviceId deviceId() {
        return this.deviceId;
    }

    @Override
    public PortNumber portNumber() {
        return this.portNumber;
    }

    @Override
    public void setRate(long rate) {
        this.rate = rate;
    }

    @Override
    public void setMeterId(MeterId meterId) {
        this.meterId = meterId;
    }

    @Override
    public void setChainId(VlanId chainId) {
        this.chainId = chainId;
    }

    @Override
    public void setDeviceId(DeviceId deviceId) {
        this.deviceId = deviceId;
    }

    @Override
    public void setPortNumber(PortNumber portNumber) {
        this.portNumber = portNumber;
    }

    public static SFInfo.Builder builder() {
        return new SFInfo.Builder();
    }

    public static final class Builder implements SFFeatures.Builder {
        private String domain;
        private VlanId chainId;
        private long rate;
        private MeterId meterId;
        private ArrayList<IpAddress> ipAddress = new ArrayList<>();
        private Map<IpAddress, MacAddress> macAddress = Maps.newConcurrentMap();

        public Builder() {

        }

        @Override
        public SFFeatures.Builder withDomain(String domain) {
            this.domain = domain;
            return this;
        }

        @Override
        public SFFeatures.Builder withRate(long rate) {
            this.rate = rate;
            return this;
        }

        @Override
        public SFFeatures.Builder withMeterId(MeterId meterId) {
            this.meterId = meterId;
            return this;
        }

        @Override
        public SFFeatures.Builder withChain(VlanId chainId) {
            this.chainId = chainId;
            return this;
        }

        @Override
        public SFFeatures.Builder withIpAddress(ArrayList<IpAddress> ipAddress) {
            this.ipAddress = ipAddress;
            return this;
        }

        @Override
        public SFFeatures.Builder withMacAddress(Map<IpAddress, MacAddress> macAddress) {
            this.macAddress = macAddress;
            return this;
        }


        @Override
        public SFFeatures build() {
            Preconditions.checkNotNull(this.domain, "Must specify a domain");
            return new SFInfo(this.domain, this.chainId, this.rate, this.meterId, this.ipAddress, this.macAddress);
        }

    }
}
