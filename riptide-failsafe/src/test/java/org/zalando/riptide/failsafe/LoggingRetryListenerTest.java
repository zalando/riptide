package org.zalando.riptide.failsafe;

import com.google.gag.annotation.remark.Hack;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.springframework.http.client.ClientHttpResponse;
import org.zalando.riptide.RequestArguments;
import org.zalando.riptide.failsafe.FailsafePlugin.RetryListenerAdapter;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

final class LoggingRetryListenerTest {

    private final Logger logger = mock(Logger.class);
    private final RetryListener unit = new LoggingRetryListener(logger);

    @Test
    void shouldLogFailure() {
        final AtomicBoolean success = new AtomicBoolean(false);

        final RequestArguments arguments = RequestArguments.create();
        final IllegalStateException exception = new IllegalStateException();

        Failsafe.with(new RetryPolicy<ClientHttpResponse>()
                .withMaxRetries(3)
                .onRetry(new RetryListenerAdapter(unit, arguments)))
                .run(() -> {
                    if (!success.getAndSet(true)) {
                        throw exception;
                    }
                });

        verify(logger).warn(any(), eq(exception));
    }

    @Test
    void shouldNotLogResults() {
        final AtomicBoolean success = new AtomicBoolean(false);

        final RequestArguments arguments = RequestArguments.create();

        Failsafe.with(new RetryPolicy<ClientHttpResponse>()
                .withMaxRetries(3)
                .handleResultIf(Objects::isNull)
                .onRetry(new RetryListenerAdapter(unit, arguments)))
                .get(() -> {
                    if (!success.getAndSet(true)) {
                        return null;
                    }

                    return mock(ClientHttpResponse.class);
                });

        verifyNoMoreInteractions(logger);
    }

    @Hack("We're not really testing anything here, since we don't want to clutter the logs.")
    @Test
    void shouldUseDefaultLogger() {
        new LoggingRetryListener();
    }

}
