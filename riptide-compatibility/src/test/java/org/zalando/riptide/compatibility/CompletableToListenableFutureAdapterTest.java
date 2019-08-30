package org.zalando.riptide.compatibility;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.util.concurrent.FailureCallback;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;
import org.springframework.util.concurrent.SuccessCallback;

import java.util.concurrent.CompletableFuture;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CompletableToListenableFutureAdapterTest {

    private final CompletableFuture<String> future = new CompletableFuture<>();

    private final ListenableFuture<String> unit =
            new CompletableToListenableFutureAdapter<>(future);

    @Mock
    private SuccessCallback<String> success;

    private final FailureCallback failure = mock(FailureCallback.class);

    @Mock
    private ListenableFutureCallback<String> callback;

    @Test
    void shouldCallSuccessCallback() {
        future.complete("test");
        unit.addCallback(success, failure);
        verify(success).onSuccess("test");
    }

    @Test
    void shouldCallFailureCallback() {
        final AssertionError error = new AssertionError();
        future.completeExceptionally(error);
        unit.addCallback(success, failure);
        verify(failure).onFailure(error);
    }

    @Test
    void shouldCallCombinedSuccessCallback() {
        future.complete("test");
        unit.addCallback(callback);
        verify(callback).onSuccess("test");
    }

    @Test
    void shouldCallCombinedFailureCallback() {
        final AssertionError error = new AssertionError();
        future.completeExceptionally(error);
        unit.addCallback(callback);
        verify(callback).onFailure(error);
    }

}
