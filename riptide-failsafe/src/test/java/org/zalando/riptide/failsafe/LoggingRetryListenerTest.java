package org.zalando.riptide.failsafe;

import com.google.gag.annotation.remark.Hack;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;
import org.junit.Test;
import org.slf4j.Logger;
import org.zalando.riptide.RequestArguments;
import org.zalando.riptide.failsafe.FailsafePlugin.RetryListenersAdapter;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

public final class LoggingRetryListenerTest {

    private final Logger logger = mock(Logger.class);
    private final RetryListener unit = new LoggingRetryListener(logger);

    @Test
    public void shouldLogFailure() {
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

        verify(logger).warn(any(), eq(exception));
    }

    @Test
    public void shouldNotLogResults() {
        final AtomicBoolean success = new AtomicBoolean(false);

        final RequestArguments arguments = RequestArguments.create();

        Failsafe.with(new RetryPolicy()
                    .withMaxRetries(3)
                    .retryIf(Objects::isNull))
                .with(new RetryListenersAdapter(unit, arguments))
                .get(() -> {
                    if (!success.getAndSet(true)) {
                        return null;
                    }

                    return "not null";
                });

        verifyNoMoreInteractions(logger);
    }

    @Hack("We're not really testing anything here, since we don't want to clutter the logs.")
    @Test
    public void shouldUseDefaultLogger() {
        new LoggingRetryListener();
    }

}
