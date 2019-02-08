package org.zalando.riptide.httpclient;

import org.junit.jupiter.api.Test;
import org.zalando.riptide.httpclient.EndOfStreamAwareInputStream.Closer;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@SuppressWarnings("ResultOfMethodCallIgnored")
final class EndOfStreamAwareInputStreamTest {

    private final Closer closer = mock(Closer.class);

    private final ByteArrayInputStream original = new ByteArrayInputStream(new byte[]{0});
    private final InputStream unit = new EndOfStreamAwareInputStream(original, closer);

    @Test
    void shouldReadBeforeEndOfStream() throws IOException {
        unit.read();
        unit.close();

        verify(closer).close(original, false);
    }

    @Test
    void shouldReadUntilEndOfStream() throws IOException {
        unit.read();
        unit.read();
        unit.close();

        verify(closer).close(original, true);
    }

    @Test
    void shouldReadBytesBeforeEndOfStream() throws IOException {
        unit.read(new byte[1]);
        unit.close();

        verify(closer).close(original, false);
    }

    @Test
    void shouldReadBytesUntilEndOfStream() throws IOException {
        unit.read(new byte[1]);
        unit.read(new byte[1]);
        unit.close();

        verify(closer).close(original, true);
    }

    @Test
    void shouldReadBytesOffsetBeforeEndOfStream() throws IOException {
        unit.read(new byte[1], 0, 1);
        unit.close();

        verify(closer).close(original, false);
    }

    @Test
    void shouldReadBytesOffsetUntilEndOfStream() throws IOException {
        unit.read(new byte[1], 0, 1);
        unit.read(new byte[1], 0, 1);
        unit.close();

        verify(closer).close(original, true);
    }

}
