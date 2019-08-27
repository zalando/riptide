package org.zalando.riptide;

import lombok.*;
import org.apiguardian.api.*;
import org.springframework.http.*;
import org.zalando.riptide.RequestArguments.*;

import javax.annotation.*;
import java.io.*;
import java.util.zip.*;

import static org.apiguardian.api.API.Status.*;

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
