package org.zalando.riptide.timeout;

import lombok.AllArgsConstructor;
import org.apiguardian.api.API;
import org.springframework.http.client.ClientHttpResponse;
import org.zalando.riptide.Plugin;
import org.zalando.riptide.RequestArguments;
import org.zalando.riptide.RequestExecution;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BiConsumer;

import static java.util.Arrays.stream;
import static org.apiguardian.api.API.Status.STABLE;
import static org.zalando.riptide.CancelableCompletableFuture.forwardTo;
import static org.zalando.riptide.CancelableCompletableFuture.preserveCancelability;

/**
 * @see "CompletableFuture#orTimeout(long, TimeUnit)"
 */
@API(status = STABLE)
@AllArgsConstructor
public final class TimeoutPlugin implements Plugin {

    private final long timeout;
    private final TimeUnit unit;
    private final Executor executor;

    public TimeoutPlugin(final long timeout, final TimeUnit unit) {
        this(timeout, unit, Runnable::run);
    }

    @Override
    public RequestExecution beforeDispatch(final RequestArguments originalArguments, final RequestExecution execution) {
        return arguments -> {
            final CompletableFuture<ClientHttpResponse> upstream = execution.execute(arguments);
            final CompletableFuture<ClientHttpResponse> downstream = upstream.orTimeout(timeout, unit);

            return downstream.whenCompleteAsync((response, throwable) -> {
                // TODO make sure this works with nested exceptions
                if (throwable instanceof TimeoutException) {
                    upstream.cancel(true);
                }
            }, executor);
        };
    }

}
