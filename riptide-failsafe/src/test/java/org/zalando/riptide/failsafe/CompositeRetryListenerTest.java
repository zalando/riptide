package org.zalando.riptide.failsafe;

import dev.failsafe.Failsafe;
import dev.failsafe.RetryPolicy;
import dev.failsafe.event.ExecutionAttemptedEvent;
import org.junit.jupiter.api.Test;
import org.springframework.http.client.ClientHttpResponse;
import org.zalando.riptide.RequestArguments;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hobsoft.hamcrest.compose.ComposeMatchers.hasFeature;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;

final class CompositeRetryListenerTest {

    private final RetryListener first = mock(RetryListener.class);
    private final RetryListener second = mock(RetryListener.class);

    private final RetryListener unit = new CompositeRetryListener(first, second);

    @Test
    void shouldPropagateRetryToEveryListener() {
        final AtomicBoolean success = new AtomicBoolean(false);

        final RequestArguments arguments = RequestArguments.create();
        final IllegalStateException exception = new IllegalStateException();

        Failsafe.with(new RetryPolicy<ClientHttpResponse>()
                .withMaxRetries(3)
                .onRetry(new RetryRequestPolicy.RetryListenerAdapter(unit, arguments)))
                .run(() -> {
                    if (!success.getAndSet(true)) {
                        throw exception;
                    }
                });

        verify(first).onRetry(eq(arguments), argThat(hasFeature(ExecutionAttemptedEvent::getLastResult, nullValue())));
        verify(first).onRetry(eq(arguments), argThat(hasFeature(ExecutionAttemptedEvent::getLastFailure, notNullValue())));
        verify(second).onRetry(eq(arguments), argThat(hasFeature(ExecutionAttemptedEvent::getLastResult, nullValue())));
        verify(second).onRetry(eq(arguments), argThat(hasFeature(ExecutionAttemptedEvent::getLastFailure, notNullValue())));
    }

}
