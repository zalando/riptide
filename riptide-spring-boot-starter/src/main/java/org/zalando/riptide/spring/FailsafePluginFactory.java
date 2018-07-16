package org.zalando.riptide.spring;

import net.jodah.failsafe.CircuitBreaker;
import net.jodah.failsafe.RetryPolicy;
import org.zalando.riptide.failsafe.CircuitBreakerListener;
import org.zalando.riptide.failsafe.FailsafePlugin;
import org.zalando.riptide.failsafe.RetryAfterDelayFunction;
import org.zalando.riptide.failsafe.RetryException;
import org.zalando.riptide.failsafe.RetryListener;
import org.zalando.riptide.faults.TransientFaultException;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static java.time.Clock.systemUTC;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

@SuppressWarnings("unused")
final class FailsafePluginFactory {

    private FailsafePluginFactory() {

    }

    public static FailsafePlugin createFailsafePlugin(final ScheduledExecutorService scheduler,
            @Nullable final RetryPolicy retryPolicy, @Nullable final CircuitBreaker circuitBreaker,
            final RetryListener listener) {
        return new FailsafePlugin(scheduler)
                .withRetryPolicy(retryPolicy)
                .withCircuitBreaker(circuitBreaker)
                .withListener(listener);
    }

    public static RetryPolicy createRetryPolicy(final RiptideProperties.Retry config) {
        final RetryPolicy policy = new RetryPolicy();

        Optional.ofNullable(config.getFixedDelay())
                .ifPresent(delay -> delay.applyTo(policy::withDelay));

        Optional.ofNullable(config.getBackoff())
                .ifPresent(backoff -> {
                    final TimeSpan delay = backoff.getDelay();
                    final TimeSpan maxDelay = backoff.getMaxDelay();
                    final TimeUnit unit = MILLISECONDS;

                    @Nullable final Double delayFactor = backoff.getDelayFactor();

                    if (delayFactor == null) {
                        policy.withBackoff(delay.to(unit), maxDelay.to(unit), unit);
                    } else {
                        policy.withBackoff(delay.to(unit), maxDelay.to(unit), unit, delayFactor);
                    }
                });

        Optional.ofNullable(config.getMaxRetries())
                .ifPresent(policy::withMaxRetries);

        Optional.ofNullable(config.getMaxDuration())
                .ifPresent(duration -> duration.applyTo(policy::withMaxDuration));

        Optional.ofNullable(config.getJitterFactor())
                .ifPresent(policy::withJitter);

        Optional.ofNullable(config.getJitter())
                .ifPresent(jitter -> jitter.applyTo(policy::withJitter));

        policy.retryOn(TransientFaultException.class);
        policy.retryOn(RetryException.class);
        policy.withDelay(new RetryAfterDelayFunction(systemUTC()));

        return policy;
    }

    public static CircuitBreaker createCircuitBreaker(final RiptideProperties.Client client,
            final CircuitBreakerListener listener) {
        final CircuitBreaker breaker = new CircuitBreaker();

        Optional.ofNullable(client.getTimeout())
                .ifPresent(timeout -> timeout.applyTo(breaker::withTimeout));

        Optional.ofNullable(client.getCircuitBreaker().getFailureThreshold())
                .ifPresent(threshold -> threshold.applyTo(breaker::withFailureThreshold));

        Optional.ofNullable(client.getCircuitBreaker().getDelay())
                .ifPresent(delay -> delay.applyTo(breaker::withDelay));

        Optional.ofNullable(client.getCircuitBreaker().getSuccessThreshold())
                .ifPresent(threshold -> threshold.applyTo(breaker::withSuccessThreshold));

        breaker
                .onOpen(listener::onOpen)
                .onHalfOpen(listener::onHalfOpen)
                .onClose(listener::onClose);

        return breaker;
    }

}
