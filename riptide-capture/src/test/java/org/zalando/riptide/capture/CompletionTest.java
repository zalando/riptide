package org.zalando.riptide.capture;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.zalando.riptide.capture.Completion.join;

final class CompletionTest {

    @Test
    void shouldJoinSuccessfully() {
        assertThat(join(completedFuture("test")), is("test"));
    }

    @Test
    void shouldPropagateCause() {
        final CompletableFuture<Void> future = new CompletableFuture<>();
        final IOException e = new IOException();
        future.completeExceptionally(e);

        final Exception exception = assertThrows(Exception.class, () -> join(future));

        assertThat(exception, is(sameInstance(e)));
    }

}
