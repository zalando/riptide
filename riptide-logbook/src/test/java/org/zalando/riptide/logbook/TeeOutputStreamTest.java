package org.zalando.riptide.logbook;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

final class TeeOutputStreamTest {

    private final OutputStream first = mock(OutputStream.class);
    private final OutputStream second = mock(OutputStream.class);
    private final OutputStream unit = new TeeOutputStream(first, second);

    @Test
    void shouldWriteInt() throws IOException {
        unit.write(0);

        verify(first).write(0);
        verify(second).write(0);
    }

    @Test
    void shouldWriteArray() throws IOException {
        unit.write(new byte[0]);

        verify(first).write(new byte[0]);
        verify(second).write(new byte[0]);
    }

    @Test
    void shouldWriteArrayWithOffset() throws IOException {
        unit.write(new byte[0], 0, 0);

        verify(first).write(new byte[0], 0, 0);
        verify(second).write(new byte[0], 0, 0);
    }

    @Test
    void shouldFlush() throws IOException {
        unit.flush();

        verify(first).flush();
        verify(second).flush();
    }

    @Test
    void shouldClose() throws IOException {
        unit.close();

        verify(first).close();
        verify(second).close();
    }

    @Test
    void shouldCloseSecondEvenIfFirstFails() throws IOException {
        final IOException expected = new IOException();
        doThrow(expected).when(first).close();

        final IOException actual = assertThrows(IOException.class, unit::close);

        assertSame(expected, actual);

        verify(second).close();
    }

}
