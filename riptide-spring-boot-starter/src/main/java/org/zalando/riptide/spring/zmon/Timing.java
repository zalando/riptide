package org.zalando.riptide.spring.zmon;

import java.io.Serializable;

import static com.google.common.base.Preconditions.checkNotNull;

class Timing implements Serializable {

    static final String ATTRIBUTE = Timing.class.getName();

    private final String method;
    private final String host;
    private final long startTime;

    public Timing(final String method, final String host, final long startTime) {
        checkNotNull(method);
        checkNotNull(host);
        this.method = method;
        this.host = host;
        this.startTime = startTime;
    }

    public String getMethod() {
        return method;
    }

    public String getHost() {
        return host;
    }

    public long getStartTime() {
        return startTime;
    }

}
