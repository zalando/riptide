package org.zalando.riptide.compression;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpOutputMessage;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.OutputStream;

final class WrappingHttpOutputMessage implements HttpOutputMessage, AutoCloseable {

    private final HttpOutputMessage message;
    private final Compression.OutputStreamDecorator wrapper;
    private OutputStream stream;

    WrappingHttpOutputMessage(HttpOutputMessage message, Compression.OutputStreamDecorator wrapper) {
        this.message = message;
        this.wrapper = wrapper;
    }

    @Nonnull
    @Override
    public OutputStream getBody() throws IOException {
        if (stream == null) {
            stream = wrapper.wrap(message.getBody());
        }
        return stream;
    }

    @Nonnull
    @Override
    public HttpHeaders getHeaders() {
        return message.getHeaders();
    }

    @Override
    public void close() throws IOException {
        // make sure any underlying compressor gets flushed
        if (stream != null) {
            stream.close();
        }
    }
}
