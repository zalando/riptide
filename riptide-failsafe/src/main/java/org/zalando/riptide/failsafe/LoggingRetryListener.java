package org.zalando.riptide.failsafe;

import net.jodah.failsafe.event.*;
import org.apiguardian.api.*;
import org.slf4j.*;
import org.springframework.http.client.*;
import org.zalando.riptide.*;

import static org.apiguardian.api.API.Status.*;

@API(status = EXPERIMENTAL)
public final class LoggingRetryListener implements RetryListener {

    private final Logger logger;

    public LoggingRetryListener() {
        this(LoggerFactory.getLogger(LoggingRetryListener.class));
    }

    public LoggingRetryListener(final Logger logger) {
        this.logger = logger;
    }

    @Override
    public void onRetry(final RequestArguments arguments,
            final ExecutionAttemptedEvent<ClientHttpResponse> event) {

        if (event.getLastFailure() != null) {
            logger.warn("Retrying failure", event.getLastFailure());
        }
    }

}
