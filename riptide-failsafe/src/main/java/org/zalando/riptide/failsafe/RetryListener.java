package org.zalando.riptide.failsafe;

import net.jodah.failsafe.event.*;
import org.apiguardian.api.*;
import org.springframework.http.client.*;
import org.zalando.riptide.*;

import static org.apiguardian.api.API.Status.*;

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
