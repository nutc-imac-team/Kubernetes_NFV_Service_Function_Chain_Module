package org.app.sfc.util.sfc;

import com.google.common.base.Preconditions;

public class Classifier implements ClassifierFeatures {
    private final String sourceInfo;
    private final String destinationInfo;
    private long rate;

    public Classifier(String sourceInfo, String destinationInfo, long rate) {
        this.sourceInfo = sourceInfo;
        this.destinationInfo = destinationInfo;
    }

    @Override
    public String sourceDomain() {
        return this.sourceInfo;
    }

    @Override
    public String destinationDomain() {
        return this.destinationInfo;
    }

    @Override
    public long rate() {
        return this.rate;
    }

    @Override
    public void setRate(long rate) {
        this.rate = rate;
    }

    public static Classifier.Builder builder() {
        return new Classifier.Builder();
    }


    public static final class Builder implements ClassifierFeatures.Builder {
        private String sourceDomain;
        private String destinationDomain;
        private long rate = 0L;

        public Builder() {

        }

        @Override
        public ClassifierFeatures.Builder withSourceDomain(String sourceDomain) {
            this.sourceDomain = sourceDomain;
            return this;
        }

        @Override
        public ClassifierFeatures.Builder withDestinationDomain(String destinationDomain) {
            this.destinationDomain = destinationDomain;
            return this;
        }

        @Override
        public ClassifierFeatures.Builder withRate(long rate) {
            this.rate = rate;
            return this;
        }

        @Override
        public ClassifierFeatures build() {
            Preconditions.checkNotNull(this.sourceDomain, "Must specify a sourceDomain");
            Preconditions.checkNotNull(this.destinationDomain, "Must specify a destinationDomain");
            return new Classifier(this.sourceDomain, this.destinationDomain, this.rate);
        }
    }

}
