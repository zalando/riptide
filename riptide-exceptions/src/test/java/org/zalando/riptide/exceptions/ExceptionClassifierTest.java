package org.zalando.riptide.exceptions;

import org.junit.Test;

import javax.net.ssl.SSLHandshakeException;
import java.io.InterruptedIOException;
import java.net.SocketTimeoutException;

import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThat;

public final class ExceptionClassifierTest {

    private final ExceptionClassifier classifier = ExceptionClassifier.createDefault();

    @Test
    public void shouldClassifyInterruptedIOException() {
        assertTemporary(new InterruptedIOException());
    }

    @Test
    public void shouldClassifySocketException() {
        assertTemporary(new SocketTimeoutException());
    }

    @Test
    public void shouldClassifySSLHandshakeException() {
        assertTemporary(new SSLHandshakeException("Remote host closed connection during handshake"));
    }

    @Test
    public void shouldNotClassifyGenericSSLHandShakeException() {
        assertNotTemporary(new SSLHandshakeException("No hands, no cookies"));
    }

    private void assertTemporary(final Exception e) {
        assertThat(classifier.classify(e), is(instanceOf(TemporaryException.class)));
    }

    private void assertNotTemporary(final Exception e) {
        assertThat(classifier.classify(e), is(sameInstance(e)));
    }

}