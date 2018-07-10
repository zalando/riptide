package org.zalando.riptide.faults;

import org.junit.Test;

import javax.net.ssl.SSLHandshakeException;
import java.io.InterruptedIOException;
import java.net.SocketTimeoutException;
import java.util.NoSuchElementException;

import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThat;

public final class FaultClassifierTest {

    private final FaultClassifier unit = FaultClassifier.createDefault();

    @Test
    public void shouldClassifyInterruptedIOException() {
        assertTransient(new InterruptedIOException());
    }

    @Test
    public void shouldClassifySocketException() {
        assertTransient(new SocketTimeoutException());
    }

    @Test
    public void shouldClassifySSLHandshakeException() {
        assertTransient(new SSLHandshakeException("Remote host closed connection during handshake"));
    }

    @Test
    public void shouldNotClassifyGenericSSLHandShakeException() {
        assertNotTransient(new SSLHandshakeException("No hands, no cookies"));
    }

    @Test
    public void shouldClassifyAsTransientWithNonTransientRootCause() {
        final SocketTimeoutException e = new SocketTimeoutException();
        e.initCause(new NoSuchElementException());
        assertTransient(e);
    }

    @Test
    public void shouldClassifyAsTransientWithTransientIntermediateCause() {
        final SocketTimeoutException e = new SocketTimeoutException();
        e.initCause(new NoSuchElementException());
        assertTransient(new IllegalStateException(e));
    }

    @Test
    public void shouldClassifyTransientFaultOnlyOnce() {
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
