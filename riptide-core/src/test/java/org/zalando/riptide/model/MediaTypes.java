package org.zalando.riptide.model;

import org.springframework.http.MediaType;

import static org.springframework.http.MediaType.parseMediaType;

public final class MediaTypes {

    public static final MediaType SUCCESS = parseMediaType("application/success+json");
    public static final MediaType SUCCESS_V1 = parseMediaType("application/success+json;version=1");
    public static final MediaType SUCCESS_V2 = parseMediaType("application/success+json;version=2");
    public static final MediaType ERROR = parseMediaType("application/vnd.error+json");
    public static final MediaType PROBLEM = parseMediaType("application/problem+json");

}
