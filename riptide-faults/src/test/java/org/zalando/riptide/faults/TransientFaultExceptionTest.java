package org.zalando.riptide.faults;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItemInArray;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;

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
