package org.zalando.riptide.httpclient;

import org.junit.jupiter.api.Test;

import java.io.Closeable;
import java.io.IOException;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.zalando.riptide.httpclient.Closing.closeQuietly;

class ClosingTest {

    @Test
    void closes() throws IOException {
        final Closeable closeable = mock(Closeable.class);
        closeQuietly(closeable);
        verify(closeable).close();
    }

    @Test
    void ignoresNonCloseable() {
        closeQuietly(new Object());
    }

    @Test
    void ignoresNull() {
        closeQuietly(null);
    }

    @Test
    void swallowsException() throws IOException {
        final Closeable closeable = mock(Closeable.class);
        doThrow(new IOException()).when(closeable).close();
        closeQuietly(closeable);
        verify(closeable).close();
    }

}
