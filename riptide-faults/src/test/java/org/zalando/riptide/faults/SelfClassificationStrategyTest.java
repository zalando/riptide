package org.zalando.riptide.faults;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.zalando.riptide.faults.ClassificationStrategy.self;
import static org.zalando.riptide.faults.TransientFaults.transientSocketFaults;

final class SelfClassificationStrategyTest {

    private final Predicate<Throwable> unit = transientSocketFaults(self());

    @Test
    void shouldClassifyAsTransientWithoutCause() {
        assertTransient(new IOException());
    }

    @Test
    void shouldClassifyAsTransientWithNonTransientCause() {
        assertTransient(new IOException(new IllegalArgumentException()));
    }

    @Test
    void shouldNotClassifyAsTransientWithTransientCause() {
        assertNotTransient(new IllegalArgumentException(new IOException()));
    }

    @Test
    void shouldNotClassifyAsTransientWithTransientIntermediateCause() {
        assertNotTransient(new IllegalStateException(new IOException(new IllegalArgumentException())));
    }

    @Test
    void shouldNotClassifyAsTransient() {
        assertNotTransient(new IllegalArgumentException());
    }

    private void assertTransient(final Exception e) {
        assertTrue(unit.test(e));
    }

    private void assertNotTransient(final Exception e) {
        assertFalse(unit.test(e));
    }

}
