package org.zalando.riptide.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public final class Message {

    private final String message;

    // TODO @JsonProperty shouldn't be necessary here...
    public Message(@JsonProperty("message") final String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

}
