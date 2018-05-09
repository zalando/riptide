package org.zalando.riptide;

import lombok.SneakyThrows;
import org.apiguardian.api.API;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import static org.apiguardian.api.API.Status.DEPRECATED;

/**
 * @see "org.zalando.riptide.capture.Completion"
 */
@API(status = DEPRECATED, since = "2.5.0")
@Deprecated//(since = "2.5.0", forRemoval = true)
public final class Completion {

    private Completion() {

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
