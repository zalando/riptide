package org.zalando.riptide;

import lombok.AllArgsConstructor;
import org.springframework.http.client.ClientHttpResponse;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import static org.zalando.fauxpas.FauxPas.throwingRunnable;
import static org.zalando.riptide.CancelableCompletableFuture.forwardTo;

@AllArgsConstructor
final class AsyncPlugin implements Plugin {

    private final Executor executor;

    @Override
    public RequestExecution aroundAsync(final RequestExecution execution) {
        return arguments -> {
            final CompletableFuture<ClientHttpResponse> future = new CompletableFuture<>();

            executor.execute(throwingRunnable(() -> {
                execution.execute(arguments).whenComplete(forwardTo(future));
            }));

            return future;
        };
    }
}
