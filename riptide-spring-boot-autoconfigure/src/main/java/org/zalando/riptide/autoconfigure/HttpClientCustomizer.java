package org.zalando.riptide.autoconfigure;

import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apiguardian.api.API;

import static org.apiguardian.api.API.Status.STABLE;

@API(status = STABLE)
public interface HttpClientCustomizer {

    void customize(final HttpClientBuilder builder);

}
