package org.zalando.riptide.model;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Builder
class Account {
    String id;
    String revision;
    String name;
}
