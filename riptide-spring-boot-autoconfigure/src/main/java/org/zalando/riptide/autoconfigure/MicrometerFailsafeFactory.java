package org.zalando.riptide.autoconfigure;

import com.google.common.collect.ImmutableList;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import org.zalando.riptide.failsafe.CircuitBreakerListener;
import org.zalando.riptide.failsafe.metrics.MetricsCircuitBreakerListener;

final class MicrometerFailsafeFactory {

    private MicrometerFailsafeFactory() {

    }

    public static CircuitBreakerListener createCircuitBreakerListener(final MeterRegistry registry,
            final ImmutableList<Tag> defaultTags) {
        return new MetricsCircuitBreakerListener(registry).withDefaultTags(defaultTags);
    }

    public static CircuitBreakerListener getDefaultCircuitBreakerListener() {
        return CircuitBreakerListener.DEFAULT;
    }

}
