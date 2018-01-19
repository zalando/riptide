package org.zalando.riptide.metrics;

import org.springframework.http.client.ClientHttpResponse;
import org.zalando.riptide.RequestArguments;

import java.io.IOException;

public interface MetricsNameGenerator {

    String generate(RequestArguments arguments, ClientHttpResponse response) throws IOException;

}
