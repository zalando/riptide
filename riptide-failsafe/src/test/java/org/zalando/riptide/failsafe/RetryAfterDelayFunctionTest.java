package org.zalando.riptide.failsafe;

import com.fasterxml.jackson.databind.*;
import com.github.restdriver.clientdriver.*;
import com.google.common.base.*;
import com.google.common.collect.*;
import net.jodah.failsafe.*;
import org.apache.http.client.config.*;
import org.apache.http.impl.client.*;
import org.junit.jupiter.api.*;
import org.springframework.http.*;
import org.springframework.http.client.*;
import org.springframework.http.converter.json.*;
import org.zalando.riptide.*;
import org.zalando.riptide.httpclient.*;

import java.io.*;
import java.time.Clock;
import java.time.*;

import static com.github.restdriver.clientdriver.RestClientDriver.*;
import static java.time.Instant.*;
import static java.time.ZoneOffset.*;
import static java.util.concurrent.Executors.*;
import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.http.HttpStatus.Series.*;
import static org.zalando.riptide.Bindings.*;
import static org.zalando.riptide.Navigators.*;
import static org.zalando.riptide.PassRoute.*;
import static org.zalando.riptide.failsafe.RetryRoute.*;

final class RetryAfterDelayFunctionTest {

    private final ClientDriver driver = new ClientDriverFactory().createClientDriver();

    private final CloseableHttpClient client = HttpClientBuilder.create()
            .setDefaultRequestConfig(RequestConfig.custom()
                    .setSocketTimeout(1000)
                    .build())
            .build();

    private final Clock clock = Clock.fixed(parse("2018-04-11T22:34:27Z"), UTC);

    private final Http unit = Http.builder()
            .executor(newSingleThreadExecutor())
            .requestFactory(new ApacheClientHttpRequestFactory(client))
            .baseUrl(driver.getBaseUrl())
            .converter(createJsonConverter())
            .plugin(new FailsafePlugin(
                    ImmutableList.of(
                            new CircuitBreaker<ClientHttpResponse>(),
                            new RetryPolicy<ClientHttpResponse>()
                                    .withDelay(Duration.ofSeconds(2))
                                    .withDelay(new RetryAfterDelayFunction(clock))
                                    .withMaxDuration(Duration.ofSeconds(5))
                                    .withMaxRetries(4)),
                    newSingleThreadScheduledExecutor()))
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
    }

    @Test
    void shouldRetryWithoutDynamicDelay() {
        driver.addExpectation(onRequestTo("/baz"), giveEmptyResponse().withStatus(503));
        driver.addExpectation(onRequestTo("/baz"), giveEmptyResponse());

        unit.get("/baz")
                .dispatch(series(),
                        on(SUCCESSFUL).call(pass()),
                        anySeries().dispatch(status(),
                                on(HttpStatus.SERVICE_UNAVAILABLE).call(retry())))
                .join();
    }

    @Test
    void shouldIgnoreDynamicDelayOnInvalidFormatAndRetryImmediately() {
        driver.addExpectation(onRequestTo("/baz"), giveEmptyResponse().withStatus(503)
                .withHeader("Retry-After", "foo"));
        driver.addExpectation(onRequestTo("/baz"), giveEmptyResponse());

        unit.get("/baz")
                .dispatch(series(),
                        on(SUCCESSFUL).call(pass()),
                        anySeries().dispatch(status(),
                                on(HttpStatus.SERVICE_UNAVAILABLE).call(retry())))
                .join();
    }

    @Test
    void shouldRetryOnDemandWithDynamicDelay() {
        driver.addExpectation(onRequestTo("/baz"), giveEmptyResponse().withStatus(503)
                .withHeader("Retry-After", "1"));
        driver.addExpectation(onRequestTo("/baz"), giveEmptyResponse());

        assertTimeout(Duration.ofMillis(1500), () ->
                atLeast(Duration.ofSeconds(1), () -> unit.get("/baz")
                        .dispatch(series(),
                                on(SUCCESSFUL).call(pass()),
                                anySeries().dispatch(status(),
                                        on(HttpStatus.SERVICE_UNAVAILABLE).call(retry())))
                        .join()));
    }

    @Test
    void shouldRetryWithDynamicDelayDate() {
        driver.addExpectation(onRequestTo("/baz"), giveEmptyResponse().withStatus(503)
                .withHeader("Retry-After", "Wed, 11 Apr 2018 22:34:28 GMT"));
        driver.addExpectation(onRequestTo("/baz"), giveEmptyResponse());

        assertTimeout(Duration.ofMillis(2000), () ->
                atLeast(Duration.ofSeconds(1), () -> unit.get("/baz")
                        .dispatch(series(),
                                on(SUCCESSFUL).call(pass()))
                        .join()));
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
