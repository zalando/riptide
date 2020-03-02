package org.zalando.riptide.compression;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apiguardian.api.API;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.StreamingHttpOutputMessage;
import org.zalando.riptide.Plugin;
import org.zalando.riptide.RequestArguments.Entity;
import org.zalando.riptide.RequestExecution;

import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.GZIPOutputStream;

import static org.apiguardian.api.API.Status.EXPERIMENTAL;
import static org.springframework.http.HttpHeaders.CONTENT_ENCODING;
import static org.springframework.http.HttpHeaders.TRANSFER_ENCODING;

@API(status = EXPERIMENTAL)
@AllArgsConstructor
public final class RequestCompressionPlugin implements Plugin {

    private final Compression compression;

    public RequestCompressionPlugin() {
        this(Compression.of("gzip", GZIPOutputStream::new));
    }

    @Override
    public RequestExecution aroundNetwork(final RequestExecution execution) {
        return arguments -> {
            final Entity entity = arguments.getEntity();

            if (entity.isEmpty() || arguments.getHeaders().containsKey(CONTENT_ENCODING)) {
                return execution.execute(arguments);
            }

            return execution.execute(
                    arguments.withEntity(new CompressingEntity(compression, entity)));
        };
    }

    @AllArgsConstructor
    private static class CompressingEntity implements Entity {

        private final Compression compression;
        private final Entity entity;

        @Override
        public void writeTo(final HttpOutputMessage message) throws IOException {
            update(message.getHeaders());

            if (message instanceof StreamingHttpOutputMessage) {
                final StreamingHttpOutputMessage streaming = (StreamingHttpOutputMessage) message;
                streaming.setBody(stream ->
                        writeToCompressing(new DelegatingHttpOutputMessage(message.getHeaders(), stream)));
            } else {
                writeToCompressing(message);
            }
        }

        private void writeToCompressing(HttpOutputMessage message) throws IOException {
            try (final WrappingHttpOutputMessage compressingMessage =
                         new WrappingHttpOutputMessage(message, compression.getOutputStreamDecorator())) {
                entity.writeTo(compressingMessage);
            }
        }

        private void update(final HttpHeaders headers) {
            headers.set(CONTENT_ENCODING, compression.getContentEncoding());
            headers.set(TRANSFER_ENCODING, "chunked");
        }

    }

    @AllArgsConstructor
    @Getter
    private static final class DelegatingHttpOutputMessage implements HttpOutputMessage {
        private final HttpHeaders headers;
        private final OutputStream body;
    }

}
