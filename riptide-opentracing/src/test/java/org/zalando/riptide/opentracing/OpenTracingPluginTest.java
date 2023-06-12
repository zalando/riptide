package org.zalando.riptide.opentracing;

import io.opentracing.Scope;
import io.opentracing.contrib.concurrent.TracedExecutorService;
import io.opentracing.mock.MockSpan;
import io.opentracing.mock.MockSpan.LogEntry;
import io.opentracing.mock.MockTracer;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.core5.util.Timeout;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.zalando.riptide.Http;
import org.zalando.riptide.UnexpectedResponseException;
import org.zalando.riptide.opentracing.span.ErrorMessageSpanDecorator;
import org.zalando.riptide.opentracing.span.StaticSpanDecorator;
import org.zalando.riptide.opentracing.span.UriVariablesTagSpanDecorator;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import static java.util.Collections.singletonMap;
import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.http.HttpMethod.POST;
import static org.zalando.riptide.NoRoute.noRoute;
import static org.zalando.riptide.PassRoute.pass;
import static org.zalando.riptide.opentracing.MockWebServerUtil.*;

final class OpenTracingPluginTest {

    private final MockWebServer server = new MockWebServer();
    private final MockTracer tracer = new MockTracer();

    private final Http unit = Http.builder()
            .executor(new TracedExecutorService(newSingleThreadExecutor(), tracer))
            .requestFactory(new HttpComponentsClientHttpRequestFactory(HttpClientBuilder.create()
                    .setDefaultRequestConfig(RequestConfig.custom()
                            .setConnectTimeout(Timeout.ofMilliseconds(500))
                            .build())
                    .build()))
            .baseUrl(getBaseUrl(server))
            .plugin(new OpenTracingPlugin(tracer)
                    .withAdditionalSpanDecorators(
                            new ErrorMessageSpanDecorator(),
                            new StaticSpanDecorator(singletonMap("test.environment", "JUnit")),
                            new UriVariablesTagSpanDecorator()))
            .build();

    @Test
    void shouldTraceRequestAndResponse() {
        server.enqueue(textMockResponse("Hello world!")
                .setHeader("Retry-After", "60"));

        final MockSpan parent = tracer.buildSpan("test").start();

        try (final Scope ignored = tracer.activateSpan(parent)) {
            unit.post("/users/{user}", "me")
                    .attribute(OpenTracingPlugin.TAGS, singletonMap("test", "true"))
                    .attribute(OpenTracingPlugin.LOGS, singletonMap("retry_number", 1))
                    .header("HTTP-Method-Override", "GET")
                    .header("Prefer", "respond-async")
                    .call(pass())
                    .join();
        } finally {
            parent.finish();
        }

        final List<MockSpan> spans = tracer.finishedSpans();
        assertThat(spans, hasSize(2));

        assertThat(spans.get(1), is(parent));

        final MockSpan child = spans.get(0);
        assertThat(child.parentId(), is(parent.context().spanId()));

        assertThat(child.tags(), hasEntry("component", "Riptide"));
        assertThat(child.tags(), hasEntry("span.kind", "client"));
        assertThat(child.tags(), hasEntry("peer.address", "localhost:" + server.getPort()));
        assertThat(child.tags(), hasEntry("peer.hostname", "localhost"));
        assertThat(child.tags(), hasEntry("peer.port", server.getPort()));
        assertThat(child.tags(), hasEntry("http.method", "POST"));
        assertThat(child.tags(), hasEntry("http.method_override", "GET"));
        assertThat(child.tags(), hasEntry("http.path", "/users/{user}"));
        assertThat(child.tags(), hasEntry("http.prefer", "respond-async"));
        assertThat(child.tags(), hasEntry("http.status_code", 200));
        assertThat(child.tags(), hasEntry("test", "true"));
        assertThat(child.tags(), hasEntry("test.environment", "JUnit"));
        assertThat(child.tags(), hasEntry("user", "me"));
        assertThat(child.tags(), hasEntry("spi", true));

        // not active by default
        assertThat(child.tags(), not(hasKey("http.url")));

        final List<LogEntry> entries = child.logEntries();
        assertThat(entries, hasSize(2));

        {
            final LogEntry log = entries.get(0);
            assertThat(log.fields(), hasEntry("retry_number", 1));
        }

        {
            final LogEntry log = entries.get(1);
            assertThat(log.fields(), hasEntry("http.retry_after", "60"));
        }

        verify(server, 1, "/users/me", POST.toString(), headers -> {
            assertNotNull(headers.get("traceid"));
            assertNotNull(headers.get("spanid"));
        });
    }

    @Test
    void shouldTraceRequestAndServerError() {
        server.enqueue(new MockResponse().setResponseCode(500));

        final MockSpan parent = tracer.buildSpan("test").start();

        try (final Scope ignored = tracer.activateSpan(parent)) {
            final CompletableFuture<ClientHttpResponse> future = unit.get(URI.create(getBaseUrl(server)))
                    .attribute(OpenTracingPlugin.TAGS, singletonMap("test", "true"))
                    .attribute(OpenTracingPlugin.LOGS, singletonMap("retry_number", 2))
                    .call(noRoute());

            final CompletionException error = assertThrows(CompletionException.class, future::join);
            assertThat(error.getCause(), is(instanceOf(UnexpectedResponseException.class)));
        } finally {
            parent.finish();
        }

        final List<MockSpan> spans = tracer.finishedSpans();
        assertThat(spans, hasSize(2));

        assertThat(spans.get(1), is(parent));

        final MockSpan child = spans.get(0);
        assertThat(child.parentId(), is(parent.context().spanId()));

        assertThat(child.tags(), hasEntry("component", "Riptide"));
        assertThat(child.tags(), hasEntry("span.kind", "client"));
        assertThat(child.tags(), hasEntry("peer.address", "localhost:" + server.getPort()));
        assertThat(child.tags(), hasEntry("peer.hostname", "localhost"));
        assertThat(child.tags(), hasEntry("peer.port", server.getPort()));
        assertThat(child.tags(), hasEntry("http.method", "GET"));
        assertThat(child.tags(), hasEntry("http.status_code", 500));
        assertThat(child.tags(), hasEntry("error", true));
        assertThat(child.tags(), hasEntry("test", "true"));
        assertThat(child.tags(), hasEntry("test.environment", "JUnit"));
        assertThat(child.tags(), hasEntry("spi", true));

        // since we didn't use a uri template
        assertThat(child.tags(), not(hasKey("http.path")));

        final List<LogEntry> logs = child.logEntries();
        assertThat(logs, hasSize(1));
        final LogEntry log = logs.get(0);
        assertThat(log.fields(), hasEntry("retry_number", 2));

        verify(server, 1, "/");
    }

    @Test
    void shouldTraceRequestAndNetworkError() {
        server.enqueue(emptyMockResponse().setHeadersDelay(1, SECONDS));

        final MockSpan parent = tracer.buildSpan("test").start();

        try (final Scope ignored = tracer.activateSpan(parent)) {
            final CompletableFuture<ClientHttpResponse> future = unit.get(URI.create(getBaseUrl(server)))
                    .call(noRoute());

            final CompletionException error = assertThrows(CompletionException.class, future::join);
            assertThat(error.getCause(), is(instanceOf(SocketTimeoutException.class)));
        } finally {
            parent.finish();
        }

        final List<MockSpan> spans = tracer.finishedSpans();
        assertThat(spans, hasSize(2));

        assertThat(spans.get(1), is(parent));

        final MockSpan child = spans.get(0);
        assertThat(child.parentId(), is(parent.context().spanId()));

        assertThat(child.tags(), hasEntry("component", "Riptide"));
        assertThat(child.tags(), hasEntry("span.kind", "client"));
        assertThat(child.tags(), hasEntry("peer.address", "localhost:" + server.getPort()));
        assertThat(child.tags(), hasEntry("peer.hostname", "localhost"));
        assertThat(child.tags(), hasEntry("peer.port", server.getPort()));
        assertThat(child.tags(), hasEntry("http.method", "GET"));
        assertThat(child.tags(), hasEntry("error", true));

        // since we didn't use a uri template
        assertThat(child.tags(), not(hasKey("http.path")));

        // since we didn't get any response
        assertThat(child.tags(), not(hasKey("http.status_code")));

        final List<LogEntry> logs = child.logEntries();
        assertThat(logs, hasSize(3));

        for (int i = 0; i < logs.size(); i++) {
            final LogEntry log = logs.get(i);

            switch (i) {
                case 0: {
                    assertThat(log.fields(), hasEntry("error.kind", "SocketTimeoutException"));
                    assertThat(log.fields(), hasEntry(is("error.object"), is(instanceOf(SocketTimeoutException.class))));
                    break;
                }
                case 1: {
                    assertThat(log.fields().get("stack").toString(),
                            containsString("java.net.SocketTimeoutException: Read timed out"));
                    break;
                }
                case 2: {
                    assertThat(log.fields(), hasEntry("message", "Read timed out"));
                    break;
                }
                default: {
                    throw new AssertionError();
                }
            }
        }

        verify(server, 1, "/");
    }

    @Test
    void shouldTraceRequestAndIgnoreClientError() {
        server.enqueue(new MockResponse().setResponseCode(400));

        final MockSpan parent = tracer.buildSpan("test").start();

        try (final Scope ignored = tracer.activateSpan(parent)) {
            final CompletableFuture<ClientHttpResponse> future = unit.get(URI.create(getBaseUrl(server)))
                    .call(noRoute());

            final CompletionException error = assertThrows(CompletionException.class, future::join);
            assertThat(error.getCause(), is(instanceOf(UnexpectedResponseException.class)));
        } finally {
            parent.finish();
        }

        final List<MockSpan> spans = tracer.finishedSpans();
        assertThat(spans, hasSize(2));

        assertThat(spans.get(1), is(parent));

        final MockSpan child = spans.get(0);
        assertThat(child.parentId(), is(parent.context().spanId()));

        assertThat(child.tags(), hasEntry("component", "Riptide"));
        assertThat(child.tags(), hasEntry("span.kind", "client"));
        assertThat(child.tags(), hasEntry("peer.address", "localhost:" + server.getPort()));
        assertThat(child.tags(), hasEntry("peer.hostname", "localhost"));
        assertThat(child.tags(), hasEntry("peer.port", server.getPort()));
        assertThat(child.tags(), hasEntry("http.method", "GET"));
        assertThat(child.tags(), hasEntry("http.status_code", 400));

        // since we didn't use a uri template
        assertThat(child.tags(), not(hasKey("error")));

        verify(server, 1, "/");
    }

    @AfterEach
    void tearDown() throws IOException {
        server.shutdown();
    }

}
