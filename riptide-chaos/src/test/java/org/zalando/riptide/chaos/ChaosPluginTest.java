package org.zalando.riptide.chaos;

import com.github.restdriver.clientdriver.*;
import org.apache.http.client.config.*;
import org.apache.http.impl.client.*;
import org.junit.jupiter.api.*;
import org.springframework.http.client.*;
import org.zalando.riptide.*;

import java.io.*;
import java.net.*;
import java.time.Clock;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;

import static com.github.restdriver.clientdriver.RestClientDriver.*;
import static java.util.concurrent.TimeUnit.*;
import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.http.HttpStatus.*;
import static org.zalando.riptide.PassRoute.*;
import static org.zalando.riptide.chaos.FailureInjection.*;

final class ChaosPluginTest {

    private final ClientDriver driver = new ClientDriverFactory().createClientDriver();

    private final Probability latencyProbability = mock(Probability.class);
    private final Probability exceptionProbability = mock(Probability.class);
    private final Probability errorResponseProbability = mock(Probability.class);

    private final CloseableHttpClient client = HttpClientBuilder.create()
            .setDefaultRequestConfig(RequestConfig.custom()
                    .setSocketTimeout(1500)
                    .build())
            .build();

    private final Http unit = Http.builder()
            .executor(Executors.newSingleThreadExecutor())
            .requestFactory(new HttpComponentsClientHttpRequestFactory(client))
            .baseUrl(driver.getBaseUrl())
            .plugin(new ChaosPlugin(composite(
                    new LatencyInjection(latencyProbability, Clock.systemUTC(), Duration.ofSeconds(1)),
                    new ExceptionInjection(exceptionProbability, Arrays.asList(
                            ConnectException::new,
                            NoRouteToHostException::new
                    )),
                    new ErrorResponseInjection(errorResponseProbability, Arrays.asList(
                            INTERNAL_SERVER_ERROR,
                            SERVICE_UNAVAILABLE
                    ))
            )))
            .build();

    @AfterEach
    void tearDown() throws IOException {
        client.close();
    }

    @Test
    void shouldNotInjectError() {
        driver.addExpectation(onRequestTo("/foo"), giveEmptyResponse());

        unit.get("/foo")
                .call(pass())
                .join();
    }

    @Test
    void shouldInjectLatency() {
        when(latencyProbability.test()).thenReturn(true);

        driver.addExpectation(onRequestTo("/foo"), giveEmptyResponse());

        final Clock clock = Clock.systemUTC();
        final Instant start = clock.instant();

        unit.get("/foo")
                .call(pass())
                .join();

        final Instant end = clock.instant();

        assertThat(Duration.between(start, end), is(greaterThanOrEqualTo(Duration.ofSeconds(1))));
    }

    @Test
    void shouldNotInjectLatencyIfDelayedAlready() {
        when(latencyProbability.test()).thenReturn(true);

        driver.addExpectation(onRequestTo("/foo"), giveEmptyResponse().after(1, SECONDS));

        final Clock clock = Clock.systemUTC();
        final Instant start = clock.instant();

        unit.get("/foo")
                .call(pass())
                .join();

        final Instant end = clock.instant();

        assertThat(Duration.between(start, end), is(lessThan(Duration.ofSeconds(2))));
    }

    @Test
    void shouldInjectErrorResponse() throws IOException {
        when(errorResponseProbability.test()).thenReturn(true);

        driver.addExpectation(onRequestTo("/foo"), giveEmptyResponse());

        final ClientHttpResponse response = unit.get("/foo")
                .call(pass())
                .join();

        // users are required to close Closeable resources by contract
        response.close();

        assertThat(response.getStatusCode(), is(oneOf(INTERNAL_SERVER_ERROR, SERVICE_UNAVAILABLE)));
        assertThat(response.getRawStatusCode(), is(oneOf(500, 503)));
        assertThat(response.getStatusText(), is(oneOf("Internal Server Error", "Service Unavailable")));
        assertThat(response.getHeaders(), is(anEmptyMap())); // TODO can we do better?
    }

    @Test
    void shouldNotInjectErrorResponseIfFailedAlready() throws IOException {
        when(errorResponseProbability.test()).thenReturn(true);

        driver.addExpectation(onRequestTo("/foo"), giveEmptyResponse().withStatus(400));

        final ClientHttpResponse response = unit.get("/foo")
                .call(pass())
                .join();

        assertThat(response.getStatusCode(), is(BAD_REQUEST));
    }

    @Test
    void shouldInjectException() {
        when(exceptionProbability.test()).thenReturn(true);

        driver.addExpectation(onRequestTo("/foo"), giveEmptyResponse());

        final CompletableFuture<ClientHttpResponse> future = unit.get("/foo")
                .call(pass());

        final CompletionException exception = assertThrows(CompletionException.class, future::join);

        assertThat(exception.getCause(), anyOf(
                instanceOf(ConnectException.class),
                instanceOf(NoRouteToHostException.class)));
    }

    @Test
    void shouldNotInjectExceptionIfThrownAlready() {
        when(exceptionProbability.test()).thenReturn(true);

        driver.addExpectation(onRequestTo("/foo"), giveEmptyResponse().after(2, SECONDS));

        final CompletableFuture<ClientHttpResponse> future = unit.get("/foo")
                .call(pass());

        final CompletionException exception = assertThrows(CompletionException.class, future::join);
        final Throwable cause = exception.getCause();

        assertThat(cause, is(instanceOf(SocketTimeoutException.class)));
    }

    @Test
    void shouldInjectLatencyAndErrorResponse() throws IOException {
        when(latencyProbability.test()).thenReturn(true);
        when(errorResponseProbability.test()).thenReturn(true);

        driver.addExpectation(onRequestTo("/foo"), giveEmptyResponse());

        final Clock clock = Clock.systemUTC();
        final Instant start = clock.instant();

        final ClientHttpResponse response = unit.get("/foo")
                .call(pass())
                .join();

        final Instant end = clock.instant();

        assertThat(Duration.between(start, end), is(greaterThanOrEqualTo(Duration.ofSeconds(1))));
        assertThat(response.getRawStatusCode(), is(oneOf(500, 503)));
    }

}
