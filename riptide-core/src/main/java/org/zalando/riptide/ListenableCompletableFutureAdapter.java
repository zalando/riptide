package org.zalando.riptide;

import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;

import java.util.concurrent.CompletableFuture;

final class ListenableCompletableFutureAdapter<T> extends CompletableFuture<T> {

    private ListenableFuture<T> future;

    ListenableCompletableFutureAdapter(final ListenableFuture<T> future) {
        this.future = future;
        future.addCallback(new ListenableFutureCallback<T>() {
            @Override
            public void onSuccess(final T result) {
                complete(result);
            }

            @Override
            public void onFailure(final Throwable throwable) {
                completeExceptionally(throwable);
            }
        });
    }

    @Override
    public boolean cancel(final boolean mayInterruptIfRunning) {
        final boolean cancelled = future.cancel(mayInterruptIfRunning);
        super.cancel(mayInterruptIfRunning);
        return cancelled;
    }

}
