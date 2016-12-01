package org.zalando.riptide.hystrix;

import rx.Observable;

import java.util.concurrent.CompletableFuture;

/**
 * @param <T>
 * @see <a href="https://blog.krecan.net/2015/04/28/converting-rxjava-observables-to-java-8-completable-future-and-back/"/>
 */
final class CompletableFutureObservable<T> extends Observable<T> {

    CompletableFutureObservable(final CompletableFuture<T> future) {
        super(subscriber -> future
                .whenComplete((result, throwable) -> {
                    if (subscriber.isUnsubscribed()) {
                        return;
                    }

                    if (throwable == null) {
                        subscriber.onNext(result);
                        subscriber.onCompleted();
                    } else {
                        subscriber.onError(throwable);
                    }
                }));
    }

}