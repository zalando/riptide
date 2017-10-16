package org.zalando.riptide.failsafe;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.restdriver.clientdriver.ClientDriverRule;
import net.jodah.failsafe.CircuitBreaker;
import net.jodah.failsafe.RetryPolicy;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.core.task.AsyncListenableTaskExecutor;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.scheduling.concurrent.ConcurrentTaskExecutor;
import org.zalando.riptide.Http;
import org.zalando.riptide.Navigators;
import org.zalando.riptide.httpclient.RestAsyncClientHttpRequestFactory;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.IntStream;

import static com.github.restdriver.clientdriver.RestClientDriver.giveEmptyResponse;
import static com.github.restdriver.clientdriver.RestClientDriver.onRequestTo;
import static org.junit.Assert.fail;
import static org.zalando.riptide.Bindings.on;
import static org.zalando.riptide.PassRoute.pass;
import static org.zalando.riptide.failsafe.RetryRoute.retry;

public class FailsafePluginTest {

    @Rule
    public final ClientDriverRule driver = new ClientDriverRule();

    private final CloseableHttpClient client = HttpClientBuilder.create()
            .setDefaultRequestConfig(RequestConfig.custom()
                    .setSocketTimeout(50)
                    .build())
            .build();
    private final AsyncListenableTaskExecutor executor = new ConcurrentTaskExecutor();
    private final RestAsyncClientHttpRequestFactory factory = new RestAsyncClientHttpRequestFactory(client, executor);

    private final Http unit = Http.builder()
            .baseUrl(driver.getBaseUrl())
            .requestFactory(factory)
            .converter(createJsonConverter())
            .plugin(new FailsafePlugin(Executors.newScheduledThreadPool(20))
                    .withRetryPolicy(new RetryPolicy()
                            .retryOn(SocketTimeoutException.class)
                            .withDelay(25, TimeUnit.MILLISECONDS)
                            .withMaxRetries(4))
                    .withCircuitBreaker(new CircuitBreaker()
                            .withFailureThreshold(3, 10)
                            .withSuccessThreshold(5)
                            .withDelay(1, TimeUnit.MINUTES)))
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

    @After
    public void tearDown() throws IOException {
        client.close();
    }

    @Test(expected = SocketTimeoutException.class)
    public void shouldExecute() throws Throwable {
        IntStream.range(0, 5).forEach(i ->
                driver.addExpectation(onRequestTo("/foo"),
                        giveEmptyResponse().after(100, TimeUnit.MILLISECONDS)));

        try {
            unit.get("/foo")
                    .call(pass())
                    .join();
            fail("Expecting exception");
        } catch (final CompletionException e) {
            throw e.getCause();
        }
    }

    @Test(expected = RetryException.class)
    public void shouldRetry() throws Throwable {
        IntStream.range(0, 5).forEach(i ->
                driver.addExpectation(onRequestTo("/foo"),
                        giveEmptyResponse().withStatus(503)));

        try {
            unit.get("/foo")
                    .dispatch(Navigators.status(),
                        on(HttpStatus.SERVICE_UNAVAILABLE).call(retry()))
                    .join();
            fail("Expecting exception");
        } catch (final CompletionException e) {
            throw e.getCause();
        }
    }

}
