package org.zalando.riptide;

import lombok.AllArgsConstructor;
import org.apiguardian.api.API;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpOutputMessage;
import org.zalando.riptide.RequestArguments.Entity;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.GZIPOutputStream;

import static org.apiguardian.api.API.Status.EXPERIMENTAL;

@API(status = EXPERIMENTAL)
public final class RequestCompressionPlugin implements Plugin {

    @Override
    public RequestExecution aroundNetwork(final RequestExecution execution) {
        return arguments -> {
            final Entity entity = arguments.getEntity();

            if (entity.isEmpty()) {
                return execution.execute(arguments);
            }

            return execution.execute(
                    arguments.withEntity(new GzipEntity(entity)));
        };
    }

    @AllArgsConstructor
    private static class GzipEntity implements Entity {

        private final Entity entity;

        @Override
        public void writeTo(final HttpOutputMessage message) throws IOException {
            entity.writeTo(new GzipHttpOutputMessage(message));
            update(message.getHeaders());
        }

        private void update(final HttpHeaders headers) {
            headers.set("Content-Encoding", "gzip");
            headers.set("Transfer-Encoding", "chunked");
        }

    }

    @AllArgsConstructor
    private static final class GzipHttpOutputMessage implements HttpOutputMessage {

        private final HttpOutputMessage message;

        @Nonnull
        @Override
        public OutputStream getBody() throws IOException {
            return new GZIPOutputStream(message.getBody());
        }

        @Nonnull
        @Override
        public HttpHeaders getHeaders() {
            return message.getHeaders();
        }

    }

}
