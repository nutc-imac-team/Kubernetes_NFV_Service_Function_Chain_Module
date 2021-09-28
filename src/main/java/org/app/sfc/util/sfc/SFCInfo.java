package org.app.sfc.util.sfc;

import com.google.common.base.Preconditions;
import org.onlab.packet.VlanId;

import java.util.ArrayList;

public class SFCInfo implements SFCFeatures {
    private final VlanId id;
    private final ClassifierFeatures classifierFeatures;
    private final ArrayList<String> rsp;

    public SFCInfo(ClassifierFeatures classifierFeatures, ArrayList<String> rsp) {
        id = VlanId.vlanId((short) (Math.random() * 4094 + 1));
        this.classifierFeatures = classifierFeatures;
        this.rsp = rsp;
    }

    @Override
    public VlanId sfcId() {
        return this.id;
    }

    @Override
    public ClassifierFeatures classifierFeatures() {
        return this.classifierFeatures;
    }

    @Override
    public ArrayList<String> rsp() {
        return this.rsp;
    }

    public static SFCInfo.Builder builder() {
        return new SFCInfo.Builder();
    }

    public static final class Builder implements SFCFeatures.Builder {
        private ClassifierFeatures classifierFeatures;
        private ArrayList<String> rsp;

        public Builder() {

        }

        @Override
        public SFCFeatures.Builder withClassifierFeatures(ClassifierFeatures classifierFeatures) {
            this.classifierFeatures = classifierFeatures;
            return this;
        }

        @Override
        public SFCFeatures.Builder withRsp(ArrayList<String> rsp) {
            this.rsp = rsp;
            return this;
        }

        @Override
        public SFCFeatures build() {
            Preconditions.checkNotNull(this.rsp, "Must specify a rsp");
            Preconditions.checkNotNull(this.classifierFeatures, "Must specify a classifierFeatures");
            return new SFCInfo(this.classifierFeatures, this.rsp);
        }
    }
}
