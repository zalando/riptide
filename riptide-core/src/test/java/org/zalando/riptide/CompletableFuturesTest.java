package org.zalando.riptide;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BiConsumer;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.zalando.riptide.CompletableFutures.forwardTo;

final class CompletableFuturesTest {

    @Test
    void shouldForwardResult() {
        final CompletableFuture<String> future = new CompletableFuture<>();
        final CompletableFuture<String> spy = new CompletableFuture<>();

        future.whenComplete(forwardTo(spy));
        future.complete("foo");

        assertThat(spy.join(), is("foo"));
    }

    @Test
    void shouldForwardException() {
        final CompletableFuture<String> future = new CompletableFuture<>();
        final CompletableFuture<String> spy = new CompletableFuture<>();

        future.whenComplete(forwardTo(spy));
        future.completeExceptionally(new IllegalStateException());

        final CompletionException exception = assertThrows(CompletionException.class, spy::join);

        assertThat(exception.getCause(), is(instanceOf(IllegalStateException.class)));
    }

    @Test
    void shouldChangeExecutorForConsecutiveCallbacks() {
        final ExecutorService executor = Executors.newSingleThreadExecutor();

        final Thread caller = Thread.currentThread();
        final BiConsumer<String, Throwable> action = (r, e) ->
                assertNotEquals(caller, Thread.currentThread());

        final CompletableFuture<String> root = new CompletableFuture<>();

        final CompletableFuture<String> future = root
                .whenCompleteAsync(action, executor)
                .whenComplete(action);

        root.complete("test");
        future.join();
    }

}
