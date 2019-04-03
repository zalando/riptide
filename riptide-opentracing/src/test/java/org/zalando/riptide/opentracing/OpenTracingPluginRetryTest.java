package org.zalando.riptide.opentracing;

import com.github.restdriver.clientdriver.ClientDriver;
import com.github.restdriver.clientdriver.ClientDriverFactory;
import com.google.common.collect.ImmutableList;
import io.opentracing.contrib.concurrent.TracedExecutorService;
import io.opentracing.contrib.concurrent.TracedScheduledExecutorService;
import io.opentracing.mock.MockSpan;
import io.opentracing.mock.MockTracer;
import net.jodah.failsafe.RetryPolicy;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.zalando.riptide.Http;
import org.zalando.riptide.Plugin;
import org.zalando.riptide.failsafe.FailsafePlugin;
import org.zalando.riptide.opentracing.span.HttpUrlSpanDecorator;
import org.zalando.riptide.opentracing.span.RetrySpanDecorator;

import java.util.List;

import static com.github.restdriver.clientdriver.RestClientDriver.giveEmptyResponse;
import static com.github.restdriver.clientdriver.RestClientDriver.onRequestTo;
import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.zalando.riptide.PassRoute.pass;

final class OpenTracingPluginRetryTest {

    private final ClientDriver driver = new ClientDriverFactory().createClientDriver();
    private final MockTracer tracer = new MockTracer();

    private final Plugin unit = new OpenTracingPlugin(tracer)
            .withAdditionalSpanDecorators(new HttpUrlSpanDecorator())
            .withAdditionalSpanDecorators(new RetrySpanDecorator());

    private final Http http = Http.builder()
            .executor(new TracedExecutorService(newSingleThreadExecutor(), tracer))
            .requestFactory(new HttpComponentsClientHttpRequestFactory())
            .baseUrl(driver.getBaseUrl())
            .plugin(unit)
            .plugin(new FailsafePlugin(
                    ImmutableList.of(new RetryPolicy<ClientHttpResponse>()
                            .withMaxRetries(1)
                            .handleResultIf(response -> true)),
                    new TracedScheduledExecutorService(newSingleThreadScheduledExecutor(), tracer)))
            .plugin(unit)
            .build();

    @Test
    void shouldTagRetries() {
        driver.addExpectation(onRequestTo("/"), giveEmptyResponse().withStatus(200));
        driver.addExpectation(onRequestTo("/"), giveEmptyResponse().withStatus(200));

        http.get("/").call(pass()).join();

        final List<MockSpan> spans = tracer.finishedSpans();

        assertThat(spans, hasSize(3));

        spans.forEach(span -> {
            assertThat(span.generatedErrors(), is(empty()));
            assertThat(span.tags(), hasKey("http.url"));
        });

        final List<MockSpan> roots = spans.stream()
                .filter(span -> span.parentId() == 0)
                .collect(toList());

        assertThat(roots, hasSize(1));

        roots.forEach(root ->
                assertThat(root.tags(), not(hasKey("http.status_code"))));

        final List<MockSpan> children = spans.stream()
                .filter(span -> span.parentId() > 0)
                .collect(toList());

        assertThat(children, hasSize(2));

        children.forEach(child ->
                assertThat(child.tags(), hasKey("http.status_code")));

        final List<MockSpan> retries = spans.stream()
                .filter(span -> span.tags().containsKey("retry"))
                .collect(toList());

        assertThat(retries, hasSize(1));
    }

    @AfterEach
    void tearDown() {
        driver.verify();
        driver.shutdown();
    }

}
