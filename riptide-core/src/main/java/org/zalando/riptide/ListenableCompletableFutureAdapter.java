package org.zalando.riptide;

import org.springframework.util.concurrent.ListenableFuture;

import java.util.concurrent.CompletableFuture;

final class ListenableCompletableFutureAdapter<T> extends CompletableFuture<T> {

    private ListenableFuture<T> future;

    public ListenableCompletableFutureAdapter(final ListenableFuture<T> future) {
        this.future = future;
        future.addCallback(this::complete, this::completeExceptionally);
    }

    @Override
    public boolean cancel(final boolean mayInterruptIfRunning) {
        final boolean cancelled = future.cancel(mayInterruptIfRunning);
        super.cancel(mayInterruptIfRunning);
        return cancelled;
    }

}
