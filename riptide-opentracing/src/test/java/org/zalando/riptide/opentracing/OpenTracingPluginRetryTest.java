package org.zalando.riptide.opentracing;

import com.github.restdriver.clientdriver.*;
import com.google.common.collect.*;
import io.opentracing.contrib.concurrent.*;
import io.opentracing.mock.*;
import io.opentracing.mock.MockSpan.*;
import net.jodah.failsafe.*;
import org.junit.jupiter.api.*;
import org.springframework.http.client.*;
import org.zalando.riptide.*;
import org.zalando.riptide.failsafe.*;
import org.zalando.riptide.opentracing.span.*;

import java.util.*;
import java.util.function.*;

import static com.github.restdriver.clientdriver.RestClientDriver.*;
import static com.google.common.collect.Iterables.*;
import static java.util.Collections.*;
import static java.util.concurrent.Executors.*;
import static java.util.stream.Collectors.*;
import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;
import static org.zalando.riptide.PassRoute.*;

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
                            .withMaxRetries(2)
                            .handleResultIf(response -> true)),
                    new TracedScheduledExecutorService(newSingleThreadScheduledExecutor(), tracer)))
            .plugin(unit)
            .build();

    @Test
    void shouldTagRetries() {
        driver.addExpectation(onRequestTo("/"), giveEmptyResponse().withStatus(200));
        driver.addExpectation(onRequestTo("/"), giveEmptyResponse().withStatus(200));
        driver.addExpectation(onRequestTo("/"), giveEmptyResponse().withStatus(200));

        http.get("/").call(pass()).join();

        final List<MockSpan> spans = tracer.finishedSpans();

        assertThat(spans, hasSize(4));

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

        assertThat(children, hasSize(3));

        children.forEach(child ->
                assertThat(child.tags(), hasKey("http.status_code")));

        final List<MockSpan> retries = spans.stream()
                .filter(span -> span.tags().containsKey("retry"))
                .collect(toList());

        assertThat(retries, hasSize(2));

        forEachWithIndex(retries, (retry, index) -> {
            final LogEntry log = getOnlyElement(retry.logEntries());
            assertThat(log.fields(), is(singletonMap("retry_number", index + 1)));
        });
    }

    private static <T> void forEachWithIndex(final Collection<T> collection, final ObjIntConsumer<T> consumer) {
        int index = 0;
        for (final T element : collection) {
            consumer.accept(element, index++);
        }
    }

    @AfterEach
    void tearDown() {
        driver.verify();
        driver.shutdown();
    }

}
