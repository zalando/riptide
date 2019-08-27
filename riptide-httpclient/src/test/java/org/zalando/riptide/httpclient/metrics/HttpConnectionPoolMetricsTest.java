package org.zalando.riptide.httpclient.metrics;

import com.github.restdriver.clientdriver.*;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.*;
import io.micrometer.core.instrument.simple.*;
import org.apache.http.impl.client.*;
import org.apache.http.impl.conn.*;
import org.junit.jupiter.api.*;
import org.springframework.http.client.*;
import org.zalando.riptide.*;
import org.zalando.riptide.httpclient.*;

import java.io.*;
import java.util.concurrent.*;

import static com.github.restdriver.clientdriver.RestClientDriver.*;
import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;
import static org.zalando.riptide.Route.*;

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
