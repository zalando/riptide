package org.zalando.riptide.httpclient;

import com.google.common.io.ByteStreams;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EmptyInputStreamTest {

    private final InputStream unit = EmptyInputStream.EMPTY;

    @Test
    void shouldReadNothing() throws IOException {
        final byte[] bytes = ByteStreams.toByteArray(unit);
        assertEquals(0, bytes.length);
    }

}
