package org.zalando.riptide;

import org.springframework.util.concurrent.ListenableFuture;

import java.util.concurrent.CompletableFuture;

public final class ListenableToCompletableFutureAdapter<T> extends CompletableFuture<T> {

    private final ListenableFuture<T> future;

    public ListenableToCompletableFutureAdapter(final ListenableFuture<T> future) {
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
