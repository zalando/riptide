package org.zalando.riptide.failsafe;

import net.jodah.failsafe.event.*;
import org.apiguardian.api.*;
import org.springframework.http.client.*;
import org.zalando.riptide.*;

import java.util.*;

import static org.apiguardian.api.API.Status.*;

@API(status = EXPERIMENTAL)
public final class CompositeRetryListener implements RetryListener {

    private final Collection<RetryListener> listeners;

    public CompositeRetryListener(final RetryListener... listeners) {
        this(Arrays.asList(listeners));
    }

    public CompositeRetryListener(final Collection<RetryListener> listeners) {
        this.listeners = listeners;
    }

    @Override
    public void onRetry(final RequestArguments arguments,
            final ExecutionAttemptedEvent<ClientHttpResponse> event) {

        listeners.forEach(listener ->
            listener.onRetry(arguments, event));
    }

}
