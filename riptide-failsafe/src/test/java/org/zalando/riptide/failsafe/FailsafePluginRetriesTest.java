package org.zalando.riptide.failsafe;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.failsafe.CircuitBreaker;
import dev.failsafe.RetryPolicy;
import lombok.SneakyThrows;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.core5.util.Timeout;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.zalando.riptide.Http;
import org.zalando.riptide.Plugin;
import org.zalando.riptide.RequestExecution;
import org.zalando.riptide.httpclient.ApacheClientHttpRequestFactory;
import org.zalando.riptide.idempotency.IdempotencyPredicate;

import javax.annotation.Nullable;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.Collections.emptyList;
import static java.util.concurrent.Executors.newCachedThreadPool;
import static java.util.concurrent.Executors.newFixedThreadPool;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.stream.IntStream.range;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeout;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE;
import static org.springframework.http.HttpStatus.Series.SUCCESSFUL;
import static org.zalando.riptide.Attributes.RETRIES;
import static org.zalando.riptide.Bindings.anySeries;
import static org.zalando.riptide.Bindings.on;
import static org.zalando.riptide.Navigators.series;
import static org.zalando.riptide.Navigators.status;
import static org.zalando.riptide.PassRoute.pass;
import static org.zalando.riptide.Route.call;
import static org.zalando.riptide.failsafe.CheckedPredicateConverter.toCheckedPredicate;
import static org.zalando.riptide.failsafe.MockWebServerUtil.emptyMockResponse;
import static org.zalando.riptide.failsafe.MockWebServerUtil.getBaseUrl;
import static org.zalando.riptide.failsafe.MockWebServerUtil.verify;
import static org.zalando.riptide.failsafe.RetryRoute.retry;
import static org.zalando.riptide.faults.Predicates.alwaysTrue;
import static org.zalando.riptide.faults.TransientFaults.transientConnectionFaults;
import static org.zalando.riptide.faults.TransientFaults.transientSocketFaults;

final class FailsafePluginRetriesTest {

    private final MockWebServer server = new MockWebServer();

    private final CloseableHttpClient client = HttpClientBuilder.create()
            .setConnectionManager(PoolingHttpClientConnectionManagerBuilder.create()
                    .setDefaultConnectionConfig(ConnectionConfig.custom()
                            .setSocketTimeout(Timeout.ofMilliseconds(500))
                            .build())
                    .build())
            .build();

    private final AtomicInteger attempt = new AtomicInteger();

    private final Http unit = Http.builder()
            .executor(newFixedThreadPool(2)) // to allow for nested calls
            .requestFactory(new ApacheClientHttpRequestFactory(client))
            .baseUrl(getBaseUrl(server))
            .converter(createJsonConverter())
            .plugin(new Plugin() {
                @Override
                public RequestExecution aroundNetwork(final RequestExecution execution) {
                    return arguments -> {
                        arguments.getAttribute(RETRIES).ifPresent(attempt::set);
                        return execution.execute(arguments);
                    };
                }
            })
            .plugin(new FailsafePlugin()
                    .withPolicy(new RetryRequestPolicy(
                            RetryPolicy.<ClientHttpResponse>builder()
                                    .handleIf(toCheckedPredicate(transientSocketFaults()))
                                    .handle(RetryException.class)
                                    .handleResultIf(this::isBadGateway)
                                    .withDelay(Duration.ofMillis(500))
                                    .withMaxRetries(4)
                                    .build())
                            .withPredicate(new IdempotencyPredicate()))
                    .withPolicy(new RetryRequestPolicy(
                            RetryPolicy.<ClientHttpResponse>builder()
                                    .handleIf(toCheckedPredicate(transientConnectionFaults()))
                                    .withDelay(Duration.ofMillis(500))
                                    .withMaxRetries(4)
                                    .build())
                            .withPredicate(alwaysTrue()))
                    .withPolicy(CircuitBreaker.<ClientHttpResponse>builder()
                            .withFailureThreshold(5, 10)
                            .withSuccessThreshold(5)
                            .withDelay(Duration.ofMinutes(1))
                            .build()))
            .build();

    @SneakyThrows
    private boolean isBadGateway(@Nullable final ClientHttpResponse response) {
        return response != null && response.getStatusCode() == HttpStatus.BAD_GATEWAY;
    }

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
    void shouldRetrySuccessfully() {
        server.enqueue(emptyMockResponse().setHeadersDelay(800, MILLISECONDS));
        server.enqueue(emptyMockResponse());

        unit.get("/foo")
                .call(pass())
                .join();

        verify(server, 2, "/foo");
    }

    @Test
    void wontRetrySocketFaultForNonIdempotentMethod() {
        server.enqueue(emptyMockResponse().setHeadersDelay(800, MILLISECONDS));

        final CompletionException exception = assertThrows(CompletionException.class,
                unit.post("/foo").call(pass())::join);

        assertThat(exception.getCause(), is(instanceOf(SocketTimeoutException.class)));
        verify(server, 1, "/foo", POST.toString());
    }

    @Test
    void retriesConnectionFaultForNonIdempotentMethod() {
        final CompletionException exception = assertThrows(CompletionException.class,
                unit.post("http://" + UUID.randomUUID() + "/foo").call(pass())::join);

        assertThat(exception.getCause(), is(instanceOf(UnknownHostException.class)));
        assertEquals(4, attempt.get());

        verify(server, 0, "");
    }

    @Test
    void shouldRetryCustomDetectedIdempotentRequest() {
        final Http unit = Http.builder()
                .executor(newCachedThreadPool())
                .requestFactory(new ApacheClientHttpRequestFactory(client))
                .baseUrl(getBaseUrl(server))
                .converter(createJsonConverter())
                .plugin(new FailsafePlugin().withPolicy(
                        new RetryRequestPolicy(
                                RetryPolicy.<ClientHttpResponse>builder()
                                        .withDelay(Duration.ofMillis(500))
                                        .withMaxRetries(1)
                                        .build())
                                .withPredicate(arguments ->
                                        arguments.getHeaders().getOrDefault("Idempotent",
                                                emptyList()).contains(
                                                "true"))))
                .build();

        server.enqueue(emptyMockResponse().setHeadersDelay(800, MILLISECONDS));
        server.enqueue(emptyMockResponse());

        unit.post("/foo")
                .header("Idempotent", "true")
                .call(pass())
                .join();

        verify(server, 2, "/foo", POST.toString());
    }

    @Test
    void shouldRetryUnsuccessfully() {
        range(0, 5).forEach(i ->
                server.enqueue(emptyMockResponse().setHeadersDelay(800, MILLISECONDS))
        );

        final CompletionException exception = assertThrows(CompletionException.class,
                unit.get("/bar").call(pass())::join);

        assertThat(exception.getCause(), is(instanceOf(SocketTimeoutException.class)));
        verify(server, 5, "/bar");
    }

    @Test
    void shouldRetryExplicitly() {
        server.enqueue(new MockResponse().setResponseCode(SERVICE_UNAVAILABLE.value()));
        server.enqueue(emptyMockResponse());

        unit.get("/baz")
                .dispatch(series(),
                        on(SUCCESSFUL).call(pass()),
                        anySeries().dispatch(status(),
                                on(SERVICE_UNAVAILABLE).call(retry())))
                .join();
        verify(server, 2, "/baz");
    }

    @Test
    void shouldAllowNestedCalls() {
        server.enqueue(emptyMockResponse());
        server.enqueue(emptyMockResponse());

        assertTimeout(Duration.ofSeconds(1),
                unit.get("/foo")
                        .call(call(() -> unit.get("/bar").call(pass()).join()))::join);
        verify(server, "/foo", "/bar");

    }

}
