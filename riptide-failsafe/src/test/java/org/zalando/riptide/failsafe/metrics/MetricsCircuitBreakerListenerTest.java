package org.zalando.riptide.failsafe.metrics;

import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import net.jodah.failsafe.CircuitBreaker;
import org.junit.jupiter.api.Test;
import org.springframework.http.client.ClientHttpResponse;
import org.zalando.riptide.failsafe.CircuitBreakerListener;

import java.util.ArrayList;
import java.util.List;

import static com.google.common.collect.Iterables.getOnlyElement;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;

final class MetricsCircuitBreakerListenerTest {

    private final SimpleMeterRegistry registry = new SimpleMeterRegistry();
    private final CircuitBreakerListener unit = new MetricsCircuitBreakerListener(registry)
            .withMetricName("circuit-breakers")
            .withDefaultTags(Tag.of("test", "true"));

    private final CircuitBreaker<ClientHttpResponse> breaker = new CircuitBreaker<ClientHttpResponse>()
            .onOpen(unit::onOpen)
            .onHalfOpen(unit::onHalfOpen)
            .onClose(unit::onClose);

    @Test
    void shouldRecordOpen() {
        breaker.open();
        breaker.close();

        final Timer timer = getOnlyElement(timers());

        final List<Tag> tags = timer.getId().getTags();
        assertThat(tags, hasItem(Tag.of("state", "OPEN")));
    }

    @Test
    void shouldRecordHalfOpen() {
        breaker.open();
        breaker.halfOpen();
        breaker.open();
        breaker.halfOpen();
        breaker.close();

        final List<Timer> timers = timers();

        assertThat(timers, hasSize(2));

        final Timer open = timers.get(0);
        assertEquals("OPEN", open.getId().getTag("state"));
        assertEquals("true", open.getId().getTag("test"));
        assertEquals(2, open.count());

        final Timer halfOpen = timers.get(1);
        assertEquals("HALF_OPEN", halfOpen.getId().getTag("state"));
        assertEquals("true", halfOpen.getId().getTag("test"));
        assertEquals(2, halfOpen.count());
    }

    private List<Timer> timers() {
        return new ArrayList<>(registry.find("circuit-breakers").timers());
    }

}
