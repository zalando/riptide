package org.zalando.riptide.model;

import lombok.Value;

import java.net.URI;

@Value
public final class Problem {

    private final URI type;
    private final String title;
    private final int status;
    private final String detail;

}
