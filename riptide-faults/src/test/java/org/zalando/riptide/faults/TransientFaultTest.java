package org.zalando.riptide.faults;

import org.junit.jupiter.api.Test;

import javax.net.ssl.SSLException;
import javax.net.ssl.SSLHandshakeException;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.zalando.riptide.faults.TransientFaults.transientFaults;

final class TransientFaultTest {

    private final Predicate<Throwable> unit = transientFaults();

    @Test
    void acceptsIOException() {
        assertTransient(new IOException());
    }

    @Test
    void acceptsInterruptedIOException() {
        assertTransient(new InterruptedIOException());
    }

    @Test
    void acceptsSocketTimeout() {
        assertTransient(new SocketTimeoutException());
    }

    @Test
    void rejectsSSLException() {
        assertNotTransient(new SSLException("Oops"));
    }

    @Test
    void acceptsSpecialSSLHandshake() {
        assertTransient(new SSLHandshakeException("Remote host closed connection during handshake"));
    }

    @Test
    void rejectsGenericSSLHandshakeException() {
        assertNotTransient(new SSLHandshakeException("No hands, no cookies"));
    }

    @Test
    void acceptsUnknownFaults() {
        assertTransient(new UnknownHostException());
    }

    private void assertTransient(final Exception e) {
        assertTrue(unit.test(e));
    }

    private void assertNotTransient(final Exception e) {
        assertFalse(unit.test(e));
    }

}
