package org.zalando.riptide.opentracing;

import com.github.restdriver.clientdriver.*;
import io.opentracing.*;
import io.opentracing.contrib.concurrent.*;
import io.opentracing.mock.*;
import io.opentracing.mock.MockSpan.*;
import org.apache.http.client.config.*;
import org.apache.http.impl.client.*;
import org.junit.jupiter.api.*;
import org.springframework.http.client.*;
import org.zalando.riptide.*;
import org.zalando.riptide.opentracing.span.*;

import java.net.*;
import java.util.*;
import java.util.concurrent.*;

import static com.github.restdriver.clientdriver.RestClientDriver.*;
import static com.google.common.collect.Iterables.*;
import static java.util.Collections.*;
import static java.util.concurrent.Executors.*;
import static java.util.concurrent.TimeUnit.*;
import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.zalando.riptide.NoRoute.*;
import static org.zalando.riptide.PassRoute.*;

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
                        .withHeader("traceid", notNullValue(String.class))
                        .withHeader("spanid", notNullValue(String.class)),
                giveEmptyResponse().withStatus(200));

        final MockSpan parent = tracer.buildSpan("test").start();

        try (final Scope ignored = tracer.activateSpan(parent)) {
            unit.get("/users/{user}", "me")
                    .attribute(OpenTracingPlugin.TAGS, singletonMap("test", "true"))
                    .attribute(OpenTracingPlugin.LOGS, singletonMap("retry_number", 1))
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
        assertThat(child.tags(), hasEntry("http.method", "GET"));
        assertThat(child.tags(), hasEntry("http.path", "/users/{user}"));
        assertThat(child.tags(), hasEntry("http.status_code", 200));
        assertThat(child.tags(), hasEntry("test", "true"));
        assertThat(child.tags(), hasEntry("test.environment", "JUnit"));
        assertThat(child.tags(), hasEntry("user", "me"));
        assertThat(child.tags(), hasEntry("spi", true));

        // not active by default
        assertThat(child.tags(), not(hasKey("http.url")));

        final LogEntry log = getOnlyElement(child.logEntries());

        assertThat(log.fields(), hasEntry("retry_number", 1));
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

        final LogEntry log = getOnlyElement(child.logEntries());
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

        assertThat(child.logEntries(), is(empty()));
    }

    @AfterEach
    void tearDown() {
        driver.verify();
        driver.shutdown();
    }

}
