package org.zalando.riptide.autoconfigure;

import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.Configurable;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.stereotype.Component;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.client.RestOperations;
import org.zalando.riptide.Http;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.NONE;

@SpringBootTest(classes = DefaultTestConfiguration.class, webEnvironment = NONE)
@TestPropertySource(properties = {
    "riptide.defaults.connections.connect-timeout: 1 second",
    "riptide.defaults.connections.socket-timeout: 2 seconds",
    "riptide.defaults.connections.time-to-live: 1 minute",
    "riptide.defaults.connections.max-per-route: 12",
    "riptide.defaults.connections.max-total: 12",
    "riptide.clients.example.connections.connect-timeout: 12 minutes",
    "riptide.clients.example.connections.socket-timeout: 34 hours",
    "riptide.clients.example.connections.time-to-live: 1 day",
    "riptide.clients.example.connections.max-per-route: 24",
    "riptide.clients.example.connections.max-total: 24",
})
@Component
final class ClientConfigurationTest {

    @Autowired
    @Qualifier("example")
    private Http exampleRest;

    @Autowired
    @Qualifier("ecb")
    private Http ecbRest;

    @Autowired
    @Qualifier("example")
    private RestOperations exampleRestOperations;

    @Autowired
    @Qualifier("example")
    private HttpClient exampleHttpClient;

    @Test
    void shouldWireOAuthCorrectly() {
        assertThat(exampleRest, is(notNullValue()));
        assertThat(exampleRestOperations, is(notNullValue()));
    }

    @Test
    void shouldWireNonOAuthCorrectly() {
        assertThat(ecbRest, is(notNullValue()));
    }

    @Test
    void shouldApplyTimeouts() {
        assertThat("Configurable http client expected", exampleHttpClient, is(instanceOf(Configurable.class)));

        final RequestConfig config = ((Configurable) exampleHttpClient).getConfig();

        assertThat(config.getSocketTimeout(), is(34 * 60 * 60 * 1000));
        assertThat(config.getConnectTimeout(), is(12 * 60 * 1000));
    }

}
