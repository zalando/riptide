package org.zalando.riptide.metrics;

import org.springframework.http.client.ClientHttpResponse;
import org.zalando.riptide.RequestArguments;

public interface MetricsNameGenerator {

    String generate(RequestArguments arguments, ClientHttpResponse response) throws Exception;

}
