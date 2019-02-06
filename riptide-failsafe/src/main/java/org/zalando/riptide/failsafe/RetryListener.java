package org.zalando.riptide.failsafe;

import net.jodah.failsafe.event.ExecutionAttemptedEvent;
import org.apiguardian.api.API;
import org.springframework.http.client.ClientHttpResponse;
import org.zalando.riptide.RequestArguments;

import static org.apiguardian.api.API.Status.EXPERIMENTAL;

@API(status = EXPERIMENTAL)
public interface RetryListener {

    RetryListener DEFAULT = new RetryListener() {
        // nothing to implement, since default methods are sufficient
    };

    default void onRetry(final RequestArguments arguments,
            final ExecutionAttemptedEvent<ClientHttpResponse> event) {
        // nothing to do
    }

}
