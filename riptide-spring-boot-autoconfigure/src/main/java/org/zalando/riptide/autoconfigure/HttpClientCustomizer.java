package org.zalando.riptide.autoconfigure;

import org.apache.http.impl.client.*;
import org.apiguardian.api.*;

import static org.apiguardian.api.API.Status.*;

@API(status = STABLE)
public interface HttpClientCustomizer {

    void customize(final HttpClientBuilder builder);

}
