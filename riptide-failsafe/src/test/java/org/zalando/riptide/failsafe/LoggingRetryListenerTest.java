package org.zalando.riptide.failsafe;

import com.google.gag.annotation.remark.*;
import net.jodah.failsafe.*;
import org.junit.jupiter.api.*;
import org.slf4j.*;
import org.springframework.http.client.*;
import org.zalando.riptide.*;
import org.zalando.riptide.failsafe.FailsafePlugin.*;

import java.util.*;
import java.util.concurrent.atomic.*;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

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
