package org.zalando.riptide;

import org.apiguardian.api.API;

import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

import static java.util.Objects.nonNull;
import static org.apiguardian.api.API.Status.EXPERIMENTAL;

@API(status = EXPERIMENTAL)
public final class CompletableFutures {

    private CompletableFutures() {

    }

    public static <T> CompletableFuture<T> exceptionallyCompletedFuture(final Throwable throwable) {
        final CompletableFuture<T> future = new CompletableFuture<>();
        future.completeExceptionally(throwable);
        return future;
    }

    public static <T> BiConsumer<T, Throwable> forwardTo(final CompletableFuture<T> future) {
        return (response, throwable) -> {
            if (nonNull(response)) {
                future.complete(response);
            }
            if (nonNull(throwable)) {
                future.completeExceptionally(throwable);
            }
        };
    }

}
