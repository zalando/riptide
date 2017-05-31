package org.zalando.riptide.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public final class Message {

    private final String message;

    public Message(@JsonProperty("message") final String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

}
