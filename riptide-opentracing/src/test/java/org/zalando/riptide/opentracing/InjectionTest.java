package org.zalando.riptide.opentracing;

import io.opentracing.contrib.concurrent.TracedExecutorService;
import io.opentracing.mock.MockTracer;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.zalando.riptide.Http;

import java.io.IOException;

import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.http.HttpStatus.OK;
import static org.zalando.riptide.PassRoute.pass;
import static org.zalando.riptide.opentracing.MockWebServerUtil.getBaseUrl;
import static org.zalando.riptide.opentracing.MockWebServerUtil.verify;

final class InjectionTest {

    private final MockWebServer server = new MockWebServer();
    private final MockTracer tracer = new MockTracer();

    private final Http unit = Http.builder()
            .executor(new TracedExecutorService(newSingleThreadExecutor(), tracer))
            .requestFactory(new HttpComponentsClientHttpRequestFactory())
            .baseUrl(getBaseUrl(server))
            .plugin(new OpenTracingPlugin(tracer)
                .withInjection(new NoOpInjection()))
            .build();

    @Test
    void shouldNotInject() {
        server.enqueue(new MockResponse().setResponseCode(OK.value()));

        unit.get("/users/{user}", "me")
                .call(pass())
                .join();

        assertThat(tracer.finishedSpans(), hasSize(1));
        verify(server, 1, "/users/me", headers -> {
            assertNull(headers.get("traceid"));
            assertNull(headers.get("spanid"));
        });
    }

    @AfterEach
    void tearDown() throws IOException {
        server.shutdown();
    }

}
