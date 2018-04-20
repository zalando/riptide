package org.zalando.riptide.timeout;

import com.google.gag.annotation.remark.ThisWouldBeOneLineIn;
import lombok.AllArgsConstructor;
import org.apiguardian.api.API;
import org.springframework.http.client.ClientHttpResponse;
import org.zalando.riptide.Plugin;
import org.zalando.riptide.RequestArguments;
import org.zalando.riptide.RequestExecution;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
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
@ThisWouldBeOneLineIn(language = "Java 9", toWit = "return () -> execution.execute().orTimeout(timeout, unit)")
public final class TimeoutPlugin implements Plugin {

    private final ScheduledExecutorService scheduler;
    private final long timeout;
    private final TimeUnit unit;
    private final Executor executor;

    public TimeoutPlugin(final ScheduledExecutorService scheduler, final long timeout, final TimeUnit unit) {
        this(scheduler, timeout, unit, Runnable::run);
    }

    @Override
    public RequestExecution prepare(final RequestArguments arguments, final RequestExecution execution) {
        return () -> {
            final CompletableFuture<ClientHttpResponse> upstream = execution.execute();

            final CompletableFuture<ClientHttpResponse> downstream = preserveCancelability(upstream);
            upstream.whenCompleteAsync(forwardTo(downstream), executor);

            final ScheduledFuture<?> scheduledTimeout = delay(timeout(downstream), cancel(upstream));
            upstream.whenCompleteAsync(cancel(scheduledTimeout), executor);

            return downstream;
        };
    }

    private <T> Runnable cancel(final CompletableFuture<T> future) {
        return () -> future.cancel(true);
    }

    private <T> Runnable timeout(final CompletableFuture<T> future) {
        return () -> future.completeExceptionally(new TimeoutException());
    }

    private ScheduledFuture<?> delay(final Runnable... tasks) {
        return scheduler.schedule(run(executor, tasks), timeout, unit);
    }

    private Runnable run(final Executor executor, final Runnable... tasks) {
        return () -> executor.execute(run(tasks));
    }

    private Runnable run(final Runnable... tasks) {
        return () -> stream(tasks).forEach(Runnable::run);
    }

    private <T> BiConsumer<T, Throwable> cancel(final Future<?> future) {
        return (result, throwable) -> future.cancel(true);
    }

}
