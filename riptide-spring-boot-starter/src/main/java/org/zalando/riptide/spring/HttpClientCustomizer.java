package org.zalando.riptide.spring;

import org.apache.http.impl.client.HttpClientBuilder;
import org.apiguardian.api.API;

import static org.apiguardian.api.API.Status.STABLE;

@API(status = STABLE)
public interface HttpClientCustomizer {

    void customize(final HttpClientBuilder builder);

}
