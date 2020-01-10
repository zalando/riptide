package org.zalando.riptide.failsafe;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.restdriver.clientdriver.ClientDriver;
import com.github.restdriver.clientdriver.ClientDriverFactory;
import lombok.SneakyThrows;
import net.jodah.failsafe.CircuitBreaker;
import net.jodah.failsafe.RetryPolicy;
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
import java.net.UnknownHostException;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicInteger;

import static com.github.restdriver.clientdriver.ClientDriverRequest.Method.POST;
import static com.github.restdriver.clientdriver.RestClientDriver.giveEmptyResponse;
import static com.github.restdriver.clientdriver.RestClientDriver.onRequestTo;
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
import static org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE;
import static org.springframework.http.HttpStatus.Series.SUCCESSFUL;
import static org.zalando.riptide.Attributes.RETRIES;
import static org.zalando.riptide.Bindings.anySeries;
import static org.zalando.riptide.Bindings.on;
import static org.zalando.riptide.Navigators.series;
import static org.zalando.riptide.Navigators.status;
import static org.zalando.riptide.PassRoute.pass;
import static org.zalando.riptide.Route.call;
import static org.zalando.riptide.failsafe.RetryRoute.retry;
import static org.zalando.riptide.faults.Predicates.alwaysTrue;
import static org.zalando.riptide.faults.TransientFaults.transientConnectionFaults;
import static org.zalando.riptide.faults.TransientFaults.transientSocketFaults;

final class FailsafePluginRetriesTest {

    private final ClientDriver driver = new ClientDriverFactory().createClientDriver();

    private final CloseableHttpClient client = HttpClientBuilder.create()
            .setDefaultRequestConfig(RequestConfig.custom()
                    .setSocketTimeout(500)
                    .build())
            .build();

    private final AtomicInteger attemps = new AtomicInteger();

    private final Http unit = Http.builder()
            .executor(newFixedThreadPool(2)) // to allow for nested calls
            .requestFactory(new ApacheClientHttpRequestFactory(client))
            .baseUrl(driver.getBaseUrl())
            .converter(createJsonConverter())
            .plugin(new Plugin() {
                @Override
                public RequestExecution aroundNetwork(final RequestExecution execution) {
                    return arguments -> {
                        arguments.getAttribute(RETRIES).ifPresent(attemps::set);
                        return execution.execute(arguments);
                    };
                }
            })
            .plugin(new FailsafePlugin()
                    .withPolicy(new RetryRequestPolicy(
                            new RetryPolicy<ClientHttpResponse>()
                                    .handleIf(transientSocketFaults())
                                    .handle(RetryException.class)
                                    .handleResultIf(this::isBadGateway)
                                    .withDelay(Duration.ofMillis(500))
                                    .withMaxRetries(4))
                            .withPredicate(new IdempotencyPredicate()))
                    .withPolicy(new RetryRequestPolicy(
                            new RetryPolicy<ClientHttpResponse>()
                                    .handleIf(transientConnectionFaults())
                                    .withDelay(Duration.ofMillis(500))
                                    .withMaxRetries(4))
                            .withPredicate(alwaysTrue()))
                    .withPolicy(new CircuitBreaker<ClientHttpResponse>()
                            .withFailureThreshold(3, 10)
                            .withSuccessThreshold(5)
                            .withDelay(Duration.ofMinutes(1))))
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
        driver.verify();
        client.close();
    }

    @Test
    void shouldRetrySuccessfully() {
        driver.addExpectation(onRequestTo("/foo"), giveEmptyResponse().after(800, MILLISECONDS));
        driver.addExpectation(onRequestTo("/foo"), giveEmptyResponse());

        unit.get("/foo")
                .call(pass())
                .join();
    }

    @Test
    void wontRetrySocketFaultForNonIdempotentMethod() {
        driver.addExpectation(onRequestTo("/foo").withMethod(POST),
                giveEmptyResponse().after(800, MILLISECONDS));

        final CompletionException exception = assertThrows(CompletionException.class,
                unit.post("/foo").call(pass())::join);

        assertThat(exception.getCause(), is(instanceOf(SocketTimeoutException.class)));
    }

    @Test
    void retriesConnectionFaultForNonIdempotentMethod() {
        final CompletionException exception = assertThrows(CompletionException.class,
                unit.post("http://" + UUID.randomUUID() + "/foo").call(pass())::join);

        assertThat(exception.getCause(), is(instanceOf(UnknownHostException.class)));
        assertEquals(4, attemps.get());
    }

    @Test
    void shouldRetryCustomDetectedIdempotentRequest() {
        final Http unit = Http.builder()
                .executor(newCachedThreadPool())
                .requestFactory(new ApacheClientHttpRequestFactory(client))
                .baseUrl(driver.getBaseUrl())
                .converter(createJsonConverter())
                .plugin(new FailsafePlugin().withPolicy(
                        new RetryRequestPolicy(
                                new RetryPolicy<ClientHttpResponse>()
                                        .withDelay(Duration.ofMillis(500))
                                        .withMaxRetries(1))
                                .withPredicate(arguments ->
                                        arguments.getHeaders().getOrDefault("Idempotent",
                                                emptyList()).contains(
                                                "true"))))
                .build();

        driver.addExpectation(onRequestTo("/foo").withMethod(POST), giveEmptyResponse().after(800, MILLISECONDS));
        driver.addExpectation(onRequestTo("/foo").withMethod(POST), giveEmptyResponse());

        unit.post("/foo")
                .header("Idempotent", "true")
                .call(pass())
                .join();
    }

    @Test
    void shouldRetryUnsuccessfully() {
        range(0, 5).forEach(i ->
                driver.addExpectation(onRequestTo("/bar"), giveEmptyResponse().after(800, MILLISECONDS)));

        final CompletionException exception = assertThrows(CompletionException.class,
                unit.get("/bar").call(pass())::join);

        assertThat(exception.getCause(), is(instanceOf(SocketTimeoutException.class)));
    }

    @Test
    void shouldRetryExplicitly() {
        driver.addExpectation(onRequestTo("/baz"), giveEmptyResponse().withStatus(503));
        driver.addExpectation(onRequestTo("/baz"), giveEmptyResponse());

        unit.get("/baz")
                .dispatch(series(),
                        on(SUCCESSFUL).call(pass()),
                        anySeries().dispatch(status(),
                                on(SERVICE_UNAVAILABLE).call(retry())))
                .join();
    }

    @Test
    void shouldAllowNestedCalls() {
        driver.addExpectation(onRequestTo("/foo"), giveEmptyResponse());
        driver.addExpectation(onRequestTo("/bar"), giveEmptyResponse());

        assertTimeout(Duration.ofSeconds(1),
                unit.get("/foo")
                        .call(call(() -> unit.get("/bar").call(pass()).join()))::join);
    }

}
