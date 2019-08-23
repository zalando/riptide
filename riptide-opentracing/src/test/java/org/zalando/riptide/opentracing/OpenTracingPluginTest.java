package org.zalando.riptide.opentracing;

import com.github.restdriver.clientdriver.ClientDriver;
import com.github.restdriver.clientdriver.ClientDriverFactory;
import io.opentracing.Scope;
import io.opentracing.contrib.concurrent.TracedExecutorService;
import io.opentracing.mock.MockSpan;
import io.opentracing.mock.MockSpan.LogEntry;
import io.opentracing.mock.MockTracer;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.HttpClientBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.zalando.riptide.Http;
import org.zalando.riptide.UnexpectedResponseException;
import org.zalando.riptide.opentracing.span.ErrorMessageSpanDecorator;
import org.zalando.riptide.opentracing.span.StaticSpanDecorator;
import org.zalando.riptide.opentracing.span.UriVariablesTagSpanDecorator;

import java.net.SocketTimeoutException;
import java.net.URI;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import static com.github.restdriver.clientdriver.ClientDriverRequest.Method.POST;
import static com.github.restdriver.clientdriver.RestClientDriver.giveEmptyResponse;
import static com.github.restdriver.clientdriver.RestClientDriver.giveResponse;
import static com.github.restdriver.clientdriver.RestClientDriver.onRequestTo;
import static com.google.common.collect.Iterables.getOnlyElement;
import static java.util.Collections.singletonMap;
import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.zalando.riptide.NoRoute.noRoute;
import static org.zalando.riptide.PassRoute.pass;

final class OpenTracingPluginTest {

    private final ClientDriver driver = new ClientDriverFactory().createClientDriver();
    private final MockTracer tracer = new MockTracer();

    private final Http unit = Http.builder()
            .executor(new TracedExecutorService(newSingleThreadExecutor(), tracer))
            .requestFactory(new HttpComponentsClientHttpRequestFactory(HttpClientBuilder.create()
                    .setDefaultRequestConfig(RequestConfig.custom()
                            .setSocketTimeout(500)
                            .build())
                    .build()))
            .baseUrl(driver.getBaseUrl())
            .plugin(new OpenTracingPlugin(tracer)
                    .withAdditionalSpanDecorators(
                            new ErrorMessageSpanDecorator(),
                            new StaticSpanDecorator(singletonMap("test.environment", "JUnit")),
                            new UriVariablesTagSpanDecorator()))
            .build();

    @Test
    void shouldTraceRequestAndResponse() {
        driver.addExpectation(onRequestTo("/users/me")
                        .withMethod(POST)
                        .withHeader("traceid", notNullValue(String.class))
                        .withHeader("spanid", notNullValue(String.class)),
                giveResponse("Hello, world!", "text/plain")
                        .withStatus(200)
                        .withHeader("Content-Language", "en")
                        .withHeader("RateLimit-Limit", "10")
                        .withHeader("RateLimit-Remaining", "0")
                        .withHeader("RateLimit-Reset", "60")
                        .withHeader("Retry-After", "60")
                        .withHeader("Warning", "110 - \"Response is Stale\""));

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
        assertThat(child.tags(), hasEntry("peer.address", "localhost:" + driver.getPort()));
        assertThat(child.tags(), hasEntry("peer.hostname", "localhost"));
        assertThat(child.tags(), hasEntry("peer.port", driver.getPort()));
        assertThat(child.tags(), hasEntry("http.content_language", "en"));
        assertThat(child.tags(), hasEntry("http.method", "POST"));
        assertThat(child.tags(), hasEntry("http.method_override", "GET"));
        assertThat(child.tags(), hasEntry("http.path", "/users/{user}"));
        assertThat(child.tags(), hasEntry("http.prefer", "respond-async"));
        assertThat(child.tags(), hasEntry("http.status_code", 200));
        assertThat(child.tags(), hasEntry("http.warning", "110 - \"Response is Stale\""));
        assertThat(child.tags(), hasEntry("test", "true"));
        assertThat(child.tags(), hasEntry("test.environment", "JUnit"));
        assertThat(child.tags(), hasEntry("user", "me"));
        assertThat(child.tags(), hasEntry("spi", true));

        // not active by default
        assertThat(child.tags(), not(hasKey("http.url")));

        final List<LogEntry> entries = child.logEntries();
        assertThat(entries, hasSize(4));

        {
            final LogEntry log = entries.get(0);
            assertThat(log.fields(), hasEntry("retry_number", 1));
        }

        {
            final LogEntry log = entries.get(1);
            assertThat(log.fields(), hasEntry("http.content_length", "13"));
        }

        {
            final LogEntry log = entries.get(2);
            assertThat(log.fields(), hasEntry("http.retry_after", "60"));
        }

        {
            final LogEntry log = entries.get(3);
            assertThat(log.fields(), hasEntry("rate_limit.limit", "10"));
            assertThat(log.fields(), hasEntry("rate_limit.remaining", "0"));
            assertThat(log.fields(), hasEntry("rate_limit.reset", "60"));
        }
    }

    @Test
    void shouldTraceRequestAndServerError() {
        driver.addExpectation(onRequestTo("/"), giveEmptyResponse().withStatus(500));

        final MockSpan parent = tracer.buildSpan("test").start();

        try (final Scope ignored = tracer.activateSpan(parent)) {
            final CompletableFuture<ClientHttpResponse> future = unit.get(URI.create(driver.getBaseUrl()))
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
        assertThat(child.tags(), hasEntry("peer.address", "localhost:" + driver.getPort()));
        assertThat(child.tags(), hasEntry("peer.hostname", "localhost"));
        assertThat(child.tags(), hasEntry("peer.port", driver.getPort()));
        assertThat(child.tags(), hasEntry("http.method", "GET"));
        assertThat(child.tags(), hasEntry("http.status_code", 500));
        assertThat(child.tags(), hasEntry("error", true));
        assertThat(child.tags(), hasEntry("test", "true"));
        assertThat(child.tags(), hasEntry("test.environment", "JUnit"));
        assertThat(child.tags(), hasEntry("spi", true));

        // since we didn't use a uri template
        assertThat(child.tags(), not(hasKey("http.path")));

        final List<LogEntry> logs = child.logEntries();
        assertThat(logs, hasSize(2)); // http.content_length + retry_number
        final LogEntry log = logs.get(0);
        assertThat(log.fields(), hasEntry("retry_number", 2));
    }

    @Test
    void shouldTraceRequestAndNetworkError() {
        driver.addExpectation(onRequestTo("/"), giveEmptyResponse().after(1, SECONDS));

        final MockSpan parent = tracer.buildSpan("test").start();

        try (final Scope ignored = tracer.activateSpan(parent)) {
            final CompletableFuture<ClientHttpResponse> future = unit.get(URI.create(driver.getBaseUrl()))
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
        assertThat(child.tags(), hasEntry("peer.address", "localhost:" + driver.getPort()));
        assertThat(child.tags(), hasEntry("peer.hostname", "localhost"));
        assertThat(child.tags(), hasEntry("peer.port", driver.getPort()));
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
    }

    @Test
    void shouldTraceRequestAndIgnoreClientError() {
        driver.addExpectation(onRequestTo("/"), giveEmptyResponse().withStatus(400));

        final MockSpan parent = tracer.buildSpan("test").start();

        try (final Scope ignored = tracer.activateSpan(parent)) {
            final CompletableFuture<ClientHttpResponse> future = unit.get(URI.create(driver.getBaseUrl()))
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
        assertThat(child.tags(), hasEntry("peer.address", "localhost:" + driver.getPort()));
        assertThat(child.tags(), hasEntry("peer.hostname", "localhost"));
        assertThat(child.tags(), hasEntry("peer.port", driver.getPort()));
        assertThat(child.tags(), hasEntry("http.method", "GET"));
        assertThat(child.tags(), hasEntry("http.status_code", 400));

        // since we didn't use a uri template
        assertThat(child.tags(), not(hasKey("error")));
    }

    @AfterEach
    void tearDown() {
        driver.verify();
        driver.shutdown();
    }

}
