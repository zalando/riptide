package org.zalando.riptide.hystrix;

import rx.Observable;
import rx.Subscription;

import java.util.concurrent.CompletableFuture;

/**
 * @see <a href="https://blog.krecan.net/2015/04/28/converting-rxjava-observables-to-java-8-completable-future-and-back/"/>
 * @param <T>
 */
final class ObservableCompletableFuture<T> extends CompletableFuture<T> {

    private final Subscription subscription;

    ObservableCompletableFuture(final Observable<T> observable) {
        this.subscription = observable.subscribe(this::complete, this::completeExceptionally);
    }

    @Override
    public boolean cancel(final boolean mayInterruptIfRunning) {
        subscription.unsubscribe();
        return super.cancel(mayInterruptIfRunning);
    }

}
