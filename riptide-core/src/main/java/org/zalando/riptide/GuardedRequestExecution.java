package org.zalando.riptide;

import lombok.*;
import org.springframework.http.client.*;

import java.util.concurrent.*;

import static org.zalando.riptide.CompletableFutures.*;

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
