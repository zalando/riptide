package org.zalando.riptide.spring;

import net.jodah.failsafe.RetryPolicy;
import org.springframework.beans.factory.FactoryBean;
import org.zalando.riptide.spring.RiptideSettings.Failsafe.Retry;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

final class RetryPolicyFactoryBean implements FactoryBean<RetryPolicy> {

    private final RetryPolicy retryPolicy = new RetryPolicy();

    public void setConfiguration(final Retry config) {
        Optional.ofNullable(config.getFixedDelay())
                .ifPresent(delay -> delay.applyTo(retryPolicy::withDelay));

        Optional.ofNullable(config.getExponentialBackoff()).ifPresent(backoff -> {
            final TimeSpan delay = backoff.getDelay();
            final TimeSpan maxDelay = backoff.getMaxDelay();
            final TimeUnit unit = MILLISECONDS;

            @Nullable final Double delayFactor = backoff.getDelayFactor();

            if (delayFactor == null) {
                retryPolicy.withBackoff(delay.to(unit), maxDelay.to(unit), unit);
            } else {
                retryPolicy.withBackoff(delay.to(unit), maxDelay.to(unit), unit, delayFactor);
            }
        });

        Optional.ofNullable(config.getMaxRetries())
                .ifPresent(retryPolicy::withMaxRetries);

        Optional.ofNullable(config.getMaxDuration())
                .ifPresent(duration -> duration.applyTo(retryPolicy::withMaxDuration));

        Optional.ofNullable(config.getJitterFactor())
                .ifPresent(retryPolicy::withJitter);

        Optional.ofNullable(config.getJitter())
                .ifPresent(jitter -> jitter.applyTo(retryPolicy::withJitter));
    }

    @Override
    public RetryPolicy getObject() throws Exception {
        return retryPolicy;
    }

    @Override
    public Class<?> getObjectType() {
        return RetryPolicy.class;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

}
