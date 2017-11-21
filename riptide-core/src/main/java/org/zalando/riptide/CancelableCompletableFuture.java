package org.zalando.riptide;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.function.BiConsumer;

import static java.util.Objects.nonNull;

public final class CancelableCompletableFuture<T> extends AbstractCancelableCompletableFuture<T> {

    private final Future<?> cause;

    private CancelableCompletableFuture(final Future<?> cause) {
        this.cause = cause;
    }

    @Override
    public boolean cancel(final boolean mayInterruptIfRunning) {
        cause.cancel(mayInterruptIfRunning);
        super.cancel(mayInterruptIfRunning);
        return isCancelled();
    }

    public static <T> CompletableFuture<T> preserveCancelability(final Future<?> future) {
        return new CancelableCompletableFuture<>(future);
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
