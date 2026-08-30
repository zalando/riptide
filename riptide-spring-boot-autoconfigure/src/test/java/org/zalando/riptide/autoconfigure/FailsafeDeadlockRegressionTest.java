package org.zalando.riptide.autoconfigure;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.zalando.logbook.autoconfigure.LogbookAutoConfiguration;
import org.zalando.riptide.Http;

import java.io.IOException;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.zalando.riptide.PassRoute.pass;

@RiptideClientTest
@ActiveProfiles("default")
@TestPropertySource(properties = {
        "riptide.clients.deadlock-test.base-url=http://localhost",
        "riptide.clients.deadlock-test.retry.enabled=true",
        "riptide.clients.deadlock-test.retry.max-retries=2",
        "riptide.clients.deadlock-test.retry.fixed-delay=100 milliseconds",
        "riptide.clients.deadlock-test.timeouts.enabled=true",
        "riptide.clients.deadlock-test.timeouts.global=500 milliseconds",
        "riptide.clients.deadlock-test.transient-fault-detection.enabled=true"
})
final class FailsafeDeadlockRegressionTest {

    @Configuration
    @ImportAutoConfiguration({
            JacksonAutoConfiguration.class,
            LogbookAutoConfiguration.class,
            OpenTracingTestAutoConfiguration.class,
            MetricsTestAutoConfiguration.class,
    })
    static class ContextConfiguration {
    }

    private MockWebServer server;

    @Autowired
    @Qualifier("deadlock-test")
    private Http unit;

    @BeforeEach
    void setUp() throws IOException {
        server = new MockWebServer();
        server.start();
    }

    @AfterEach
    void tearDown() throws IOException {
        server.shutdown();
    }

    // Guards against issue #1859: Proves that the real Spring auto-configured Http client
    // containing both Retry and Timeout policies does not deadlock when a timeout occurs.
    @Test
    @org.junit.jupiter.api.Timeout(value = 10, unit = TimeUnit.SECONDS)
    void shouldNotDeadlock() {
        server.enqueue(new MockResponse().setHeadersDelay(2, SECONDS));
        server.enqueue(new MockResponse().setHeadersDelay(2, SECONDS));
        server.enqueue(new MockResponse().setHeadersDelay(2, SECONDS));

        assertThrows(CompletionException.class, () ->
                unit.get(server.url("/foo").toString())
                        .call(pass())
                        .join());
    }
}
