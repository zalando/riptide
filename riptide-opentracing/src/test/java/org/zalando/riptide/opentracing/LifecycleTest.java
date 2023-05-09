package org.zalando.riptide.opentracing;

import io.opentracing.Scope;
import io.opentracing.contrib.concurrent.TracedExecutorService;
import io.opentracing.mock.MockSpan;
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
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpStatus.OK;
import static org.zalando.riptide.PassRoute.pass;
import static org.zalando.riptide.opentracing.MockWebServerUtil.getBaseUrl;
import static org.zalando.riptide.opentracing.MockWebServerUtil.verify;

final class LifecycleTest {

    private final MockWebServer server = new MockWebServer();
    private final MockTracer tracer = new MockTracer();

    private final Http unit = Http.builder()
            .executor(new TracedExecutorService(newSingleThreadExecutor(), tracer))
            .requestFactory(new HttpComponentsClientHttpRequestFactory())
            .baseUrl(getBaseUrl(server))
            .plugin(new OpenTracingPlugin(tracer)
                .withLifecycle(Lifecycle.composite(
                        new ExplicitSpanLifecycle(),
                        new ActiveSpanLifecycle()
                )))
            .build();

    @Test
    void shouldFallbackToNoOp() {
        server.enqueue(new MockResponse().setResponseCode(OK.value()));

        unit.get("/users/{user}", "me")
                    .call(pass())
                    .join();

        assertThat(tracer.finishedSpans(), is(empty()));

        verify(server, 1, "/users/me", GET.toString(), headers -> {
            assertNull(headers.get("traceid"));
            assertNull(headers.get("spanid"));
        });
    }

    @Test
    void shouldUseExplicitSpan() {
        server.enqueue(new MockResponse().setResponseCode(OK.value()));

        final MockSpan span = tracer.buildSpan("test").start();

        unit.get("/users/{user}", "me")
                .attribute(OpenTracingPlugin.SPAN, span)
                .call(pass())
                .join();

        span.finish();

        assertThat(tracer.finishedSpans(), contains(span));

        verify(server, 1, "/users/me", GET.toString(), headers -> {
            assertNotNull(headers.get("traceid"));
            assertNotNull(headers.get("spanid"));
        });
    }

    @Test
    void shouldUseActiveSpan() {
        server.enqueue(new MockResponse().setResponseCode(OK.value()));

        final MockSpan span = tracer.buildSpan("test").start();

        try (final Scope ignored = tracer.activateSpan(span)) {
            unit.get("/users/{user}", "me")
                    .call(pass())
                    .join();
        } finally {
            span.finish();
        }

        assertThat(tracer.finishedSpans(), contains(span));

        verify(server, 1, "/users/me", GET.toString(), headers -> {
            assertNotNull(headers.get("traceid"));
            assertNotNull(headers.get("spanid"));
        });
    }

    @AfterEach
    void tearDown() throws IOException {
        server.shutdown();
    }

}
