package org.zalando.riptide.httpclient.metrics;

import com.github.restdriver.clientdriver.ClientDriverRule;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.core.task.AsyncListenableTaskExecutor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.scheduling.concurrent.ConcurrentTaskExecutor;
import org.zalando.riptide.Http;
import org.zalando.riptide.httpclient.RestAsyncClientHttpRequestFactory;

import java.io.IOException;

import static com.github.restdriver.clientdriver.RestClientDriver.giveEmptyResponse;
import static com.github.restdriver.clientdriver.RestClientDriver.onRequestTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.zalando.riptide.Route.call;

public final class HttpConnectionPoolMetricsTest {

    @Rule
    public final ClientDriverRule driver = new ClientDriverRule();

    private final PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();

    private final CloseableHttpClient client = HttpClientBuilder.create()
            .setConnectionManager(connectionManager)
            .build();

    private final AsyncListenableTaskExecutor executor = new ConcurrentTaskExecutor();
    private final RestAsyncClientHttpRequestFactory factory = new RestAsyncClientHttpRequestFactory(client, executor);

    private final Http http = Http.builder()
            .baseUrl(driver.getBaseUrl())
            .requestFactory(factory)
            .build();

    private final MeterRegistry meterRegistry = new SimpleMeterRegistry();

    @After
    public void closeClient() throws IOException {
        client.close();
    }

    @Test
    public void shouldRecordConnectionPoolMetrics() {
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
