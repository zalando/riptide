package org.zalando.riptide;

import org.junit.jupiter.api.Test;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureTask;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.zalando.riptide.CancelableCompletableFuture.forwardTo;
import static org.zalando.riptide.CancelableCompletableFuture.preserveCancelability;

final class CancelableCompletableFutureTest {

    @Test
    void shouldCancel() {
        final ListenableFuture<Void> original = new ListenableFutureTask<>(() -> {}, null);
        final CompletableFuture<Void> unit = preserveCancelability(original);
        original.addCallback(unit::complete, unit::completeExceptionally);

        unit.cancel(true);

        assertThat(original.isCancelled(), is(true));
    }

    // TODO: this test is actually fake and should be rewritten for Java 9
    @Test
    void shouldPreserveCancelability() {
        final AbstractCancelableCompletableFuture<String> unit = new AbstractCancelableCompletableFuture<String>() {
        };

        assertThat(unit.newIncompleteFuture(), is(instanceOf(CancelableCompletableFuture.class)));
    }

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
