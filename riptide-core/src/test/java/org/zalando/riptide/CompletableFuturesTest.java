package org.zalando.riptide;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
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

}
