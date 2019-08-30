package org.zalando.riptide.compatibility;

import lombok.experimental.Delegate;
import org.springframework.util.concurrent.FailureCallback;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;
import org.springframework.util.concurrent.ListenableFutureCallbackRegistry;
import org.springframework.util.concurrent.SuccessCallback;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

import static java.util.Objects.nonNull;

final class CompletableToListenableFutureAdapter<T> implements ListenableFuture<T> {

    @Delegate
    @SuppressWarnings({"PMD.UnusedPrivateField", "PMD.SingularField"})
    private final Future<T> future;

    private final ListenableFutureCallbackRegistry<T> callbacks = 
            new ListenableFutureCallbackRegistry<>();

    CompletableToListenableFutureAdapter(final CompletableFuture<T> future) {
        this.future = future.whenComplete((result, e) -> {
            if (nonNull(result)) {
                callbacks.success(result);
            }
            
            if (nonNull(e)) {
                callbacks.failure(e);
            }
        });
    }

    @Override
    public void addCallback(final ListenableFutureCallback<? super T> callback) {
        callbacks.addCallback(callback);
    }

    @Override
    public void addCallback(final SuccessCallback<? super T> successCallback, final FailureCallback failureCallback) {
        callbacks.addSuccessCallback(successCallback);
        callbacks.addFailureCallback(failureCallback);
    }

}

