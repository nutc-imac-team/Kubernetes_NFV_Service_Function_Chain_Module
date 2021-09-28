package org.app.sfc.util.sf;

import org.onlab.packet.VlanId;

import java.util.Objects;

public class SFKey {
    private final String domain;

    private SFKey(String domain) {
        this.domain = domain;
    }

    public static SFKey key(String domain) {
        return new SFKey(domain);
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (obj != null && this.getClass() == obj.getClass()) {
            SFKey sfKey = (SFKey) obj;
            return Objects.equals(this.domain, sfKey.domain());
        } else {
            return false;
        }
    }

    public int hashCode() {
        return Objects.hashCode(this.domain);
    }

    private String domain() {
        return this.domain;
    }
}
