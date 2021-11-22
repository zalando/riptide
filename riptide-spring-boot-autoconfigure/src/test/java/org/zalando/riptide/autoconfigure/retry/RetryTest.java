package org.zalando.riptide.autoconfigure.retry;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.client.MockRestServiceServer;
import org.zalando.logbook.autoconfigure.LogbookAutoConfiguration;
import org.zalando.riptide.Http;
import org.zalando.riptide.autoconfigure.MetricsTestAutoConfiguration;
import org.zalando.riptide.autoconfigure.OpenTelemetryTestAutoConfiguration;
import org.zalando.riptide.autoconfigure.OpenTracingTestAutoConfiguration;
import org.zalando.riptide.autoconfigure.RiptideClientTest;

import java.util.concurrent.CompletionException;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.http.HttpStatus.Series.SERVER_ERROR;
import static org.springframework.test.web.client.ExpectedCount.times;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.zalando.riptide.Bindings.on;
import static org.zalando.riptide.Navigators.series;
import static org.zalando.riptide.failsafe.RetryRoute.retry;

@RiptideClientTest
@ActiveProfiles("default")
public class RetryTest {
    @Configuration
    @ImportAutoConfiguration({
            JacksonAutoConfiguration.class,
            LogbookAutoConfiguration.class,
            OpenTelemetryTestAutoConfiguration.class,
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
}
