package org.zalando.riptide;

import org.junit.Test;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.SettableListenableFuture;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public final class ListenableCompletableFutureAdapterTest {

    @Test
    public void shouldCancel() {
        final ListenableFuture<Void> original = new SettableListenableFuture<>();
        final ListenableCompletableFutureAdapter<Void> unit = new ListenableCompletableFutureAdapter<>(original);

        unit.cancel(true);

        assertThat(original.isCancelled(), is(true));
    }

}