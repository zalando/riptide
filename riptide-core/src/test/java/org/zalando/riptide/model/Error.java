package org.zalando.riptide.model;

import java.net.URI;

public final class Error {

    private final String message;
    private final URI path;

    public Error(final String message, final URI path) {
        this.message = message;
        this.path = path;
    }

    public String getMessage() {
        return message;
    }

    public URI getPath() {
        return path;
    }

}
