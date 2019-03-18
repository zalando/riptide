package org.zalando.riptide.failsafe;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.restdriver.clientdriver.ClientDriver;
import com.github.restdriver.clientdriver.ClientDriverFactory;
import com.google.common.collect.ImmutableList;
import lombok.SneakyThrows;
import net.jodah.failsafe.CircuitBreaker;
import net.jodah.failsafe.RetryPolicy;
import net.jodah.failsafe.event.ExecutionAttemptedEvent;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.zalando.riptide.Http;
import org.zalando.riptide.httpclient.ApacheClientHttpRequestFactory;

import javax.annotation.Nullable;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.time.Duration;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import static com.github.restdriver.clientdriver.ClientDriverRequest.Method.POST;
import static com.github.restdriver.clientdriver.RestClientDriver.giveEmptyResponse;
import static com.github.restdriver.clientdriver.RestClientDriver.onRequestTo;
import static java.util.Collections.emptyList;
import static java.util.concurrent.Executors.newCachedThreadPool;
import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.stream.IntStream.range;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hobsoft.hamcrest.compose.ComposeMatchers.hasFeature;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeout;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;
import static org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE;
import static org.springframework.http.HttpStatus.Series.SUCCESSFUL;
import static org.zalando.riptide.Bindings.anySeries;
import static org.zalando.riptide.Bindings.anyStatus;
import static org.zalando.riptide.Bindings.on;
import static org.zalando.riptide.Navigators.series;
import static org.zalando.riptide.Navigators.status;
import static org.zalando.riptide.PassRoute.pass;
import static org.zalando.riptide.Route.call;
import static org.zalando.riptide.failsafe.RetryRoute.retry;

final class FailsafePluginRetriesTest {

    private final ClientDriver driver = new ClientDriverFactory().createClientDriver();

    private final CloseableHttpClient client = HttpClientBuilder.create()
            .setDefaultRequestConfig(RequestConfig.custom()
                    .setSocketTimeout(500)
                    .build())
            .build();

    private final RetryListener listeners = mock(RetryListener.class);

    private final Http unit = Http.builder()
            .executor(newSingleThreadExecutor())
            .requestFactory(new ApacheClientHttpRequestFactory(client))
            .baseUrl(driver.getBaseUrl())
            .converter(createJsonConverter())
            .plugin(new FailsafePlugin(
                    ImmutableList.of(
                            new RetryPolicy<ClientHttpResponse>()
                                    .withDelay(Duration.ofMillis(500))
                                    .withMaxRetries(4)
                                    .handle(Exception.class)
                                    .handleResultIf(this::isBadGateway),
                            new CircuitBreaker<ClientHttpResponse>()
                                    .withFailureThreshold(3, 10)
                                    .withSuccessThreshold(5)
                                    .withDelay(Duration.ofMinutes(1))
                    ),
                    new ScheduledThreadPoolExecutor(2))
                    .withListener(listeners))
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
    void shouldNotRetryNonIdempotentMethod() {
        driver.addExpectation(onRequestTo("/foo").withMethod(POST),
                giveEmptyResponse().after(800, MILLISECONDS));

        final CompletionException exception = assertThrows(CompletionException.class,
                unit.post("/foo").call(pass())::join);

        assertThat(exception.getCause(), is(instanceOf(SocketTimeoutException.class)));
    }

    @Test
    void shouldRetryCustomDetectedIdempotentRequest() {
        final Http unit = Http.builder()
                .executor(newCachedThreadPool())
                .requestFactory(new ApacheClientHttpRequestFactory(client))
                .baseUrl(driver.getBaseUrl())
                .converter(createJsonConverter())
                .plugin(new FailsafePlugin(
                        ImmutableList.of(
                                new RetryPolicy<ClientHttpResponse>()
                                        .withDelay(Duration.ofMillis(500))
                                        .withMaxRetries(1)
                        ),
                        newSingleThreadScheduledExecutor())
                        .withPredicate(arguments ->
                                arguments.getHeaders().getOrDefault("Idempotent", emptyList()).contains("true"))
                        .withListener(listeners))
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
    void shouldInvokeListenersOnFailure() {
        driver.addExpectation(onRequestTo("/foo"), giveEmptyResponse().after(800, MILLISECONDS));
        driver.addExpectation(onRequestTo("/foo"), giveEmptyResponse());

        unit.get("/foo")
                .call(pass())
                .join();

        verify(listeners).onRetry(notNull(), argThat(hasFeature(ExecutionAttemptedEvent::getLastResult, nullValue())));
    }

    @Test
    void shouldInvokeListenersOnRetryableResult() {
        driver.addExpectation(onRequestTo("/baz"), giveEmptyResponse().withStatus(502));
        driver.addExpectation(onRequestTo("/baz"), giveEmptyResponse());

        unit.get("/baz")
                .call(pass())
                .join();

        verify(listeners).onRetry(notNull(),
                argThat(hasFeature(ExecutionAttemptedEvent::getLastResult, notNullValue())));
    }

    @Test
    void shouldInvokeListenersOnExplicitRetry() {
        driver.addExpectation(onRequestTo("/baz"), giveEmptyResponse().withStatus(503));
        driver.addExpectation(onRequestTo("/baz"), giveEmptyResponse());

        unit.get("/baz")
                .dispatch(status(),
                        on(SERVICE_UNAVAILABLE).call(retry()),
                        anyStatus().call(pass()))
                .join();

        verify(listeners).onRetry(notNull(), argThat(hasFeature(ExecutionAttemptedEvent::getLastResult, nullValue())));
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
