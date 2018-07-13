package org.zalando.riptide.failsafe;

import net.jodah.failsafe.ExecutionContext;
import org.apiguardian.api.API;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.ClientHttpResponse;
import org.zalando.riptide.RequestArguments;

import javax.annotation.Nullable;

import static org.apiguardian.api.API.Status.EXPERIMENTAL;

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
    public void onRetry(final RequestArguments arguments, @Nullable final ClientHttpResponse result,
            @Nullable final Throwable failure, final ExecutionContext context) {

        if (failure != null) {
            logger.warn("Retrying failure", failure);
        }
    }

}
