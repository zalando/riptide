package org.zalando.riptide.micrometer;

import org.zalando.fauxpas.ThrowingBiConsumer;
import org.zalando.fauxpas.ThrowingConsumer;

import static java.util.Objects.nonNull;

final class CompletableFutures {

    private CompletableFutures() {
        // nothing to do
    }

    static <T, X extends Exception> ThrowingBiConsumer<T, Throwable, X> onResult(
            final ThrowingConsumer<T, X> consumer) {

        return (result, throwable) -> {
            if (nonNull(result)) {
                consumer.tryAccept(result);
            }
        };
    }

    static <T, X extends Exception> ThrowingBiConsumer<T, Throwable, X> onError(
            final ThrowingConsumer<Throwable, X> consumer) {

        return (result, throwable) -> {
            if (nonNull(throwable)) {
                consumer.tryAccept(throwable);
            }
        };
    }

}
