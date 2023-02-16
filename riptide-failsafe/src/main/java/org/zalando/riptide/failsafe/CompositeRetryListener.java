package org.zalando.riptide.failsafe;

import lombok.AllArgsConstructor;
import dev.failsafe.event.ExecutionAttemptedEvent;
import org.apiguardian.api.API;
import org.springframework.http.client.ClientHttpResponse;
import org.zalando.riptide.RequestArguments;

import java.util.Arrays;
import java.util.Collection;

import static org.apiguardian.api.API.Status.EXPERIMENTAL;

@API(status = EXPERIMENTAL)
@AllArgsConstructor
public final class CompositeRetryListener implements RetryListener {

    private final Collection<RetryListener> listeners;

    public CompositeRetryListener(final RetryListener... listeners) {
        this(Arrays.asList(listeners));
    }

    @Override
    public void onRetry(final RequestArguments arguments,
            final ExecutionAttemptedEvent<ClientHttpResponse> event) {

        listeners.forEach(listener ->
            listener.onRetry(arguments, event));
    }

}
