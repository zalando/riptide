package org.zalando.riptide.model;

import java.net.URI;

public final class Problem {

    private final URI type;
    private final String title;
    private final int status;
    private final String detail;

    public Problem(final URI type, final String title, final int status, final String detail) {
        this.type = type;
        this.title = title;
        this.status = status;
        this.detail = detail;
    }

    public URI getType() {
        return type;
    }

    public String getTitle() {
        return title;
    }

    public int getStatus() {
        return status;
    }

    public String getDetail() {
        return detail;
    }

}
