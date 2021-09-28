package org.app.sfc.util.sf;

import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.meter.MeterId;

import java.util.ArrayList;
import java.util.Map;

public interface SFFeatures {
    int sfId();

    String domain();

    VlanId chainId();

    long rate();

    MeterId meterId();

    ArrayList<IpAddress> ipAddress();

    Map<IpAddress, MacAddress> macAddress();

    DeviceId deviceId();

    PortNumber portNumber();

    void setChainId(VlanId chainId);

    void setRate(long rate);

    void setMeterId(MeterId meterId);

    void setDeviceId(DeviceId deviceId);

    void setPortNumber(PortNumber portNumber);

    public interface Builder {
        SFFeatures.Builder withDomain(String domain);

        SFFeatures.Builder withRate(long rate);

        SFFeatures.Builder withMeterId(MeterId meterId);

        SFFeatures.Builder withChain(VlanId chainId);

        SFFeatures.Builder withIpAddress(ArrayList<IpAddress> ipAddress);

        SFFeatures.Builder withMacAddress(Map<IpAddress, MacAddress> macAddress);

        SFFeatures build();

    }
}
