package org.zalando.riptide.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public final class Success {

    private final boolean happy;

    public Success(@JsonProperty("happy") final boolean happy) {
        this.happy = happy;
    }

    public boolean isHappy() {
        return happy;
    }

}
