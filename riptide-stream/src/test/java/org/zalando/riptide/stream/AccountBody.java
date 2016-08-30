package org.zalando.riptide.stream;

import lombok.Value;

@Value
public final class AccountBody {

    private final String id;
    private final String name;

    public AccountBody(final String id, final String name) {
        this.id = id;
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

}
