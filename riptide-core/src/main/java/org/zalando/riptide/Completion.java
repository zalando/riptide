package org.zalando.riptide;

import lombok.SneakyThrows;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

/**
 * @see "org.zalando.riptide.capture.Completion"
 */
@Deprecated//(since = "2.5.0", forRemoval = true)
public final class Completion {

    Completion() {
        // package private so we can trick code coverage
    }

    /**
     * Joins the given {@link CompletableFuture future} by calling {@link CompletableFuture#join()}. If the given
     * future was completed exceptionally, this method will throw the cause of that {@link CompletionException}.
     *
     * @see CompletableFuture#join()
     * @see CompletionException#getCause()
     * @param future the given future
     * @param <T> generic future result type
     * @return the result of the given future
     * @throws RuntimeException the exception if the given future was completed exceptionally
     */
    @SneakyThrows
    public static <T> T join(final CompletableFuture<T> future) {
        try {
            return future.join();
        } catch (final CompletionException e) {
            throw e.getCause();
        }
    }

}
