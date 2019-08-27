package org.zalando.riptide.autoconfigure;

import com.google.common.collect.*;
import io.micrometer.core.instrument.*;
import org.zalando.riptide.*;
import org.zalando.riptide.failsafe.*;
import org.zalando.riptide.failsafe.metrics.*;
import org.zalando.riptide.micrometer.*;

final class MicrometerPluginFactory {

    private MicrometerPluginFactory() {

    }

    public static Plugin createMicrometerPlugin(final MeterRegistry registry,
            final ImmutableList<Tag> tags) {
        return new MicrometerPlugin(registry).withDefaultTags(tags);
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
        return new CompositeRetryListener(
                new MetricsRetryListener(registry).withDefaultTags(defaultTags),
                new LoggingRetryListener()
        );
    }

    public static RetryListener getDefaultRetryListener() {
        return new LoggingRetryListener();
    }
}
