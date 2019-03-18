package org.zalando.riptide.httpclient.metrics;

import com.github.restdriver.clientdriver.ClientDriver;
import com.github.restdriver.clientdriver.ClientDriverFactory;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpResponse;
import org.zalando.riptide.Http;
import org.zalando.riptide.httpclient.ApacheClientHttpRequestFactory;

import java.io.IOException;
import java.util.concurrent.Executors;

import static com.github.restdriver.clientdriver.RestClientDriver.giveEmptyResponse;
import static com.github.restdriver.clientdriver.RestClientDriver.onRequestTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.zalando.riptide.Route.call;

final class HttpConnectionPoolMetricsTest {

    private final ClientDriver driver = new ClientDriverFactory().createClientDriver();

    private final PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();

    private final CloseableHttpClient client = HttpClientBuilder.create()
            .setConnectionManager(connectionManager)
            .build();

    private final ClientHttpRequestFactory factory = new ApacheClientHttpRequestFactory(client);

    private final Http http = Http.builder()
            .executor(Executors.newSingleThreadExecutor())
            .requestFactory(factory)
            .baseUrl(driver.getBaseUrl())
            .build();

    private final MeterRegistry meterRegistry = new SimpleMeterRegistry();

    @AfterEach
    void closeClient() throws IOException {
        client.close();
    }

    @Test
    void shouldRecordConnectionPoolMetrics() {
        new HttpConnectionPoolMetrics(connectionManager)
                .withMetricName("connection-pool")
                .withDefaultTags(Tag.of("version", "1"))
                .bindTo(meterRegistry);

        driver.addExpectation(onRequestTo("/"), giveEmptyResponse());

        http.get("/").call(call(ClientHttpResponse::close)).join();

        assertThat(gauge("connection-pool.available").value(), is(1.0));
        assertThat(gauge("connection-pool.leased").value(), is(0.0));
        assertThat(gauge("connection-pool.max").value(), is(20.0));
        assertThat(gauge("connection-pool.pending").value(), is(0.0));
    }

    private Gauge gauge(final String name) {
        return meterRegistry.find(name).tag("version", "1").gauge();
    }

}
