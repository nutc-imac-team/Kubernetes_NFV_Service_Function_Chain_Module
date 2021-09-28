package org.app.sfc.util.sfc;

import org.onlab.packet.VlanId;

import java.util.ArrayList;

public interface SFCFeatures {
    VlanId sfcId();

    ClassifierFeatures classifierFeatures();

    ArrayList<String> rsp();

    public interface Builder {
        SFCFeatures.Builder withClassifierFeatures(ClassifierFeatures classifierFeatures);

        SFCFeatures.Builder withRsp(ArrayList<String> rsp);

        SFCFeatures build();
    }
}
