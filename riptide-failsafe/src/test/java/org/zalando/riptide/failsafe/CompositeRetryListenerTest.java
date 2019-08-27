package org.zalando.riptide.failsafe;

import net.jodah.failsafe.*;
import net.jodah.failsafe.event.*;
import org.junit.jupiter.api.*;
import org.springframework.http.client.*;
import org.zalando.riptide.*;

import java.util.concurrent.atomic.*;

import static org.hamcrest.Matchers.*;
import static org.hobsoft.hamcrest.compose.ComposeMatchers.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
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
                .onRetry(new FailsafePlugin.RetryListenerAdapter(unit, arguments)))
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
