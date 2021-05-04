package org.zalando.riptide.autoconfigure;

import static java.time.Clock.systemUTC;
import static java.time.temporal.ChronoUnit.MILLIS;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.zalando.riptide.faults.Predicates.alwaysTrue;
import static org.zalando.riptide.faults.TransientFaults.transientConnectionFaults;
import static org.zalando.riptide.faults.TransientFaults.transientSocketFaults;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;
import net.jodah.failsafe.CircuitBreaker;
import net.jodah.failsafe.RetryPolicy;
import net.jodah.failsafe.Timeout;
import net.jodah.failsafe.function.DelayFunction;
import org.springframework.http.client.ClientHttpResponse;
import org.zalando.riptide.Plugin;
import org.zalando.riptide.autoconfigure.RiptideProperties.Client;
import org.zalando.riptide.autoconfigure.RiptideProperties.Retry;
import org.zalando.riptide.autoconfigure.RiptideProperties.Retry.Backoff;
import org.zalando.riptide.failsafe.BackupRequest;
import org.zalando.riptide.failsafe.CircuitBreakerListener;
import org.zalando.riptide.failsafe.CompositeDelayFunction;
import org.zalando.riptide.failsafe.CompositeTaskDecorator;
import org.zalando.riptide.failsafe.FailsafePlugin;
import org.zalando.riptide.failsafe.RateLimitResetDelayFunction;
import org.zalando.riptide.failsafe.RequestPolicies;
import org.zalando.riptide.failsafe.RetryAfterDelayFunction;
import org.zalando.riptide.failsafe.RetryException;
import org.zalando.riptide.failsafe.RetryRequestPolicy;
import org.zalando.riptide.failsafe.TaskDecorator;
import org.zalando.riptide.idempotency.IdempotencyPredicate;

@SuppressWarnings("unused")
final class FailsafePluginFactory {

    private FailsafePluginFactory() {

    }

    public static Plugin createCircuitBreakerPlugin(
            final CircuitBreaker<ClientHttpResponse> breaker,
            final List<TaskDecorator> decorators) {

        return new FailsafePlugin()
                .withPolicy(breaker)
                .withDecorator(composite(decorators));
    }

    public static CircuitBreaker<ClientHttpResponse> createCircuitBreaker(
            final Client client,
            final CircuitBreakerListener listener) {

        final CircuitBreaker<ClientHttpResponse> breaker = new CircuitBreaker<>();

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

        return breaker;
    }

    public static Plugin createRetryFailsafePlugin(
            final Client client, final List<TaskDecorator> decorators) {

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

        policy.withDelay(delayFunction());

        if (client.getTransientFaultDetection().getEnabled()) {
            return new FailsafePlugin()
                    .withPolicy(new RetryRequestPolicy(policy.copy()
                            .handleIf(transientSocketFaults()))
                            .withPredicate(new IdempotencyPredicate()))
                    .withPolicy(new RetryRequestPolicy(policy.copy()
                            .handleIf(transientConnectionFaults()))
                            .withPredicate(alwaysTrue()))
                    .withPolicy(new RetryRequestPolicy(policy.handle(RetryException.class)))
                    .withDecorator(composite(decorators));

        } else {
            return new FailsafePlugin()
                    .withPolicy(new RetryRequestPolicy(policy.handle(RetryException.class)))
                    .withDecorator(composite(decorators));
        }
    }

    public static Plugin createBackupRequestPlugin(
            final Client client, final List<TaskDecorator> decorators) {

        final TimeSpan delay = client.getBackupRequest().getDelay();

        return new FailsafePlugin()
                .withPolicy(RequestPolicies.of(
                        new BackupRequest<>(
                                delay.getAmount(),
                                delay.getUnit()),
                        new IdempotencyPredicate()))
                .withDecorator(composite(decorators));
    }

    public static Plugin createTimeoutPlugin(
            final Client client, final List<TaskDecorator> decorators) {

        final Duration timeout = client.getTimeouts().getGlobal().toDuration();

        return new FailsafePlugin()
                .withPolicy(
                        Timeout.<ClientHttpResponse>of(timeout)
                                .withCancel(true))
                .withDecorator(composite(decorators));
    }

    private static DelayFunction<ClientHttpResponse, Throwable> delayFunction() {
        return new CompositeDelayFunction<>(Arrays.asList(
                new RetryAfterDelayFunction(systemUTC()),
                new RateLimitResetDelayFunction(systemUTC())
        ));
    }

    private static TaskDecorator composite(final List<TaskDecorator> decorators) {
        if (decorators.isEmpty()) {
            return TaskDecorator.identity();
        }

        return new CompositeTaskDecorator(decorators);
    }
}
