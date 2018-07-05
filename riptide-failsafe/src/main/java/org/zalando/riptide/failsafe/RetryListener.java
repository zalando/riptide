package org.zalando.riptide.failsafe;

import net.jodah.failsafe.ExecutionContext;
import org.apiguardian.api.API;
import org.springframework.http.client.ClientHttpResponse;
import org.zalando.riptide.RequestArguments;

import javax.annotation.Nullable;

import static org.apiguardian.api.API.Status.EXPERIMENTAL;

@API(status = EXPERIMENTAL)
public interface RetryListener {

    RetryListener DEFAULT = new RetryListener() {
        // nothing to implement, since default methods are sufficient
    };

    default void onRetry(final RequestArguments arguments,
            @Nullable final ClientHttpResponse result, @Nullable final Throwable failure,
            final ExecutionContext context) {
        // nothing to do
    }

}
