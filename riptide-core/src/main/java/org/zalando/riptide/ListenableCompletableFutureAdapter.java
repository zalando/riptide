package org.zalando.riptide;

import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.concurrent.ListenableFuture;

import java.util.concurrent.CompletableFuture;

final class ListenableCompletableFutureAdapter extends CompletableFuture<Void> {

    private ListenableFuture<ClientHttpResponse> future;

    public ListenableCompletableFutureAdapter(final ListenableFuture<ClientHttpResponse> future) {
        this.future = future;
    }

    @Override
    public boolean cancel(final boolean mayInterruptIfRunning) {
        final boolean cancelled = future.cancel(mayInterruptIfRunning);
        super.cancel(mayInterruptIfRunning);
        return cancelled;
    }

}
