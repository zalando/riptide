package org.zalando.riptide.faults;

import org.junit.jupiter.api.*;

import java.io.*;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;

final class CausalChainClassificationStrategyTest {

    private final FaultClassifier unit = new DefaultFaultClassifier(new CausalChainClassificationStrategy());

    @Test
    void shouldClassifyAsTransientWithoutCause() {
        assertTransient(new IOException());
    }

    @Test
    void shouldClassifyAsTransientWithNonTransientRootCause() {
        assertTransient(new IOException(new IllegalArgumentException()));
    }

    @Test
    void shouldClassifyAsTransientWithTransientCause() {
        assertTransient(new IllegalArgumentException(new IOException()));
    }

    @Test
    void shouldClassifyAsTransientWithTransientIntermediateCause() {
        assertTransient(new IllegalStateException(new IOException(new IllegalArgumentException())));
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
