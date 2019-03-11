package org.zalando.riptide.faults;

import org.junit.jupiter.api.Test;

import javax.net.ssl.SSLException;
import javax.net.ssl.SSLHandshakeException;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.SocketTimeoutException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;

final class DefaultFaultClassifierTest {

    private final FaultClassifier unit = new DefaultFaultClassifier();

    @Test
    void shouldClassifyIOException() {
        assertTransient(new IOException());
    }

    @Test
    void shouldClassifyInterruptedIOException() {
        assertTransient(new InterruptedIOException());
    }

    @Test
    void shouldClassifySocketTimeoutException() {
        assertTransient(new SocketTimeoutException());
    }

    @Test
    void shouldNotClassifySSLException() {
        assertNotTransient(new SSLException("Oops"));
    }

    @Test
    void shouldClassifySSLHandshakeException() {
        assertTransient(new SSLHandshakeException("Remote host closed connection during handshake"));
    }

    @Test
    void shouldNotClassifyGenericSSLHandShakeException() {
        assertNotTransient(new SSLHandshakeException("No hands, no cookies"));
    }

    @Test
    void shouldClassifyTransientFaultOnlyOnce() {
        final Throwable throwable = unit.classify(
                unit.classify(new TransientFaultException(new InterruptedIOException())));

        assertThat(throwable, is(instanceOf(TransientFaultException.class)));
        assertThat(throwable.getCause(), is(instanceOf(InterruptedIOException.class)));
    }

    private void assertTransient(final Exception e) {
        assertThat(unit.classify(e), is(instanceOf(TransientFaultException.class)));
    }

    private void assertNotTransient(final Exception e) {
        assertThat(unit.classify(e), is(sameInstance(e)));
    }

}
