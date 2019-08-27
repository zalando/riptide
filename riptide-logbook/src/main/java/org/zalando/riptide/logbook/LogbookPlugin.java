package org.zalando.riptide.logbook;

import lombok.*;
import org.apiguardian.api.*;
import org.springframework.http.*;
import org.zalando.logbook.*;
import org.zalando.logbook.Logbook.*;
import org.zalando.riptide.*;
import org.zalando.riptide.RequestArguments.*;

import java.io.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.function.*;

import static org.apiguardian.api.API.Status.*;
import static org.zalando.fauxpas.FauxPas.*;

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
