package org.zalando.riptide.autoconfigure;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.zalando.riptide.Http;
import org.zalando.riptide.autoconfigure.DefaultTestConfiguration;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.zalando.riptide.Route.call;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = DefaultTestConfiguration.class)
@ActiveProfiles("default")
public class NetworkMetricsTest {

    @Autowired
    @Qualifier("foo")
    private Http foo;

    @Autowired
    private SimpleMeterRegistry registry;

    @Test
    public void shouldRecordConnectionPools() {
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
