package org.zalando.riptide.failsafe;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.failsafe.CircuitBreaker;
import dev.failsafe.RetryPolicy;
import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.client.MockRestServiceServer;
import org.zalando.riptide.Http;
import org.zalando.riptide.Plugin;
import org.zalando.riptide.RequestExecution;
import org.zalando.riptide.idempotency.IdempotencyPredicate;

import javax.annotation.Nullable;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.time.Duration;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicInteger;

import static com.google.common.io.Resources.getResource;
import static java.util.concurrent.Executors.newFixedThreadPool;
import static java.util.stream.IntStream.range;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeout;
import static org.springframework.http.HttpStatus.NO_CONTENT;
import static org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE;
import static org.springframework.http.HttpStatus.Series.SUCCESSFUL;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
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

final class HttpMockTestSpring {

    private final MockSetup mockSetup = new MockSetup();
    private final MockRestServiceServer server = mockSetup.getServer();
    
    private final AtomicInteger attempt = new AtomicInteger();

    private final Http unit = mockSetup.getRestBuilder(newFixedThreadPool(2)) // to allow for nested calls
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
        try {
            server.verify();
        } finally {
            server.reset();
        }
    }

    @SneakyThrows
    @Test
    void shouldRetrySuccessfully() {
        server.expect(requestTo(mockSetup.getBaseUrl() + "/foo")).andRespond((r) ->
        {
            // no way to pass own request factory, see MockRestServiceServer 290,
            // so we need to throw SocketTimeoutException explicitly
            throw new java.net.SocketTimeoutException();
        });
        server.expect(requestTo(mockSetup.getBaseUrl() + "/foo"))
                .andRespond(withSuccess(getResource("contributors.json").openStream().readAllBytes(), MediaType.APPLICATION_JSON));

        unit.get("/foo")
                .call(pass())
                .join();
    }


    @Test
    void shouldRetryUnsuccessfully() {
        range(0, 5).forEach(i ->
                server.expect(requestTo(mockSetup.getBaseUrl() + "/bar")).andRespond((r) ->
                {
                    throw new java.net.SocketTimeoutException();
                })
        );

        final CompletionException exception = assertThrows(CompletionException.class,
                unit.get("/bar").call(pass())::join);

        assertThat(exception.getCause(), is(instanceOf(SocketTimeoutException.class)));
    }

    @Test
    void shouldRetryExplicitly() {
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.set("X-RateLimit-Reset", "1523486068");

        server.expect(requestTo(mockSetup.getBaseUrl() + "/baz")).andRespond(withStatus(SERVICE_UNAVAILABLE).headers(httpHeaders));
        server.expect(requestTo(mockSetup.getBaseUrl() + "/baz")).andRespond(withStatus(NO_CONTENT));

        unit.get("/baz")
                .dispatch(series(),
                        on(SUCCESSFUL).call(pass()),
                        anySeries().dispatch(status(),
                                on(SERVICE_UNAVAILABLE).call(retry())))
                .join();
    }

    @Test
    void shouldAllowNestedCalls() {
        server.expect(requestTo(mockSetup.getBaseUrl() + "/foo")).andRespond(withStatus(NO_CONTENT));
        server.expect(requestTo(mockSetup.getBaseUrl() + "/bar")).andRespond(withStatus(NO_CONTENT));

        assertTimeout(Duration.ofSeconds(1),
                unit.get("/foo")
                        .call(call(() -> unit.get("/bar").call(pass()).join()))::join);
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
