package org.zalando.riptide.autoconfigure;

import org.apache.hc.client5.http.HttpRoute;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.core5.function.Resolver;
import org.apache.hc.core5.util.Timeout;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.stereotype.Component;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.client.RestOperations;
import org.zalando.riptide.Http;

import java.lang.reflect.Field;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
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
    void shouldApplyTimeouts() throws NoSuchFieldException, IllegalAccessException {
        Field connManagerField = exampleHttpClient.getClass().getDeclaredField("connManager");
        connManagerField.setAccessible(true);
        PoolingHttpClientConnectionManager connManager =
                (PoolingHttpClientConnectionManager) connManagerField.get(exampleHttpClient);


        Field connectionConfigResolverField = connManager.getClass().getDeclaredField("connectionConfigResolver");
        connectionConfigResolverField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Resolver<HttpRoute, ConnectionConfig> connectionConfigResolver =
                (Resolver<HttpRoute, ConnectionConfig>) connectionConfigResolverField.get(connManager);

        ConnectionConfig connectionConfig = connectionConfigResolver.resolve(null);

        assertThat(connectionConfig.getSocketTimeout(), is(Timeout.of(34, TimeUnit.HOURS)));
        assertThat(connectionConfig.getConnectTimeout(), is(Timeout.of(12, TimeUnit.MINUTES)));
    }

}
