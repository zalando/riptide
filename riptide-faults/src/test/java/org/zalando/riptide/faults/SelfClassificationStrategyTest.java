package org.zalando.riptide.faults;

import org.junit.jupiter.api.*;

import java.io.*;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;

final class SelfClassificationStrategyTest {

    private final FaultClassifier unit = new DefaultFaultClassifier(new SelfClassificationStrategy());

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
        assertThat(unit.classify(e), is(instanceOf(TransientFaultException.class)));
    }

    private void assertNotTransient(final Exception e) {
        assertThat(unit.classify(e), is(sameInstance(e)));
    }

}
