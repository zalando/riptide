package org.zalando.riptide.autoconfigure;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.zalando.riptide.Http;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.NONE;
import static org.zalando.riptide.Route.call;

@SpringBootTest(
        classes = DefaultTestConfiguration.class,
        webEnvironment = NONE)
@ActiveProfiles("default")
final class NetworkMetricsTest {

    @Autowired
    @Qualifier("foo")
    private Http foo;

    @Autowired
    private SimpleMeterRegistry registry;

    @Test
    void shouldRecordConnectionPools() {
        final String url = "https://example.org";

        verify("http.client.connections.available", 0.0);
        verify("http.client.connections.leased", 0.0);
        verify("http.client.connections.total", 0.0);
        verify("http.client.connections.min", 0.0);
        verify("http.client.connections.max", 20.0);
        verify("http.client.connections.queued", 0.0);

        foo.get(url).call(call(first -> {
            verify("http.client.connections.available", 0.0);
            verify("http.client.connections.leased", 1.0);
            verify("http.client.connections.total", 1.0);
            verify("http.client.connections.min", 0.0);
            verify("http.client.connections.max", 20.0);
            verify("http.client.connections.queued", 0.0);

            foo.get(url).call(call(second -> {
                verify("http.client.connections.available", 0.0);
                verify("http.client.connections.leased", 2.0);
                verify("http.client.connections.total", 2.0);
                verify("http.client.connections.min", 0.0);
                verify("http.client.connections.max", 20.0);
                verify("http.client.connections.queued", 0.0);
            }));
        }));

        verify("http.client.connections.available", 0.0);
        verify("http.client.connections.leased", 0.0);
        verify("http.client.connections.total", 0.0);
        verify("http.client.connections.min", 0.0);
        verify("http.client.connections.max", 20.0);
        verify("http.client.connections.queued", 0.0);
    }

    private void verify(final String name, final double value) {
        assertThat(gauge(name).value(), is(value));
    }

    private Gauge gauge(final String name) {
        return registry.find(name).tag("client_id", "foo").gauge();
    }

}
