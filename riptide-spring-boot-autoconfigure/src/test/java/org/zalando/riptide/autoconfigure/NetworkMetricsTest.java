package org.zalando.riptide.autoconfigure;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.test.context.ActiveProfiles;
import org.zalando.riptide.Http;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.NONE;
import static org.zalando.riptide.Route.call;

@SpringBootTest(classes = DefaultTestConfiguration.class, webEnvironment = NONE)
@ActiveProfiles("default")
final class NetworkMetricsTest {

    @Autowired
    @Qualifier("foo")
    private Http foo;

    @Autowired
    private SimpleMeterRegistry registry;

    @Test
    void shouldRecordConnectionPools() {
        foo.get("https://example.org").call(call(ClientHttpResponse.class, response -> {
            // not closing to keep the connection leased
        }));
        foo.get("https://example.org").call(call(ClientHttpResponse::close)).join();

        assertThat(gauge("http.client.connections.available").value(), is(1.0));
        assertThat(gauge("http.client.connections.leased").value(), is(1.0));
        assertThat(gauge("http.client.connections.max").value(), is(20.0));
        assertThat(gauge("http.client.connections.pending").value(), is(0.0));
    }

    private Gauge gauge(final String name) {
        return registry.find(name).tag("clientId", "foo").gauge();
    }

}
