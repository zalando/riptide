package org.zalando.riptide.model;

import lombok.Value;

import java.net.URI;

@Value
public final class Error {

    private final String message;
    private final URI path;

}
