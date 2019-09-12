package org.zalando.riptide.autoconfigure;

import net.jodah.failsafe.CircuitBreaker;
import net.jodah.failsafe.Policy;
import net.jodah.failsafe.RetryPolicy;
import net.jodah.failsafe.function.DelayFunction;
import org.springframework.http.client.ClientHttpResponse;
import org.zalando.riptide.Plugin;
import org.zalando.riptide.autoconfigure.RiptideProperties.Client;
import org.zalando.riptide.autoconfigure.RiptideProperties.Retry;
import org.zalando.riptide.autoconfigure.RiptideProperties.Retry.Backoff;
import org.zalando.riptide.failsafe.BackupRequest;
import org.zalando.riptide.failsafe.CircuitBreakerListener;
import org.zalando.riptide.failsafe.CompositeDelayFunction;
import org.zalando.riptide.failsafe.FailsafePlugin;
import org.zalando.riptide.failsafe.RateLimitResetDelayFunction;
import org.zalando.riptide.failsafe.RequestPolicy;
import org.zalando.riptide.failsafe.RetryAfterDelayFunction;
import org.zalando.riptide.failsafe.RetryException;
import org.zalando.riptide.failsafe.RetryListener;
import org.zalando.riptide.failsafe.RetryRequestPolicy;
import org.zalando.riptide.failsafe.TaskDecorator;
import org.zalando.riptide.faults.TransientFaultException;
import org.zalando.riptide.idempotency.IdempotencyPredicate;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static java.time.Clock.systemUTC;
import static java.time.temporal.ChronoUnit.MILLIS;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

@SuppressWarnings("unused")
final class FailsafePluginFactory {

    private FailsafePluginFactory() {

    }

    public static Plugin create(
            final RequestPolicy policy,
            final TaskDecorator decorator) {

        return new FailsafePlugin()
                .withPolicy(policy)
                .withDecorator(decorator);
    }

    public static RequestPolicy createRetryPolicy(
            final Client client,
            final RetryListener listener) {

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

        policy.handle(RetryException.class);
        policy.withDelay(delayFunction());

        return new RetryRequestPolicy(policy)
                .withListener(listener);
    }

    public static RequestPolicy createCircuitBreaker(
            final Client client,
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

        breaker.withDelay(delayFunction());
        breaker.onOpen(listener::onOpen);
        breaker.onHalfOpen(listener::onHalfOpen);
        breaker.onClose(listener::onClose);

        return RequestPolicy.of(breaker);
    }

    public static RequestPolicy createBackupRequest(final Client client) {
        return RequestPolicy.of(
                new BackupRequest<>(
                        client.getBackupRequest().getDelay().getAmount(),
                        client.getBackupRequest().getDelay().getUnit()),
                new IdempotencyPredicate());
    }

    private static DelayFunction<ClientHttpResponse, Throwable> delayFunction() {
        return new CompositeDelayFunction<>(Arrays.asList(
                new RetryAfterDelayFunction(systemUTC()),
                new RateLimitResetDelayFunction(systemUTC())
        ));
    }

}
