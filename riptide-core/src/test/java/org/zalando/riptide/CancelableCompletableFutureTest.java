package org.zalando.riptide;

import org.junit.Test;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureTask;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.zalando.riptide.CancelableCompletableFuture.forwardTo;
import static org.zalando.riptide.CancelableCompletableFuture.preserveCancelability;

public final class CancelableCompletableFutureTest {

    @Test
    public void shouldCancel() {
        final ListenableFuture<Void> original = new ListenableFutureTask<>(() -> {}, null);
        final CompletableFuture<Void> unit = preserveCancelability(original);
        original.addCallback(unit::complete, unit::completeExceptionally);

        unit.cancel(true);

        assertThat(original.isCancelled(), is(true));
    }

    // TODO: this test is actually fake and should be rewritten for Java 9
    @Test
    public void shouldPreserveCancelability() {
        final AbstractCancelableCompletableFuture<String> unit = new AbstractCancelableCompletableFuture<String>() {
        };

        assertThat(unit.newIncompleteFuture(), is(instanceOf(CancelableCompletableFuture.class)));
    }

    @Test
    public void shouldForwardResult() {
        final CompletableFuture<String> future = new CompletableFuture<>();
        final CompletableFuture<String> spy = new CompletableFuture<>();

        future.whenComplete(forwardTo(spy));
        future.complete("foo");

        assertThat(spy.join(), is("foo"));
    }

    @Test(expected = IllegalStateException.class)
    public void shouldForwardException() throws Throwable {
        final CompletableFuture<String> future = new CompletableFuture<>();
        final CompletableFuture<String> spy = new CompletableFuture<>();

        future.whenComplete(forwardTo(spy));
        future.completeExceptionally(new IllegalStateException());

        try {
            spy.join();
        } catch (final CompletionException e) {
            throw e.getCause();
        }
    }

}
