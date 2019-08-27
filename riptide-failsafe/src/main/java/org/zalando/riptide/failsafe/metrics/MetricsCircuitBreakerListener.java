package org.zalando.riptide.failsafe.metrics;

import com.google.common.collect.*;
import io.micrometer.core.instrument.*;
import io.micrometer.core.instrument.Timer.*;
import net.jodah.failsafe.CircuitBreaker.*;
import org.apiguardian.api.*;
import org.zalando.riptide.failsafe.*;

import java.util.concurrent.atomic.*;

import static com.google.common.collect.Iterables.*;
import static io.micrometer.core.instrument.Timer.*;
import static java.util.Collections.*;
import static net.jodah.failsafe.CircuitBreaker.State.*;
import static org.apiguardian.api.API.Status.*;

@API(status = EXPERIMENTAL)
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
    MetricsCircuitBreakerListener(final MeterRegistry registry, final String metricName,
            final ImmutableList<Tag> defaultTags) {
        this.registry = registry;
        this.metricName = metricName;
        this.defaultTags = defaultTags;
        this.sample = new AtomicReference<>(start(registry));
    }

    public MetricsCircuitBreakerListener withMetricName(final String metricName) {
        return new MetricsCircuitBreakerListener(registry, metricName, defaultTags);
    }

    public MetricsCircuitBreakerListener withDefaultTags(final Tag... defaultTags) {
        return withDefaultTags(ImmutableList.copyOf(defaultTags));
    }

    public MetricsCircuitBreakerListener withDefaultTags(final Iterable<Tag> defaultTags) {
        return new MetricsCircuitBreakerListener(registry, metricName, ImmutableList.copyOf(defaultTags));
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
