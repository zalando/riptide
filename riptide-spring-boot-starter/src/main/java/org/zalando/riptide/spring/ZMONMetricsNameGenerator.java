package org.zalando.riptide.spring;

import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpResponse;
import org.zalando.riptide.RequestArguments;
import org.zalando.riptide.metrics.MetricsNameGenerator;

import java.io.IOException;

class ZMONMetricsNameGenerator implements MetricsNameGenerator {

    @Override
    public String generate(final RequestArguments arguments, final ClientHttpResponse response) throws IOException {
        final int statusCode = response.getRawStatusCode();
        final HttpMethod method = arguments.getMethod();
        final String host = arguments.getRequestUri().getHost();

        return "zmon.request." + statusCode + "." + method + "." + host;
    }

}
