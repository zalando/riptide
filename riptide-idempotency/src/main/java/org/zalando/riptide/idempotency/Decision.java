package org.zalando.riptide.idempotency;

import org.apiguardian.api.API;

import static org.apiguardian.api.API.Status.EXPERIMENTAL;

@API(status = EXPERIMENTAL)
public enum Decision {
    NEUTRAL, ACCEPT, DENY
}
