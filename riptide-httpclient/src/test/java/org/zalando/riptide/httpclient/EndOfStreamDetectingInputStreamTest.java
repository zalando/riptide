package org.zalando.riptide.httpclient;

import org.junit.Test;
import org.zalando.fauxpas.ThrowingBiConsumer;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@SuppressWarnings("ResultOfMethodCallIgnored")
public final class EndOfStreamDetectingInputStreamTest {

    @SuppressWarnings("unchecked")
    private final ThrowingBiConsumer<InputStream, Boolean, IOException> closer = mock(ThrowingBiConsumer.class);

    private final ByteArrayInputStream original = new ByteArrayInputStream(new byte[]{0});
    private final InputStream unit = new EndOfStreamDetectingInputStream(original, closer);

    @Test
    public void shouldReadBeforeEndOfStream() throws IOException {
        unit.read();
        unit.close();

        verify(closer).tryAccept(original, false);
    }

    @Test
    public void shouldReadUntilEndOfStream() throws IOException {
        unit.read();
        unit.read();
        unit.close();

        verify(closer).tryAccept(original, true);
    }

    @Test
    public void shouldReadBytesBeforeEndOfStream() throws IOException {
        unit.read(new byte[1]);
        unit.close();

        verify(closer).tryAccept(original, false);
    }

    @Test
    public void shouldReadBytesUntilEndOfStream() throws IOException {
        unit.read(new byte[2]);
        unit.close();

        verify(closer).tryAccept(original, true);
    }

    @Test
    public void shouldReadBytesOffsetBeforeEndOfStream() throws IOException {
        unit.read(new byte[1], 0, 1);
        unit.close();

        verify(closer).tryAccept(original, false);
    }

    @Test
    public void shouldReadBytesOffsetUntilEndOfStream() throws IOException {
        unit.read(new byte[2], 0, 2);
        unit.close();

        verify(closer).tryAccept(original, true);
    }

}
