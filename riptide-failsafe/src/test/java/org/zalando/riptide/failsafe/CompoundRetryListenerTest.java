package org.zalando.riptide.failsafe;

import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;
import org.junit.Test;
import org.zalando.riptide.RequestArguments;
import org.zalando.riptide.failsafe.FailsafePlugin.RetryListenersAdapter;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public final class CompoundRetryListenerTest {

    private final RetryListener first = mock(RetryListener.class);
    private final RetryListener second = mock(RetryListener.class);

    private final RetryListener unit = new CompoundRetryListener(first, second);

    @Test
    public void shouldPropagateRetryToEveryListener() {
        final AtomicBoolean success = new AtomicBoolean(false);

        final RequestArguments arguments = RequestArguments.create();
        final IllegalStateException exception = new IllegalStateException();

        Failsafe.with(new RetryPolicy().withMaxRetries(3))
                .with(new RetryListenersAdapter(unit, arguments))
                .run(() -> {
                    if (!success.getAndSet(true)) {
                        throw exception;
                    }
                });

        verify(first).onRetry(eq(arguments), isNull(), eq(exception), any());
        verify(second).onRetry(eq(arguments), isNull(), eq(exception), any());
    }

}
