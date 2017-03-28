package org.zalando.riptide.hystrix;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;
import rx.Observable;
import rx.Subscriber;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public final class CompletableFutureObservableTest<T> {

    @Spy
    private Subscriber<T> subscriber = new Subscriber<T>() {
        @Override
        public void onCompleted() {

        }

        @Override
        public void onError(final Throwable e) {

        }

        @Override
        public void onNext(final T t) {

        }
    };

    @Test
    public void shouldSkipUnsubscribed() {
        final CompletableFuture<T> future = new CompletableFuture<>();
        final Observable<T> unit = new CompletableFutureObservable<>(future);

        unit.subscribe(subscriber).unsubscribe();

        future.complete(null);

        verify(subscriber, never()).onNext(any());
        verify(subscriber, never()).onCompleted();
        verify(subscriber, never()).onError(any());
    }

    @Test
    public void shouldPropagateResult() {
        final CompletableFuture<T> future = new CompletableFuture<>();
        final Observable<T> unit = new CompletableFutureObservable<>(future);

        unit.subscribe(subscriber);

        future.complete(null);

        verify(subscriber).onNext(null);
        verify(subscriber).onCompleted();
        verify(subscriber, never()).onError(any());
    }

    @Test
    public void shouldPropagateError() {
        final CompletableFuture<T> future = new CompletableFuture<>();
        final Observable<T> unit = new CompletableFutureObservable<>(future);

        unit.subscribe(subscriber);

        final IOException exception = new IOException();

        future.completeExceptionally(exception);

        verify(subscriber, never()).onNext(any());
        verify(subscriber, never()).onCompleted();
        verify(subscriber).onError(exception);
    }

}