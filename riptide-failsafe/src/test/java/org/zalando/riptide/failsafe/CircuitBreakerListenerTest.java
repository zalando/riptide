package org.zalando.riptide.failsafe;

import org.junit.Test;

public final class CircuitBreakerListenerTest {

    private final CircuitBreakerListener unit = CircuitBreakerListener.DEFAULT;

    @Test
    public void shouldDoNothing() {
        unit.onOpen();
        unit.onHalfOpen();
        unit.onClose();
    }

}
