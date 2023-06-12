package org.zalando.riptide.failsafe;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.failsafe.CircuitBreaker;
import dev.failsafe.CircuitBreakerOpenException;
import okhttp3.mockwebserver.MockWebServer;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.core5.util.Timeout;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.zalando.riptide.Http;
import org.zalando.riptide.OriginalStackTracePlugin;
import org.zalando.riptide.httpclient.ApacheClientHttpRequestFactory;
import org.zalando.riptide.soap.PreserveContextClassLoaderTaskDecorator;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.time.Duration;
import java.util.concurrent.CompletionException;

import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.zalando.fauxpas.FauxPas.partially;
import static org.zalando.riptide.PassRoute.pass;
import static org.zalando.riptide.failsafe.MockWebServerUtil.*;
import static org.zalando.riptide.failsafe.TaskDecorator.composite;

final class FailsafePluginCircuitBreakerTest {

    private final MockWebServer server = new MockWebServer();

    private final CloseableHttpClient client = HttpClientBuilder.create()
            .setConnectionManager(PoolingHttpClientConnectionManagerBuilder.create()
                    .setDefaultConnectionConfig(ConnectionConfig.custom()
                            .setSocketTimeout(Timeout.ofMilliseconds(500))
                            .build())
                    .build())
            .build();


    private final Http unit = Http.builder()
            .executor(newSingleThreadExecutor())
            .requestFactory(new ApacheClientHttpRequestFactory(client))
            .baseUrl(getBaseUrl(server))
            .converter(createJsonConverter())
            .plugin(new FailsafePlugin()
                    .withPolicy(CircuitBreaker.<ClientHttpResponse>builder()
                            .withDelay(Duration.ofSeconds(1))
                            .build())
                    .withDecorator(composite(
                            TaskDecorator.identity(),
                            new PreserveContextClassLoaderTaskDecorator()
                    ))
                    .withDecorator(TaskDecorator.identity()))
            .plugin(new OriginalStackTracePlugin())
            .build();

    private static MappingJackson2HttpMessageConverter createJsonConverter() {
        final MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
        converter.setObjectMapper(createObjectMapper());
        return converter;
    }

    private static ObjectMapper createObjectMapper() {
        return new ObjectMapper().findAndRegisterModules()
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }

    @AfterEach
    void tearDown() throws IOException {
        client.close();
        server.shutdown();
    }

    @Test
    void shouldOpenCircuit() {

        server.enqueue(emptyMockResponse().setHeadersDelay(800, MILLISECONDS));

        unit.get("/foo").call(pass())
                .exceptionally(partially(SocketTimeoutException.class, this::ignore))
                .join();

        final CompletionException exception = assertThrows(CompletionException.class,
                unit.get("/foo").call(pass())::join);

        assertThat(exception.getCause(), is(instanceOf(CircuitBreakerOpenException.class)));

        verify(server, 1, "/foo");
    }

    private ClientHttpResponse ignore(@SuppressWarnings("unused") final Throwable throwable) {
        return null;
    }

}
