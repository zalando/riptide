package org.zalando.riptide.httpclient;

import org.junit.jupiter.api.Test;
import org.zalando.fauxpas.ThrowingBiConsumer;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@SuppressWarnings("ResultOfMethodCallIgnored")
final class EndOfStreamDetectingInputStreamTest {

    @SuppressWarnings("unchecked")
    private final ThrowingBiConsumer<InputStream, Boolean, IOException> closer = mock(ThrowingBiConsumer.class);

    private final ByteArrayInputStream original = new ByteArrayInputStream(new byte[]{0});
    private final InputStream unit = new EndOfStreamDetectingInputStream(original, closer);

    @Test
    void shouldReadBeforeEndOfStream() throws IOException {
        unit.read();
        unit.close();

        verify(closer).tryAccept(original, false);
    }

    @Test
    void shouldReadUntilEndOfStream() throws IOException {
        unit.read();
        unit.read();
        unit.close();

        verify(closer).tryAccept(original, true);
    }

    @Test
    void shouldReadBytesBeforeEndOfStream() throws IOException {
        unit.read(new byte[1]);
        unit.close();

        verify(closer).tryAccept(original, false);
    }

    @Test
    void shouldReadBytesUntilEndOfStream() throws IOException {
        unit.read(new byte[2]);
        unit.close();

        verify(closer).tryAccept(original, true);
    }

    @Test
    void shouldReadBytesOffsetBeforeEndOfStream() throws IOException {
        unit.read(new byte[1], 0, 1);
        unit.close();

        verify(closer).tryAccept(original, false);
    }

    @Test
    void shouldReadBytesOffsetUntilEndOfStream() throws IOException {
        unit.read(new byte[2], 0, 2);
        unit.close();

        verify(closer).tryAccept(original, true);
    }

}
