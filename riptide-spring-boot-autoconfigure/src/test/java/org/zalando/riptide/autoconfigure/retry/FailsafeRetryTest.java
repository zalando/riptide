package org.zalando.riptide.autoconfigure.retry;

import com.google.common.base.Stopwatch;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.client.MockRestServiceServer;
import org.zalando.logbook.autoconfigure.LogbookAutoConfiguration;
import org.zalando.riptide.Http;
import org.zalando.riptide.autoconfigure.MetricsTestAutoConfiguration;
import org.zalando.riptide.autoconfigure.OpenTracingTestAutoConfiguration;
import org.zalando.riptide.autoconfigure.RiptideClientTest;
import org.zalando.riptide.failsafe.RetryException;

import java.time.Duration;
import java.util.concurrent.CompletionException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeout;
import static org.springframework.http.HttpStatus.Series.CLIENT_ERROR;
import static org.springframework.http.HttpStatus.Series.SERVER_ERROR;
import static org.springframework.http.HttpStatus.Series.SUCCESSFUL;
import static org.springframework.test.web.client.ExpectedCount.times;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withBadRequest;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withTooManyRequests;
import static org.zalando.riptide.Bindings.anySeries;
import static org.zalando.riptide.Bindings.on;
import static org.zalando.riptide.Navigators.series;
import static org.zalando.riptide.Navigators.status;
import static org.zalando.riptide.PassRoute.pass;
import static org.zalando.riptide.failsafe.RetryRoute.retry;

@RiptideClientTest
@ActiveProfiles("default")
public class FailsafeRetryTest {
    @Configuration
    @ImportAutoConfiguration({
            JacksonAutoConfiguration.class,
            LogbookAutoConfiguration.class,
            OpenTracingTestAutoConfiguration.class,
            MetricsTestAutoConfiguration.class,
    })
    static class ContextConfiguration {
    }

    @Autowired
    @Qualifier("retry-test")
    private Http retryClient;

    @Autowired
    private MockRestServiceServer server;

    @Test
    void shouldRetryForAtMostMaxRetriesTimes() {
        server.expect(times(3), requestTo("http://retry-test")).andRespond(withServerError());

        assertThrows(CompletionException.class,
                     () -> retryClient.get().dispatch(series(), on(SERVER_ERROR).call(retry())).join());

        server.verify();
    }

    @Test
    void shouldRetryForCustomRetryException() {
        server.expect(times(3), requestTo("http://retry-test")).andRespond(withBadRequest());


        assertThrows(CompletionException.class,
                () -> retryClient.get().dispatch(series(), on(CLIENT_ERROR).call(response -> {
                    throw new RetryException(response);
                })).join());

        server.verify();
    }

    @Test
    void shouldRetryWithDelayEpochSeconds() {
        server.expect(times(1), requestTo("http://retry-test"))
                .andRespond(withTooManyRequests().header("X-RateLimit-Reset", "2"));
        server.expect(times(1), requestTo("http://retry-test")).andRespond(withSuccess());

        assertTimeout(Duration.ofMillis(3000), () -> {
            atLeast(Duration.ofSeconds(2), () -> retryClient.get()
                    .dispatch(series(),
                            on(SUCCESSFUL).call(pass()),
                            anySeries().dispatch(status(),
                                    on(HttpStatus.TOO_MANY_REQUESTS).call(retry())))
                    .join());
        });

        server.verify();
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
