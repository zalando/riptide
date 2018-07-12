package org.zalando.riptide.failsafe.metrics;

import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import net.jodah.failsafe.ExecutionContext;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.Listeners;
import net.jodah.failsafe.RetryPolicy;
import org.junit.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpResponse;
import org.zalando.riptide.RequestArguments;
import org.zalando.riptide.failsafe.RetryListener;

import java.net.URI;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static com.google.common.collect.Ordering.from;
import static java.util.Comparator.comparing;
import static org.junit.Assert.assertEquals;

public class MetricsRetryListenerTest {

    private final SimpleMeterRegistry registry = new SimpleMeterRegistry();
    private final RetryListener unit = new MetricsRetryListener(registry)
            .withMetricName("retries")
            .withDefaultTags(Tag.of("test", "true"));

    private final RetryPolicy policy = new RetryPolicy()
            .withMaxRetries(3);

    @Test
    public void shouldRecordRetries() {
        final AtomicInteger count = new AtomicInteger();

        Failsafe
                .with(policy)
                .with(new Listeners<ClientHttpResponse>() {
                    @Override
                    public void onRetry(final ClientHttpResponse result, final Throwable failure, final ExecutionContext context) {
                        final RequestArguments arguments = RequestArguments.create()
                                .withMethod(HttpMethod.GET)
                                .withRequestUri(URI.create("/"));
                        unit.onRetry(arguments, result, failure, context);
                    }
                })
                .run(() -> {
            if (count.incrementAndGet() < 3) {
                throw new IllegalStateException();
            }
        });

        final List<Timer> timers = timers();

        assertEquals(2, timers.size());

        final Timer first = timers.get(0);
        assertEquals("1", first.getId().getTag("retries"));
        assertEquals("true", first.getId().getTag("test"));

        final Timer second = timers.get(1);
        assertEquals("2", second.getId().getTag("retries"));
        assertEquals("true", second.getId().getTag("test"));
    }

    private List<Timer> timers() {
        return registry.find("retries").timers().stream()
                .sorted(comparing(this::tags, from(comparing(Tag::getKey)
                        .thenComparing(Tag::getValue)).lexicographical()))
                .collect(Collectors.toList());
    }

    private List<Tag> tags(final Timer timer) {
        return timer.getId().getTags();
    }

}
