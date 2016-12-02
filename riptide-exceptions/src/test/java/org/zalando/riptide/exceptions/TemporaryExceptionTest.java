package org.zalando.riptide.exceptions;

import org.junit.Test;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import static org.hamcrest.Matchers.hasItemInArray;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;

public final class TemporaryExceptionTest {

    @Test
    public void shouldSupportSuppressedExceptions() {
        final TemporaryException unit = new TemporaryException();
        unit.addSuppressed(new TimeoutException());
        unit.addSuppressed(new IOException());
        unit.addSuppressed(new IllegalStateException());

        assertThat(unit.getSuppressed(), is(hasItemInArray(instanceOf(TimeoutException.class))));
        assertThat(unit.getSuppressed(), is(hasItemInArray(instanceOf(IOException.class))));
        assertThat(unit.getSuppressed(), is(hasItemInArray(instanceOf(IllegalStateException.class))));
    }

}