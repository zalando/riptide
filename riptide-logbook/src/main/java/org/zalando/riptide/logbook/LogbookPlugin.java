package org.zalando.riptide.logbook;

import lombok.AllArgsConstructor;
import org.apiguardian.api.API;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.StreamingHttpOutputMessage;
import org.zalando.logbook.Logbook;
import org.zalando.logbook.Logbook.RequestWritingStage;
import org.zalando.logbook.Logbook.ResponseProcessingStage;
import org.zalando.riptide.Plugin;
import org.zalando.riptide.RequestArguments;
import org.zalando.riptide.RequestArguments.Entity;
import org.zalando.riptide.RequestExecution;

import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.apiguardian.api.API.Status.EXPERIMENTAL;
import static org.zalando.fauxpas.FauxPas.throwingConsumer;

@API(status = EXPERIMENTAL)
@AllArgsConstructor
public final class LogbookPlugin implements Plugin {

    private final Logbook logbook;

    @Override
    public RequestExecution aroundNetwork(final RequestExecution execution) {
        return arguments -> {
            // TODO is there a better way?!
            final AtomicReference<ResponseProcessingStage> stage = new AtomicReference<>();

            final CompletableFuture<RemoteResponse> future = execution
                    .execute(arguments.withEntity(new LogbookEntity(arguments, stage::set)))
                    .thenApply(RemoteResponse::new);

            future.thenAccept(throwingConsumer(response ->
                    stage.get().process(response).write()));

            return future.thenApply(RemoteResponse::asClientHttpResponse);
        };
    }

    @AllArgsConstructor
    private final class LogbookEntity implements Entity {

        private final Entity entity;
        private final RequestArguments arguments;
        private final Consumer<ResponseProcessingStage> next;

        LogbookEntity(
                final RequestArguments arguments,
                final Consumer<ResponseProcessingStage> next) {
            this(arguments.getEntity(), arguments, next);
        }

        @Override
        public void writeTo(final HttpOutputMessage message) throws IOException {
            final Process process = process(message);

            if (entity.isEmpty()) {
                process.close();
                return;
            }

            if (message instanceof StreamingHttpOutputMessage) {
                final StreamingHttpOutputMessage streaming =
                        (StreamingHttpOutputMessage) message;
                streaming.setBody(process::writeTo);
            } else {
                process.writeTo(message.getBody());
            }
        }

        private Process process(final HttpOutputMessage message) throws IOException {
            final LocalRequest request = new LocalRequest(arguments);
            final HttpHeaders headers = message.getHeaders();
            final RequestWritingStage writing = logbook.process(request);
            return new Process(request, headers, writing);
        }

        @Override
        public boolean isEmpty() {
            return entity.isEmpty();
        }

        @AllArgsConstructor
        private final class Process implements Closeable {

            private final LocalRequest request;
            private final HttpHeaders headers;
            private final RequestWritingStage writing;

            void writeTo(final OutputStream stream) throws IOException {
                final HttpOutputMessage message =
                        new SimpleHttpOutputMessage(headers, stream);
                request.writeTo(entity, message);
                close();
            }

            @Override
            public void close() throws IOException {
                next.accept(writing.write());
            }

        }

    }

}
