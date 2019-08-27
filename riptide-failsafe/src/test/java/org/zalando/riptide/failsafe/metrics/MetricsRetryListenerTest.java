package org.zalando.riptide.failsafe.metrics;

import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.*;
import net.jodah.failsafe.*;
import org.junit.jupiter.api.*;
import org.springframework.http.*;
import org.springframework.http.client.*;
import org.zalando.riptide.*;
import org.zalando.riptide.failsafe.*;

import java.net.*;
import java.util.*;
import java.util.concurrent.atomic.*;
import java.util.stream.*;

import static com.google.common.collect.Ordering.*;
import static java.util.Comparator.*;
import static org.junit.jupiter.api.Assertions.*;

final class MetricsRetryListenerTest {

    private final SimpleMeterRegistry registry = new SimpleMeterRegistry();
    private final RetryListener unit = new MetricsRetryListener(registry)
            .withMetricName("retries")
            .withDefaultTags(Tag.of("test", "true"));

    private final RetryPolicy<ClientHttpResponse> policy = new RetryPolicy<ClientHttpResponse>()
            .withMaxRetries(3);

    @Test
    void shouldRecordRetries() {
        final AtomicInteger count = new AtomicInteger();

        Failsafe
                .with(policy.onRetry(event -> {
                    final RequestArguments arguments = RequestArguments.create()
                            .withMethod(HttpMethod.GET)
                            .withUri(URI.create("https://www.example.org/"));
                    unit.onRetry(arguments, event);
                }))
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
