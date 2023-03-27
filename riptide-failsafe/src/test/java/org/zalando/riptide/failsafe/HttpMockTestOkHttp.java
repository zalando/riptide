package org.zalando.riptide.failsafe;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.failsafe.CircuitBreaker;
import dev.failsafe.RetryPolicy;
import io.micrometer.core.instrument.util.IOUtils;
import lombok.SneakyThrows;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
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
import java.time.Duration;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicInteger;

import static com.google.common.io.Resources.getResource;
import static java.util.concurrent.Executors.newFixedThreadPool;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.stream.IntStream.range;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeout;
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
import static org.zalando.riptide.failsafe.RetryRoute.retry;
import static org.zalando.riptide.faults.Predicates.alwaysTrue;
import static org.zalando.riptide.faults.TransientFaults.transientConnectionFaults;
import static org.zalando.riptide.faults.TransientFaults.transientSocketFaults;

final class HttpMockTestOkHttp {

    MockWebServer server = new MockWebServer();

    private final CloseableHttpClient client = HttpClientBuilder.create()
            .setDefaultRequestConfig(RequestConfig.custom()
                    .setSocketTimeout(500)
                    .build())
            .build();

    private final AtomicInteger attempt = new AtomicInteger();

    private final Http unit = Http.builder()
            .executor(newFixedThreadPool(2)) // to allow for nested calls
            .requestFactory(new ApacheClientHttpRequestFactory(client))
            .baseUrl( "http://localhost:" + server.getPort() )
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

    @AfterEach
    void tearDown() throws IOException {
        //TODO: where server started?
        client.close();
        server.shutdown();
    }

    @SneakyThrows
    @Test
    void shouldRetrySuccessfully() {
        server.enqueue(new MockResponse().setResponseCode(200).setBody("Hello")
                .setBodyDelay(800, MILLISECONDS)
        );

        server.enqueue(new MockResponse().setResponseCode(200)
                .setBody(IOUtils.toString(getResource("contributors.json").openStream()))
                .setHeader("Content-Type","application/json" ));

        unit.get("/foo")
                .call(pass())
                .join();

        //can't invoke verify so verifying in every test
        assertEquals(2, server.getRequestCount());
    }


    @Test
    void shouldRetryUnsuccessfully() {
        range(0, 5).forEach(i ->
                server.enqueue(new MockResponse().setResponseCode(200).setBody("Hello").throttleBody(4, 800, MILLISECONDS) ));

        final CompletionException exception = assertThrows(CompletionException.class,
                unit.get("/bar").call(pass())::join);

        assertThat(exception.getCause(), is(instanceOf(SocketTimeoutException.class)));
        assertEquals(5, server.getRequestCount());
    }

    @Test
    void shouldRetryExplicitly() {

        server.enqueue(new MockResponse().setResponseCode(503).setHeader("X-RateLimit-Reset", "1523486068"));
        server.enqueue(new MockResponse().setResponseCode(204));

        unit.get("/baz")
                .dispatch(series(),
                        on(SUCCESSFUL).call(pass()),
                        anySeries().dispatch(status(),
                                on(SERVICE_UNAVAILABLE).call(retry())))
                .join();

        assertEquals(2, server.getRequestCount());
    }

    @SneakyThrows
    @Test
    void shouldAllowNestedCalls() {
        final Dispatcher dispatcher = new Dispatcher() {

            @Override
            public MockResponse dispatch (RecordedRequest request) throws InterruptedException {

                //not needed here, just to demonstrate dispatch logic
                switch (request.getPath()) {
                    case "/foo":
                        return new MockResponse().setResponseCode(204);
                    case "/bar":
                        return new MockResponse().setResponseCode(204);
                }
                return new MockResponse().setResponseCode(404);
            }
        };
        server.setDispatcher(dispatcher);

        assertTimeout(Duration.ofSeconds(1),
                unit.get("/foo")
                        .call(call(() -> unit.get("/bar").call(pass()).join()))::join);

        assertEquals(2, server.getRequestCount());
        assertEquals("/foo", server.takeRequest().getPath());
        assertEquals("/bar", server.takeRequest().getPath());
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
}
