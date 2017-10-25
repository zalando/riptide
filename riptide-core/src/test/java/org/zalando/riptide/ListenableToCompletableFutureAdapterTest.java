package org.zalando.riptide;

import org.junit.Test;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureTask;

import java.util.concurrent.CompletableFuture;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public final class ListenableToCompletableFutureAdapterTest {

    @Test
    public void shouldCancel() {
        final ListenableFuture<Void> original = new ListenableFutureTask<Void>(() -> {}, null);
        final CompletableFuture<Void> unit = new ListenableToCompletableFutureAdapter<>(original);

        unit.cancel(true);

        assertThat(original.isCancelled(), is(true));
    }

}
