package org.zalando.riptide.logbook;

import lombok.AllArgsConstructor;
import org.apiguardian.api.API;
import org.springframework.http.HttpOutputMessage;
import org.zalando.logbook.Logbook;
import org.zalando.logbook.Logbook.RequestWritingStage;
import org.zalando.logbook.Logbook.ResponseProcessingStage;
import org.zalando.riptide.Plugin;
import org.zalando.riptide.RequestArguments;
import org.zalando.riptide.RequestArguments.Entity;
import org.zalando.riptide.RequestExecution;

import java.io.IOException;
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
    private class LogbookEntity implements Entity {

        private final RequestArguments arguments;
        private final Consumer<ResponseProcessingStage> next;

        @Override
        public void writeTo(final HttpOutputMessage message) throws IOException {
            final LocalRequest request = new LocalRequest(arguments);
            final RequestWritingStage writing = logbook.process(request);
            request.writeTo(message);
            next.accept(writing.write());
        }

        @Override
        public boolean isEmpty() {
            return arguments.getEntity().isEmpty();
        }

    }

}
