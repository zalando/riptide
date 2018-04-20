package org.zalando.riptide;

import org.apiguardian.api.API;

import static org.apiguardian.api.API.Status.STABLE;

@API(status = STABLE)
@FunctionalInterface
public interface HttpConfigurer {

    void configure(final HttpBuilder builder);

}
