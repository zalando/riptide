package org.zalando.riptide.timeout;

import lombok.AllArgsConstructor;
import org.apiguardian.api.API;
import org.springframework.http.client.ClientHttpResponse;
import org.zalando.riptide.Plugin;
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

import static org.zalando.fauxpas.FauxPas.failedWith;

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
    public RequestExecution beforeDispatch(final RequestExecution execution) {
        return arguments -> {
            final CompletableFuture<ClientHttpResponse> upstream = execution.execute(arguments);
            final CompletableFuture<ClientHttpResponse> downstream = upstream.orTimeout(timeout, unit);

            return downstream
                    .whenCompleteAsync(failedWith(TimeoutException.class, e -> upstream.cancel(true)));
        };
    }

}
