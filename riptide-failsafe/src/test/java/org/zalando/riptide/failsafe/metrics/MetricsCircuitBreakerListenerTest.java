package org.zalando.riptide.failsafe.metrics;

import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import net.jodah.failsafe.CircuitBreaker;
import org.junit.Test;
import org.zalando.riptide.failsafe.CircuitBreakerListener;

import java.util.List;
import java.util.stream.Collectors;

import static com.google.common.collect.Iterables.getOnlyElement;
import static com.google.common.collect.Ordering.from;
import static java.util.Comparator.comparing;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

public final class MetricsCircuitBreakerListenerTest {

    private final SimpleMeterRegistry registry = new SimpleMeterRegistry();
    private final CircuitBreakerListener unit = new MetricsCircuitBreakerListener(registry)
            .withMetricName("circuit-breakers")
            .withDefaultTags(Tag.of("test", "true"));

    private final CircuitBreaker breaker = new CircuitBreaker()
            .onOpen(unit::onOpen)
            .onHalfOpen(unit::onHalfOpen)
            .onClose(unit::onClose);

    @Test
    public void shouldRecordOpen() {
        breaker.open();
        breaker.close();

        final Timer timer = getOnlyElement(timers());

        final List<Tag> tags = tags(timer);
        assertThat(tags, hasItem(Tag.of("state", "OPEN")));
    }

    @Test
    public void shouldRecordHalfOpen() {
        breaker.open();
        breaker.halfOpen();
        breaker.open();
        breaker.halfOpen();
        breaker.close();

        final List<Timer> timers = timers();

        assertThat(timers, hasSize(2));

        final Timer halfOpen = timers.get(0);
        assertEquals("HALF_OPEN", halfOpen.getId().getTag("state"));
        assertEquals("true", halfOpen.getId().getTag("test"));
        assertEquals(2, halfOpen.count());

        final Timer open = timers.get(1);
        assertEquals("OPEN", open.getId().getTag("state"));
        assertEquals("true", open.getId().getTag("test"));
        assertEquals(2, open.count());
    }

    private List<Timer> timers() {
        return registry.find("circuit-breakers").timers().stream()
                .sorted(comparing(this::tags, from(comparing(Tag::getKey)
                        .thenComparing(Tag::getValue)).lexicographical()))
                .collect(Collectors.toList());
    }

    private List<Tag> tags(final Timer timer) {
        return timer.getId().getTags();
    }

}
