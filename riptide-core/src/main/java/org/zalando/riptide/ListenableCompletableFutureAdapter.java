package org.zalando.riptide;

import org.springframework.util.concurrent.ListenableFuture;

import java.util.concurrent.CompletableFuture;

final class ListenableCompletableFutureAdapter<T> extends CompletableFuture<T> {

    private final ListenableFuture<T> future;

    private ListenableCompletableFutureAdapter(final ListenableFuture<T> future) {
        this.future = future;
    }

    @Override
    public boolean cancel(final boolean mayInterruptIfRunning) {
        final boolean cancelled = future.cancel(mayInterruptIfRunning);
        super.cancel(mayInterruptIfRunning);
        return cancelled;
    }

    static <T> CompletableFuture<T> adapt(final ListenableFuture<T> original) {
        final CompletableFuture<T> future = new ListenableCompletableFutureAdapter<>(original);
        original.addCallback(future::complete, future::completeExceptionally);
        return future;
    }

}
