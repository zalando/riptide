package org.zalando.riptide.autoconfigure;

import dev.failsafe.CircuitBreaker;
import dev.failsafe.CircuitBreakerBuilder;
import dev.failsafe.RetryPolicy;
import dev.failsafe.RetryPolicyBuilder;
import dev.failsafe.Timeout;
import dev.failsafe.function.ContextualSupplier;
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
import org.zalando.riptide.failsafe.RequestPolicies;
import org.zalando.riptide.failsafe.RetryAfterDelayFunction;
import org.zalando.riptide.failsafe.RetryException;
import org.zalando.riptide.failsafe.RetryRequestPolicy;
import org.zalando.riptide.failsafe.TaskDecorator;
import org.zalando.riptide.idempotency.IdempotencyPredicate;

import javax.annotation.Nullable;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static java.time.Clock.systemUTC;
import static java.time.temporal.ChronoUnit.MILLIS;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.zalando.riptide.failsafe.CheckedPredicateConverter.toCheckedPredicate;
import static org.zalando.riptide.failsafe.TaskDecorator.composite;
import static org.zalando.riptide.faults.Predicates.alwaysTrue;
import static org.zalando.riptide.faults.TransientFaults.transientConnectionFaults;
import static org.zalando.riptide.faults.TransientFaults.transientSocketFaults;

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

        final CircuitBreakerBuilder<ClientHttpResponse> breakerBuilder = CircuitBreaker.builder();

        Optional.ofNullable(client.getCircuitBreaker().getFailureThreshold())
                .ifPresent(threshold -> threshold.applyTo(breakerBuilder::withFailureThreshold));

        Optional.ofNullable(client.getCircuitBreaker().getFailureRateThreshold())
                        .ifPresent(threshold -> threshold.applyTo(breakerBuilder::withFailureRateThreshold));

        Optional.ofNullable(client.getCircuitBreaker().getDelay())
                .ifPresent(delay -> delay.applyTo(breakerBuilder::withDelay));

        Optional.ofNullable(client.getCircuitBreaker().getSuccessThreshold())
                .ifPresent(threshold -> threshold.applyTo(breakerBuilder::withSuccessThreshold));

        breakerBuilder.withDelayFn(delayFunction())
                .onOpen(event -> listener.onOpen())
                .onHalfOpen(event -> listener.onHalfOpen())
                .onClose(event -> listener.onClose());

        return breakerBuilder.build();
    }

    public static Plugin createRetryFailsafePlugin(
            final Client client, final List<TaskDecorator> decorators) {

        if (client.getTransientFaultDetection().getEnabled()) {
            return new FailsafePlugin()
                    .withPolicy(new RetryRequestPolicy(getRetryPolicyBuilder(client)
                            .handleIf(toCheckedPredicate(transientSocketFaults()))
                            .build())
                            .withPredicate(new IdempotencyPredicate()))
                    .withPolicy(new RetryRequestPolicy(getRetryPolicyBuilder(client)
                            .handleIf(toCheckedPredicate(transientConnectionFaults()))
                            .build())
                            .withPredicate(alwaysTrue()))
                    .withPolicy(new RetryRequestPolicy(getRetryPolicyBuilder(client).handle(RetryException.class).build()))
                    .withDecorator(composite(decorators));
        } else {
            return new FailsafePlugin()
                    .withPolicy(new RetryRequestPolicy(getRetryPolicyBuilder(client).handle(RetryException.class).build()))
                    .withDecorator(composite(decorators));
        }
    }

    private static RetryPolicyBuilder<ClientHttpResponse> getRetryPolicyBuilder(Client client) {
        final RetryPolicyBuilder<ClientHttpResponse> policyBuilder = RetryPolicy.builder();

        final Retry config = client.getRetry();

        Optional.ofNullable(config.getFixedDelay())
                .ifPresent(delay -> delay.applyTo((Consumer<Duration>)policyBuilder::withDelay));

        Optional.ofNullable(config.getBackoff())
                .filter(Backoff::getEnabled)
                .ifPresent(backoff -> {
                    final TimeSpan delay = backoff.getDelay();
                    final TimeSpan maxDelay = backoff.getMaxDelay();
                    final TimeUnit unit = MILLISECONDS;

                    @Nullable final Double delayFactor = backoff.getDelayFactor();

                    if (delayFactor == null) {
                        policyBuilder.withBackoff(delay.to(unit), maxDelay.to(unit), MILLIS);
                    } else {
                        policyBuilder.withBackoff(delay.to(unit), maxDelay.to(unit), MILLIS, delayFactor);
                    }
                });

        Optional.ofNullable(config.getMaxRetries())
                .ifPresent(policyBuilder::withMaxRetries);

        Optional.ofNullable(config.getMaxDuration())
                .ifPresent(duration -> duration.applyTo(policyBuilder::withMaxDuration));

        Optional.ofNullable(config.getJitterFactor())
                .ifPresent(policyBuilder::withJitter);

        Optional.ofNullable(config.getJitter())
                .ifPresent(jitter -> jitter.applyTo(policyBuilder::withJitter));

        policyBuilder.withDelayFn(delayFunction());
        return policyBuilder;
    }

    public static Plugin createBackupRequestPlugin(
            final Client client, final List<TaskDecorator> decorators) {

        final TimeSpan delay = client.getBackupRequest().getDelay();

        return new FailsafePlugin()
                .withPolicy(RequestPolicies.of(
                        new BackupRequest<>(delay.getAmount(), delay.getUnit()),
                        new IdempotencyPredicate()))
                .withDecorator(composite(decorators));
    }

    public static Plugin createTimeoutPlugin(
            final Client client, final List<TaskDecorator> decorators) {

        final Duration timeout = client.getTimeouts().getGlobal().toDuration();

        return new FailsafePlugin()
                .withPolicy(
                        Timeout.<ClientHttpResponse>builder(timeout)
                                .withInterrupt()
                                .build()
                )
                .withDecorator(composite(decorators));
    }

    private static ContextualSupplier<ClientHttpResponse, Duration> delayFunction() {
        return new CompositeDelayFunction<>(Arrays.asList(
                new RetryAfterDelayFunction(systemUTC()),
                new RateLimitResetDelayFunction(systemUTC())
        ));
    }

}
