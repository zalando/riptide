package org.zalando.riptide;

import lombok.AllArgsConstructor;

import java.util.concurrent.Executor;

import static java.util.concurrent.CompletableFuture.supplyAsync;
import static java.util.function.Function.identity;
import static org.zalando.fauxpas.FauxPas.throwingSupplier;

@AllArgsConstructor
final class AsyncPlugin implements Plugin {

    private final Executor executor;

    @Override
    public RequestExecution aroundAsync(final RequestExecution execution) {
        return arguments ->
                supplyAsync(throwingSupplier(() -> execution.execute(arguments)), executor)
                .thenCompose(identity());
    }

}
