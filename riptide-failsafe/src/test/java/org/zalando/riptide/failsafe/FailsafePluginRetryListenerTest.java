package org.zalando.riptide.failsafe;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.failsafe.CircuitBreaker;
import dev.failsafe.RetryPolicy;
import dev.failsafe.event.ExecutionAttemptedEvent;
import lombok.SneakyThrows;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.zalando.riptide.Http;
import org.zalando.riptide.httpclient.ApacheClientHttpRequestFactory;

import javax.annotation.Nullable;
import java.io.IOException;
import java.time.Duration;

import static java.util.concurrent.Executors.newFixedThreadPool;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hobsoft.hamcrest.compose.ComposeMatchers.hasFeature;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;
import static org.springframework.http.HttpStatus.BAD_GATEWAY;
import static org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE;
import static org.zalando.riptide.Bindings.anyStatus;
import static org.zalando.riptide.Bindings.on;
import static org.zalando.riptide.Navigators.status;
import static org.zalando.riptide.PassRoute.pass;
import static org.zalando.riptide.failsafe.MockWebServerUtil.emptyMockResponse;
import static org.zalando.riptide.failsafe.MockWebServerUtil.getBaseUrl;
import static org.zalando.riptide.failsafe.RetryRoute.retry;

final class FailsafePluginRetryListenerTest {

    private final MockWebServer server = new MockWebServer();

    private final CloseableHttpClient client = HttpClientBuilder.create()
            .setDefaultRequestConfig(RequestConfig.custom()
                    .setSocketTimeout(500)
                    .build())
            .build();

    private final RetryListener listeners = mock(RetryListener.class);

    private final Http unit = Http.builder()
            .executor(newFixedThreadPool(2)) // to allow for nested calls
            .requestFactory(new ApacheClientHttpRequestFactory(client))
            .baseUrl(getBaseUrl(server))
            .converter(createJsonConverter())
            .plugin(new FailsafePlugin()
                    .withPolicy(new RetryRequestPolicy(
                            RetryPolicy.<ClientHttpResponse>builder()
                                    .withDelay(Duration.ofMillis(500))
                                    .withMaxRetries(4)
                                    .handle(Exception.class)
                                    .handleResultIf(this::isBadGateway)
                                    .build())
                            .withListener(listeners))
                    .withPolicy(CircuitBreaker.<ClientHttpResponse>builder()
                            .withFailureThreshold(3, 10)
                            .withSuccessThreshold(5)
                            .withDelay(Duration.ofMinutes(1))
                            .build()))
            .build();

    @SneakyThrows
    private boolean isBadGateway(@Nullable final ClientHttpResponse response) {
        return response != null && response.getStatusCode() == BAD_GATEWAY;
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
    void shouldInvokeListenersOnFailure() {
        server.enqueue(emptyMockResponse().setHeadersDelay(800, MILLISECONDS));
        server.enqueue(emptyMockResponse());

        unit.get("/foo")
                .call(pass())
                .join();

        verify(listeners).onRetry(notNull(), argThat(hasFeature(ExecutionAttemptedEvent::getLastResult, nullValue())));
    }

    @Test
    void shouldInvokeListenersOnRetryableResult() {
        server.enqueue(new MockResponse().setResponseCode(BAD_GATEWAY.value()));
        server.enqueue(emptyMockResponse());

        unit.get("/baz")
                .call(pass())
                .join();

        verify(listeners).onRetry(notNull(),
                argThat(hasFeature(ExecutionAttemptedEvent::getLastResult, notNullValue())));
    }

    @Test
    void shouldInvokeListenersOnExplicitRetry() {
        server.enqueue(new MockResponse().setResponseCode(SERVICE_UNAVAILABLE.value()));
        server.enqueue(emptyMockResponse());

        unit.get("/baz")
                .dispatch(status(),
                        on(SERVICE_UNAVAILABLE).call(retry()),
                        anyStatus().call(pass()))
                .join();

        verify(listeners).onRetry(notNull(), argThat(hasFeature(ExecutionAttemptedEvent::getLastResult, nullValue())));
    }

}
