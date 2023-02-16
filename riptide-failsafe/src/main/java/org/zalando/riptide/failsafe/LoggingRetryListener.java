package org.zalando.riptide.failsafe;

import lombok.AllArgsConstructor;
import dev.failsafe.event.ExecutionAttemptedEvent;
import org.apiguardian.api.API;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.ClientHttpResponse;
import org.zalando.riptide.RequestArguments;

import static org.apiguardian.api.API.Status.EXPERIMENTAL;

@API(status = EXPERIMENTAL)
@AllArgsConstructor
public final class LoggingRetryListener implements RetryListener {

    private final Logger logger;

    public LoggingRetryListener() {
        this(LoggerFactory.getLogger(LoggingRetryListener.class));
    }

    @Override
    public void onRetry(final RequestArguments arguments,
            final ExecutionAttemptedEvent<ClientHttpResponse> event) {

        if (event.getLastFailure() != null) {
            logger.warn("Retrying failure", event.getLastFailure());
        }
    }

}
