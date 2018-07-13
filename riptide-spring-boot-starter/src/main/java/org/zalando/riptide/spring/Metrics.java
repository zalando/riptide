package org.zalando.riptide.spring;

import com.google.common.collect.ImmutableList;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import org.zalando.riptide.Plugin;
import org.zalando.riptide.failsafe.CircuitBreakerListener;
import org.zalando.riptide.failsafe.RetryListener;
import org.zalando.riptide.failsafe.metrics.MetricsCircuitBreakerListener;
import org.zalando.riptide.failsafe.metrics.MetricsRetryListener;
import org.zalando.riptide.metrics.MetricsPlugin;

final class Metrics {

    private Metrics() {

    }

    public static Plugin createMetricsPlugin(final MeterRegistry registry,
            final ImmutableList<Tag> tags) {
        return new MetricsPlugin(registry).withDefaultTags(tags);
    }

    public static CircuitBreakerListener createCircuitBreakerListener(final MeterRegistry registry,
            final ImmutableList<Tag> defaultTags) {
        return new MetricsCircuitBreakerListener(registry).withDefaultTags(defaultTags);
    }

    public static CircuitBreakerListener getDefaultCircuitBreakerListener() {
        return CircuitBreakerListener.DEFAULT;
    }

    public static RetryListener createRetryListener(final MeterRegistry registry,
            final ImmutableList<Tag> defaultTags) {
        return new MetricsRetryListener(registry).withDefaultTags(defaultTags);
    }

    public static RetryListener getDefaultRetryListener() {
        return RetryListener.DEFAULT;
    }
}
