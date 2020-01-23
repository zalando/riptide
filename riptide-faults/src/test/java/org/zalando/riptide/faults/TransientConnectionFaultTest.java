package org.zalando.riptide.faults;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.ConnectException;
import java.net.MalformedURLException;
import java.net.NoRouteToHostException;
import java.net.UnknownHostException;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.zalando.riptide.faults.TransientFaults.transientConnectionFaults;

final class TransientConnectionFaultTest {

    private final Predicate<Throwable> unit = transientConnectionFaults();

    @Test
    void acceptsConnectException() {
        assertTransient(new ConnectException());
    }

    @Test
    void shouldMalformedURL() {
        assertTransient(new MalformedURLException());
    }

    @Test
    void acceptsNoRouteToHost() {
        assertTransient(new NoRouteToHostException());
    }

    @Test
    void acceptsUnknownHost() {
        assertTransient(new UnknownHostException());
    }

    @Test
    void rejectsIOException() {
        assertNotTransient(new IOException());
    }

    private void assertTransient(final Exception e) {
        assertTrue(unit.test(e));
    }

    private void assertNotTransient(final Exception e) {
        assertFalse(unit.test(e));
    }

}
