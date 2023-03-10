package org.zalando.riptide.failsafe.metrics;

import com.google.common.collect.ImmutableList;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Timer.Sample;
import lombok.AllArgsConstructor;
import dev.failsafe.CircuitBreaker.State;
import org.apiguardian.api.API;
import org.zalando.riptide.failsafe.CircuitBreakerListener;

import java.util.concurrent.atomic.AtomicReference;

import static com.google.common.collect.ImmutableList.copyOf;
import static com.google.common.collect.Iterables.concat;
import static io.micrometer.core.instrument.Timer.start;
import static java.util.Collections.singleton;
import static lombok.AccessLevel.PRIVATE;
import static dev.failsafe.CircuitBreaker.State.CLOSED;
import static dev.failsafe.CircuitBreaker.State.HALF_OPEN;
import static dev.failsafe.CircuitBreaker.State.OPEN;
import static org.apiguardian.api.API.Status.EXPERIMENTAL;
import static org.apiguardian.api.API.Status.INTERNAL;

@API(status = EXPERIMENTAL)
@AllArgsConstructor(access = PRIVATE)
public final class MetricsCircuitBreakerListener implements CircuitBreakerListener {

    private final MeterRegistry registry;
    private final String metricName;
    private final ImmutableList<Tag> defaultTags;

    private final AtomicReference<State> state = new AtomicReference<>(CLOSED);
    private final AtomicReference<Sample> sample;

    public MetricsCircuitBreakerListener(final MeterRegistry registry) {
        this(registry, "http.client.circuit-breakers", ImmutableList.of());
    }

    @API(status = INTERNAL)
    MetricsCircuitBreakerListener(
            final MeterRegistry registry,
            final String metricName,
            final ImmutableList<Tag> defaultTags) {

        this(registry, metricName, defaultTags,
                new AtomicReference<>(start(registry)));
    }

    public MetricsCircuitBreakerListener withMetricName(final String metricName) {
        return new MetricsCircuitBreakerListener(registry, metricName, defaultTags);
    }

    public MetricsCircuitBreakerListener withDefaultTags(final Tag... defaultTags) {
        return withDefaultTags(copyOf(defaultTags));
    }

    public MetricsCircuitBreakerListener withDefaultTags(final Iterable<Tag> defaultTags) {
        return new MetricsCircuitBreakerListener(registry, metricName, copyOf(defaultTags));
    }

    @Override
    public void onOpen() {
        on(OPEN);
    }

    @Override
    public void onHalfOpen() {
        on(HALF_OPEN);
    }

    @Override
    public void onClose() {
        on(CLOSED);
    }

    private void on(final State to) {
        final State from = state.getAndSet(to);
        final Sample last = sample.getAndSet(start(registry));

        if (from != CLOSED) {
            last.stop(registry.timer(metricName, tags(from)));
        }
    }

    private Iterable<Tag> tags(final State state) {
        return concat(defaultTags, singleton(Tag.of("state", state.name())));
    }

}
