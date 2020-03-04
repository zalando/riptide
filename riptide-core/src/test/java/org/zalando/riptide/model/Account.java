package org.zalando.riptide.model;

import lombok.Value;

@Value
final class Account {

    private final String id;
    private final String revision;
    private final String name;

}
