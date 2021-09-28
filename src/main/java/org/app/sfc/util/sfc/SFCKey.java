package org.app.sfc.util.sfc;


import java.util.Objects;

public class SFCKey {
    private final String key;

    private SFCKey(String srcDomain, String dstDomain) {
        this.key = srcDomain + dstDomain;
    }

    public static SFCKey key(String srcDomain, String dstDomain) {
        return new SFCKey(srcDomain, dstDomain);
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (obj != null && this.getClass() == obj.getClass()) {
            SFCKey sfcKey = (SFCKey) obj;
            return Objects.equals(this.key, sfcKey.domain());
        } else {
            return false;
        }
    }

    public int hashCode() {
        return Objects.hashCode(this.key);
    }

    private String domain() {
        return this.key;
    }
}
