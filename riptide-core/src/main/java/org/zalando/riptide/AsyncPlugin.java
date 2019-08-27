package org.zalando.riptide;

import lombok.*;

import java.util.concurrent.*;

import static java.util.concurrent.CompletableFuture.*;
import static java.util.function.Function.*;
import static org.zalando.fauxpas.FauxPas.*;

@AllArgsConstructor
final class AsyncPlugin implements Plugin {

    private final Executor executor;

    @Override
    public RequestExecution aroundAsync(final RequestExecution execution) {
        return arguments ->
                supplyAsync(throwingSupplier(() -> execution.execute(arguments)), executor)
                .thenCompose(identity());
    }

    @Override
    public RequestExecution aroundNetwork(final RequestExecution execution) {
        return arguments ->
                execution.execute(arguments)
                        .whenCompleteAsync((r, e) -> {
                            // this will force any further callbacks to be executed using the executor
                        }, executor);
    }
}
