package org.zalando.riptide.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public final class Success {

    private final boolean happy;

    // TODO @JsonProperty shouldn't be necessary here...
    public Success(@JsonProperty("happy") final boolean happy) {
        this.happy = happy;
    }

    public boolean isHappy() {
        return happy;
    }

}
