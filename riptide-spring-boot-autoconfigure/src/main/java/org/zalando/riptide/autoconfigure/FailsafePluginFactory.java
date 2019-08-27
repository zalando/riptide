package org.zalando.riptide.autoconfigure;

import com.google.common.collect.*;
import net.jodah.failsafe.CircuitBreaker;
import net.jodah.failsafe.*;
import net.jodah.failsafe.function.*;
import org.springframework.http.client.*;
import org.zalando.riptide.*;
import org.zalando.riptide.autoconfigure.RiptideProperties.*;
import org.zalando.riptide.autoconfigure.RiptideProperties.Retry.*;
import org.zalando.riptide.failsafe.*;
import org.zalando.riptide.faults.*;

import javax.annotation.*;
import java.util.*;
import java.util.concurrent.*;

import static java.time.Clock.*;
import static java.time.temporal.ChronoUnit.*;
import static java.util.concurrent.TimeUnit.*;

@SuppressWarnings("unused")
final class FailsafePluginFactory {

    private FailsafePluginFactory() {

    }

    public static Plugin create(
            final ScheduledExecutorService scheduler,
            @Nullable final RetryPolicy<ClientHttpResponse> retryPolicy,
            @Nullable final CircuitBreaker<ClientHttpResponse> circuitBreaker,
            final RetryListener listener) {

        final ImmutableList.Builder<Policy<ClientHttpResponse>> policies = ImmutableList.builder();

        if (retryPolicy != null) {
            policies.add(retryPolicy);
        }

        if (circuitBreaker != null) {
            policies.add(circuitBreaker);
        }

        return new FailsafePlugin(policies.build(), scheduler)
                .withListener(listener);
    }

    public static RetryPolicy<ClientHttpResponse> createRetryPolicy(final Client client) {
        final RetryPolicy<ClientHttpResponse> policy = new RetryPolicy<>();

        final Retry config = client.getRetry();

        Optional.ofNullable(config.getFixedDelay())
                .ifPresent(delay -> delay.applyTo(policy::withDelay));

        Optional.ofNullable(config.getBackoff())
                .filter(Backoff::getEnabled)
                .ifPresent(backoff -> {
                    final TimeSpan delay = backoff.getDelay();
                    final TimeSpan maxDelay = backoff.getMaxDelay();
                    final TimeUnit unit = MILLISECONDS;

                    @Nullable final Double delayFactor = backoff.getDelayFactor();

                    if (delayFactor == null) {
                        policy.withBackoff(delay.to(unit), maxDelay.to(unit), MILLIS);
                    } else {
                        policy.withBackoff(delay.to(unit), maxDelay.to(unit), MILLIS, delayFactor);
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

        if (client.getTransientFaultDetection().getEnabled()) {
            policy.handle(TransientFaultException.class);
        }

        return policy
                .handle(RetryException.class)
                .withDelay(delayFunction());
    }

    public static CircuitBreaker<ClientHttpResponse> createCircuitBreaker(final Client client,
            final CircuitBreakerListener listener) {
        final CircuitBreaker<ClientHttpResponse> breaker = new CircuitBreaker<>();

        Optional.ofNullable(client.getTimeouts())
                .filter(RiptideProperties.Timeouts::getEnabled)
                .map(RiptideProperties.Timeouts::getGlobal)
                .ifPresent(timeout -> timeout.applyTo(breaker::withTimeout));

        Optional.ofNullable(client.getCircuitBreaker().getFailureThreshold())
                .ifPresent(threshold -> threshold.applyTo(breaker::withFailureThreshold));

        Optional.ofNullable(client.getCircuitBreaker().getDelay())
                .ifPresent(delay -> delay.applyTo(breaker::withDelay));

        Optional.ofNullable(client.getCircuitBreaker().getSuccessThreshold())
                .ifPresent(threshold -> threshold.applyTo(breaker::withSuccessThreshold));

        return breaker
                .withDelay(delayFunction())
                .onOpen(listener::onOpen)
                .onHalfOpen(listener::onHalfOpen)
                .onClose(listener::onClose);
    }

    private static DelayFunction<ClientHttpResponse, Throwable> delayFunction() {
        return new CompositeDelayFunction<>(Arrays.asList(
                new RetryAfterDelayFunction(systemUTC()),
                new RateLimitResetDelayFunction(systemUTC())
        ));
    }

}
