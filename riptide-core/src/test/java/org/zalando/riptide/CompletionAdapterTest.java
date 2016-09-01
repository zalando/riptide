package org.zalando.riptide;

import org.junit.Test;

import java.util.concurrent.CompletableFuture;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public final class CompletionAdapterTest {

    @SuppressWarnings("unchecked")
    private final CompletableFuture<Void> future = mock(CompletableFuture.class);

    private final Completion<Void> unit = Completion.valueOf(future);

    @Test
    public void shouldReturnDelegateAsCompletableFuture() {
        assertThat(unit.toCompletableFuture(), is(sameInstance(future)));
    }

    @Test
    public void shouldDelegateJoin() {
        unit.join();
        verify(future).join();
    }

    @Test
    public void shouldDelegateGetNow() {
        unit.getNow(null);
        verify(future).getNow(null);
    }

}