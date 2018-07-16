package org.zalando.riptide.spring;

import net.jodah.failsafe.CircuitBreaker;
import net.jodah.failsafe.RetryPolicy;
import org.zalando.riptide.Plugin;
import org.zalando.riptide.failsafe.FailsafePlugin;
import org.zalando.riptide.failsafe.RetryListener;

import java.util.concurrent.ScheduledExecutorService;

final class Resilience {

    private Resilience() {

    }

    public static Plugin createFailsafePlugin(final ScheduledExecutorService scheduler,
            final RetryPolicy retryPolicy, final CircuitBreaker circuitBreaker, final RetryListener listener) {
        return new FailsafePlugin(scheduler)
                .withRetryPolicy(retryPolicy)
                .withCircuitBreaker(circuitBreaker)
                .withListener(listener);
    }

}
