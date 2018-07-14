package org.zalando.riptide.failsafe;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.restdriver.clientdriver.ClientDriverRule;
import lombok.SneakyThrows;
import net.jodah.failsafe.CircuitBreaker;
import net.jodah.failsafe.RetryPolicy;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.scheduling.concurrent.ConcurrentTaskExecutor;
import org.zalando.riptide.Http;
import org.zalando.riptide.httpclient.RestAsyncClientHttpRequestFactory;

import javax.annotation.Nullable;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static com.github.restdriver.clientdriver.ClientDriverRequest.Method.POST;
import static com.github.restdriver.clientdriver.RestClientDriver.giveEmptyResponse;
import static com.github.restdriver.clientdriver.RestClientDriver.onRequestTo;
import static java.util.concurrent.Executors.newCachedThreadPool;
import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.stream.IntStream.range;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
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

public class FailsafePluginRetriesTest {

    @Rule
    public final ClientDriverRule driver = new ClientDriverRule();

    private final CloseableHttpClient client = HttpClientBuilder.create()
            .setDefaultRequestConfig(RequestConfig.custom()
                    .setSocketTimeout(500)
                    .build())
            .build();

    private final RetryListener listeners = mock(RetryListener.class);

    private final Http unit = Http.builder()
            .baseUrl(driver.getBaseUrl())
            .requestFactory(new RestAsyncClientHttpRequestFactory(client,
                    new ConcurrentTaskExecutor(newCachedThreadPool())))
            .converter(createJsonConverter())
            .plugin(new FailsafePlugin(new ScheduledThreadPoolExecutor(2))
                    .withRetryPolicy(new RetryPolicy()
                            .withDelay(500, MILLISECONDS)
                            .withMaxRetries(4)
                            .retryOn(Exception.class)
                            .retryIf(this::isBadGateway))
                    .withCircuitBreaker(new CircuitBreaker()
                            .withFailureThreshold(3, 10)
                            .withSuccessThreshold(5)
                            .withDelay(1, TimeUnit.MINUTES))
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

    @After
    public void tearDown() throws IOException {
        client.close();
    }

    @Test
    public void shouldRetrySuccessfully() {
        driver.addExpectation(onRequestTo("/foo"), giveEmptyResponse().after(800, MILLISECONDS));
        driver.addExpectation(onRequestTo("/foo"), giveEmptyResponse());

        unit.get("/foo")
                .call(pass())
                .join();
    }

    @Test(expected = SocketTimeoutException.class)
    public void shouldNotRetryNonIdempotentMethod() throws Throwable {
        driver.addExpectation(onRequestTo("/foo").withMethod(POST),
                giveEmptyResponse().after(800, MILLISECONDS));

        try {
            unit.post("/foo")
                    .call(pass())
                    .join();
        } catch (final CompletionException e) {
            throw e.getCause();
        }
    }

    @Test
    public void shouldRetryCustomDetectedIdempotentRequest() {
        final Http unit = Http.builder()
                .baseUrl(driver.getBaseUrl())
                .requestFactory(new RestAsyncClientHttpRequestFactory(client,
                        new ConcurrentTaskExecutor(newCachedThreadPool())))
                .converter(createJsonConverter())
                .plugin(new FailsafePlugin(newSingleThreadScheduledExecutor())
                        .withIdempotentMethodDetector(arguments ->
                            arguments.getHeaders().containsEntry("Idempotent", "true"))
                        .withRetryPolicy(new RetryPolicy()
                                .withDelay(500, MILLISECONDS)
                                .withMaxRetries(1))
                        .withListener(listeners))
                .build();

        driver.addExpectation(onRequestTo("/foo").withMethod(POST), giveEmptyResponse().after(800, MILLISECONDS));
        driver.addExpectation(onRequestTo("/foo").withMethod(POST), giveEmptyResponse());

        unit.post("/foo")
                .header("Idempotent", "true")
                .call(pass())
                .join();
    }

    @Test(expected = SocketTimeoutException.class)
    public void shouldRetryUnsuccessfully() throws Throwable {
        range(0, 5).forEach(i ->
                driver.addExpectation(onRequestTo("/bar"), giveEmptyResponse().after(800, MILLISECONDS)));

        try {
            unit.get("/bar")
                    .call(pass())
                    .join();
        } catch (final CompletionException e) {
            throw e.getCause();
        }
    }

    @Test
    public void shouldRetryExplicitly() {
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
    public void shouldInvokeListenersOnFailure() {
        driver.addExpectation(onRequestTo("/foo"), giveEmptyResponse().after(800, MILLISECONDS));
        driver.addExpectation(onRequestTo("/foo"), giveEmptyResponse());

        unit.get("/foo")
                .call(pass())
                .join();

        verify(listeners).onRetry(notNull(), isNull(), notNull(), notNull());
    }

    @Test
    public void shouldInvokeListenersOnRetryableResult() {
        driver.addExpectation(onRequestTo("/baz"), giveEmptyResponse().withStatus(502));
        driver.addExpectation(onRequestTo("/baz"), giveEmptyResponse());

        unit.get("/baz")
                .call(pass())
                .join();

        verify(listeners).onRetry(notNull(), notNull(), isNull(), notNull());
    }

    @Test
    public void shouldInvokeListenersOnExplicitRetry() {
        driver.addExpectation(onRequestTo("/baz"), giveEmptyResponse().withStatus(503));
        driver.addExpectation(onRequestTo("/baz"), giveEmptyResponse());

        unit.get("/baz")
                .dispatch(status(),
                        on(SERVICE_UNAVAILABLE).call(retry()),
                        anyStatus().call(pass()))
                .join();

        verify(listeners).onRetry(notNull(), isNull(), notNull(), notNull());
    }

    @Test(timeout = 1000)
    public void shouldAllowNestedCalls() {
        driver.addExpectation(onRequestTo("/foo"), giveEmptyResponse());
        driver.addExpectation(onRequestTo("/bar"), giveEmptyResponse());

        unit.get("/foo")
                .call(call(() -> unit.get("/bar").call(pass()).join()))
                .join();

    }

}
