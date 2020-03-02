package org.zalando.riptide.compression;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpOutputMessage;
import org.zalando.fauxpas.ThrowingUnaryOperator;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.OutputStream;

@RequiredArgsConstructor
final class WrappingHttpOutputMessage implements HttpOutputMessage, AutoCloseable {

    private final HttpOutputMessage message;
    private final ThrowingUnaryOperator<OutputStream, IOException> wrapper;
    private OutputStream stream;

    @Nonnull
    @Override
    public OutputStream getBody() throws IOException {
        if (stream == null) {
            stream = wrapper.apply(message.getBody());
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
