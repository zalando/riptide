package org.zalando.riptide.failsafe;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Stopwatch;
import dev.failsafe.CircuitBreaker;
import dev.failsafe.RetryPolicy;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
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

import java.io.IOException;
import java.time.Clock;
import java.time.Duration;

import static java.time.Instant.parse;
import static java.time.ZoneOffset.UTC;
import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.junit.jupiter.api.Assertions.assertTimeout;
import static org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE;
import static org.springframework.http.HttpStatus.Series.SUCCESSFUL;
import static org.zalando.riptide.Bindings.anySeries;
import static org.zalando.riptide.Bindings.on;
import static org.zalando.riptide.Navigators.series;
import static org.zalando.riptide.Navigators.status;
import static org.zalando.riptide.PassRoute.pass;
import static org.zalando.riptide.failsafe.MockWebServerUtil.emptyMockResponse;
import static org.zalando.riptide.failsafe.MockWebServerUtil.getBaseUrl;
import static org.zalando.riptide.failsafe.MockWebServerUtil.verify;
import static org.zalando.riptide.failsafe.RetryRoute.retry;

final class RateLimitResetDelayFunctionTest {

    private final MockWebServer server = new MockWebServer();

    private final CloseableHttpClient client = HttpClientBuilder.create()
            .setDefaultRequestConfig(RequestConfig.custom()
                    .setSocketTimeout(1000)
                    .build())
            .build();

    private final Clock clock = Clock.fixed(parse("2018-04-11T22:34:27Z"), UTC);

    private final Http unit = Http.builder()
            .executor(newSingleThreadExecutor())
            .requestFactory(new ApacheClientHttpRequestFactory(client))
            .baseUrl(getBaseUrl(server))
            .converter(createJsonConverter())
            .plugin(new FailsafePlugin()
                    .withPolicy(CircuitBreaker.<ClientHttpResponse>builder()
                            .build())
                    .withPolicy(RetryPolicy.<ClientHttpResponse>builder()
                            .withDelay(Duration.ofSeconds(2))
                            .withDelayFn(new RateLimitResetDelayFunction(clock))
                            .withMaxDuration(Duration.ofSeconds(5))
                            .withMaxRetries(4)
                            .build()))
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
    void shouldRetryWithoutDynamicDelay() {
        server.enqueue(new MockResponse().setResponseCode(SERVICE_UNAVAILABLE.value()));
        server.enqueue(emptyMockResponse());


        unit.get("/baz")
                .dispatch(series(),
                        on(SUCCESSFUL).call(pass()),
                        anySeries().dispatch(status(),
                                on(HttpStatus.SERVICE_UNAVAILABLE).call(retry())))
                .join();

        verify(server, 2, "/baz");
    }

    @Test
    void shouldIgnoreDynamicDelayOnInvalidFormatAndRetryImmediately() {
        server.enqueue(new MockResponse().setResponseCode(SERVICE_UNAVAILABLE.value())
                .setHeader("X-RateLimit-Reset", "foo")
        );
        server.enqueue(emptyMockResponse());

        unit.get("/baz")
                .dispatch(series(),
                        on(SUCCESSFUL).call(pass()),
                        anySeries().dispatch(status(),
                                on(HttpStatus.SERVICE_UNAVAILABLE).call(retry())))
                .join();

        verify(server, 2, "/baz");
    }

    @Test
    void shouldRetryOnDemandWithDelaySeconds() {
        server.enqueue(new MockResponse().setResponseCode(SERVICE_UNAVAILABLE.value())
                .setHeader("X-RateLimit-Reset", "1")
        );
        server.enqueue(emptyMockResponse());

        assertTimeout(Duration.ofMillis(1500), () ->
                atLeast(Duration.ofSeconds(1), () -> unit.get("/baz")
                        .dispatch(series(),
                                on(SUCCESSFUL).call(pass()),
                                anySeries().dispatch(status(),
                                        on(HttpStatus.SERVICE_UNAVAILABLE).call(retry())))
                        .join()));

        verify(server, 2, "/baz");
    }

    @Test
    void shouldRetryWithDelayEpochSeconds() {
        server.enqueue(new MockResponse().setResponseCode(SERVICE_UNAVAILABLE.value())
                .setHeader("X-RateLimit-Reset", "1523486068")
        );
        server.enqueue(emptyMockResponse());

        assertTimeout(Duration.ofMillis(2000), () ->
                atLeast(Duration.ofSeconds(1), () -> unit.get("/baz")
                        .dispatch(series(),
                                on(SUCCESSFUL).call(pass()))
                        .join()));

        verify(server, 2, "/baz");
    }

    private void atLeast(final Duration minimum, final Runnable runnable) {
        final Duration actual = time(runnable);

        assertThat(actual, greaterThanOrEqualTo(minimum));
    }

    private Duration time(final Runnable runnable) {
        final Stopwatch stopwatch = Stopwatch.createStarted();
        runnable.run();
        return stopwatch.stop().elapsed();
    }

}
