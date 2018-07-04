package org.zalando.riptide.spring;

import org.zalando.riptide.failsafe.CircuitBreakerListener;

final class CircuitBreakerListeners {

    private CircuitBreakerListeners() {

    }

    public static CircuitBreakerListener getDefault() {
        return CircuitBreakerListener.DEFAULT;
    }

}
