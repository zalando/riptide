package org.zalando.riptide.failsafe;

import net.jodah.failsafe.ExecutionContext;
import org.apiguardian.api.API;
import org.springframework.http.client.ClientHttpResponse;
import org.zalando.riptide.RequestArguments;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Collection;

import static org.apiguardian.api.API.Status.EXPERIMENTAL;

@API(status = EXPERIMENTAL)
public final class CompoundRetryListener implements RetryListener {

    private final Collection<RetryListener> listeners;

    public CompoundRetryListener(final RetryListener... listeners) {
        this(Arrays.asList(listeners));
    }

    public CompoundRetryListener(final Collection<RetryListener> listeners) {
        this.listeners = listeners;
    }

    @Override
    public void onRetry(final RequestArguments arguments, @Nullable final ClientHttpResponse result,
            @Nullable final Throwable failure, final ExecutionContext context) {

        listeners.forEach(listener ->
            listener.onRetry(arguments, result, failure, context));
    }

}
