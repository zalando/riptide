package org.zalando.riptide.spring;

import com.google.common.collect.ImmutableList;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import org.zalando.riptide.Plugin;
import org.zalando.riptide.failsafe.CircuitBreakerListener;
import org.zalando.riptide.failsafe.CompoundRetryListener;
import org.zalando.riptide.failsafe.LoggingRetryListener;
import org.zalando.riptide.failsafe.RetryListener;
import org.zalando.riptide.failsafe.metrics.MetricsCircuitBreakerListener;
import org.zalando.riptide.failsafe.metrics.MetricsRetryListener;
import org.zalando.riptide.metrics.MetricsPlugin;

final class MetricsPluginFactory {

    private MetricsPluginFactory() {

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
        return new CompoundRetryListener(
                new MetricsRetryListener(registry).withDefaultTags(defaultTags),
                new LoggingRetryListener()
        );
    }

    public static RetryListener getDefaultRetryListener() {
        return new LoggingRetryListener();
    }
}
