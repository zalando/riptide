package org.zalando.riptide.failsafe;

import org.apiguardian.api.API;

import static org.apiguardian.api.API.Status.EXPERIMENTAL;

@API(status = EXPERIMENTAL)
public interface CircuitBreakerListener {

    CircuitBreakerListener DEFAULT = new CircuitBreakerListener() {
        // nothing to implement, since default methods are sufficient
    };

    default void onOpen() {

    }

    default void onHalfOpen() {

    }

    default void onClose() {

    }

}
