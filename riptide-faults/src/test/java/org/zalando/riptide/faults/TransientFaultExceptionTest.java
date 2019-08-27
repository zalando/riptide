package org.zalando.riptide.faults;

import org.junit.jupiter.api.*;

import java.io.*;
import java.util.concurrent.*;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;

final class TransientFaultExceptionTest {

    @Test
    void shouldSupportSuppressedExceptions() {
        final TransientFaultException unit = new TransientFaultException();
        unit.addSuppressed(new TimeoutException());
        unit.addSuppressed(new IOException());
        unit.addSuppressed(new IllegalStateException());

        assertThat(unit.getSuppressed(), is(hasItemInArray(instanceOf(TimeoutException.class))));
        assertThat(unit.getSuppressed(), is(hasItemInArray(instanceOf(IOException.class))));
        assertThat(unit.getSuppressed(), is(hasItemInArray(instanceOf(IllegalStateException.class))));
    }

}
