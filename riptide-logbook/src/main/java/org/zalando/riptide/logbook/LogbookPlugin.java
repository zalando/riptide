package org.zalando.riptide.logbook;

import lombok.AllArgsConstructor;
import org.apiguardian.api.API;
import org.zalando.logbook.Logbook;
import org.zalando.logbook.Logbook.RequestWritingStage;
import org.zalando.logbook.Logbook.ResponseProcessingStage;
import org.zalando.riptide.Plugin;
import org.zalando.riptide.RequestArguments;
import org.zalando.riptide.RequestExecution;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

import static org.apiguardian.api.API.Status.EXPERIMENTAL;
import static org.zalando.fauxpas.FauxPas.throwingConsumer;

@API(status = EXPERIMENTAL)
@AllArgsConstructor
public final class LogbookPlugin implements Plugin {

    private final Logbook logbook;

    @Override
    public RequestExecution aroundNetwork(final RequestExecution execution) {
        return original -> {
            // TODO is there a better way?!
            final AtomicReference<ResponseProcessingStage> responseStage = new AtomicReference<>();

            final RequestArguments arguments = original.withEntity(message -> {
                final LocalRequest request = new LocalRequest(original);
                final RequestWritingStage stage = logbook.process(request);
                request.writeTo(message);
                responseStage.set(stage.write());
            });

            final CompletableFuture<RemoteResponse> future = execution.execute(arguments)
                    .thenApply(RemoteResponse::new);

            future.thenAccept(throwingConsumer(response ->
                    responseStage.get().process(response).write()));

            return future.thenApply(RemoteResponse::asClientHttpResponse);
        };
    }

}
