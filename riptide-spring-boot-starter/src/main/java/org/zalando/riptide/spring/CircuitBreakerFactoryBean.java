package org.zalando.riptide.spring;

import net.jodah.failsafe.CircuitBreaker;
import org.springframework.beans.factory.FactoryBean;

import java.util.Optional;

final class CircuitBreakerFactoryBean implements FactoryBean<CircuitBreaker> {

    private final CircuitBreaker circuitBreaker = new CircuitBreaker();

    public void setTimeout(final TimeSpan timeout) {
        timeout.applyTo(circuitBreaker::withTimeout);
    }

    public void setConfiguration(final RiptideSettings.CircuitBreaker config) {
        Optional.ofNullable(config.getFailureThreshold())
                .ifPresent(threshold -> threshold.applyTo(circuitBreaker::withFailureThreshold));

        Optional.ofNullable(config.getDelay())
                .ifPresent(delay -> delay.applyTo(circuitBreaker::withDelay));

        Optional.ofNullable(config.getSuccessThreshold())
                .ifPresent(threshold -> threshold.applyTo(circuitBreaker::withSuccessThreshold));
    }

    @Override
    public CircuitBreaker getObject() {
        return circuitBreaker;
    }

    @Override
    public Class<?> getObjectType() {
        return CircuitBreaker.class;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

}
