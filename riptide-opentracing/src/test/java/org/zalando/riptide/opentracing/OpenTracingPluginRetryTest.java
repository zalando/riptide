package org.zalando.riptide.opentracing;

import dev.failsafe.RetryPolicy;
import io.opentracing.Scope;
import io.opentracing.contrib.concurrent.TracedExecutorService;
import io.opentracing.mock.MockSpan;
import io.opentracing.mock.MockTracer;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.zalando.riptide.Http;
import org.zalando.riptide.Plugin;
import org.zalando.riptide.failsafe.FailsafePlugin;
import org.zalando.riptide.opentracing.span.HttpUrlSpanDecorator;
import org.zalando.riptide.opentracing.span.RetrySpanDecorator;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.function.ObjIntConsumer;

import static com.google.common.collect.Iterables.getOnlyElement;
import static java.util.Comparator.comparing;
import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.springframework.http.HttpStatus.OK;
import static org.zalando.riptide.PassRoute.pass;
import static org.zalando.riptide.opentracing.MockWebServerUtil.getBaseUrl;
import static org.zalando.riptide.opentracing.MockWebServerUtil.verify;

final class OpenTracingPluginRetryTest {

    private final MockWebServer server = new MockWebServer();
    private final MockTracer tracer = new MockTracer();

    private final Plugin unit = new OpenTracingPlugin(tracer)
            .withAdditionalSpanDecorators(new HttpUrlSpanDecorator())
            .withAdditionalSpanDecorators(new RetrySpanDecorator());

    private final Http http = Http.builder()
            .executor(new TracedExecutorService(newSingleThreadExecutor(), tracer))
            .requestFactory(new HttpComponentsClientHttpRequestFactory())
            .baseUrl(getBaseUrl(server))
            .plugin(unit)
            .plugin(new FailsafePlugin()
                    .withPolicy(RetryPolicy.<ClientHttpResponse>builder()
                            .withMaxRetries(2)
                            .handleResultIf(response -> true)
                            .build())
                    .withDecorator(new TracedTaskDecorator(tracer)))
            .build();

    @Test
    void shouldTagRetries() {
        server.enqueue(new MockResponse().setResponseCode(OK.value()));
        server.enqueue(new MockResponse().setResponseCode(OK.value()));
        server.enqueue(new MockResponse().setResponseCode(OK.value()));

        final MockSpan parent = tracer.buildSpan("test").start();
        try (final Scope ignored = tracer.activateSpan(parent)) {
            http.get("/").call(pass()).join();
        }
        parent.finish();

        final List<MockSpan> spans = tracer.finishedSpans();

        assertThat(spans, hasSize(4));

        spans.forEach(span ->
                assertThat(span.generatedErrors(), is(empty())));

        final List<MockSpan> roots = spans.stream()
                .filter(span -> span.parentId() == 0)
                .collect(toList());

        assertThat(roots, hasSize(1));
        final MockSpan root = getOnlyElement(roots);

        assertThat(root.parentId(), is(0L));
        assertThat(root.tags(), not(hasKey("retry")));

        final Collection<MockSpan> leafs = spans.stream()
                .filter(span -> span.parentId() != 0)
                .collect(toList());

        assertThat(leafs, hasSize(3));

        leafs.forEach(span -> {
            assertThat(span.tags(), hasKey("http.url"));
            assertThat(span.tags(), hasKey("http.status_code"));
        });

        final List<MockSpan> requests = leafs.stream()
                .filter(span -> !span.tags().containsKey("retry"))
                .collect(toList());

        assertThat(requests, hasSize(1));
        final MockSpan request = getOnlyElement(requests);
        assertThat(request.tags(), not(hasKey("retry")));

        final List<MockSpan> retries = leafs.stream()
                .filter(span -> span.tags().containsKey("retry"))
                .sorted(comparing(MockSpan::startMicros))
                .collect(toList());

        assertThat(retries, hasSize(2));

        forEachWithIndex(retries, (span, index) -> {
            assertThat(span.tags(), hasKey("retry"));
            assertThat(span.logEntries().get(0).fields(),
                    hasEntry("retry_number", index + 1));
        });

        verify(server, 3, "/");
    }

    private static <T> void forEachWithIndex(
            final Iterable<T> collection,
            final ObjIntConsumer<T> consumer) {

        int index = 0;
        for (final T element : collection) {
            consumer.accept(element, index++);
        }
    }

    @AfterEach
    void tearDown() throws IOException {
        server.shutdown();
    }

}
