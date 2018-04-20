package org.zalando.riptide.metrics;

import org.apiguardian.api.API;
import org.springframework.http.client.ClientHttpResponse;
import org.zalando.riptide.RequestArguments;

import static org.apiguardian.api.API.Status.EXPERIMENTAL;

@API(status = EXPERIMENTAL)
public interface MetricsNameGenerator {

    String generate(RequestArguments arguments, ClientHttpResponse response) throws Exception;

}
