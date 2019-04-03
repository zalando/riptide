package org.zalando.riptide;

import lombok.AllArgsConstructor;
import org.springframework.http.client.ClientHttpResponse;

import java.util.concurrent.CompletableFuture;

import static org.zalando.riptide.CompletableFutures.exceptionallyCompletedFuture;

@AllArgsConstructor
final class GuardedRequestExecution implements RequestExecution {

    private final RequestExecution execution;

    @Override
    public CompletableFuture<ClientHttpResponse> execute(final RequestArguments arguments) {
        try {
            return execution.execute(arguments);
        } catch (final Exception e) {
            return exceptionallyCompletedFuture(e);
        }
    }

}
