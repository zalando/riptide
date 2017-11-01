package org.zalando.riptide.timeout;

import com.google.gag.annotation.remark.ThisWouldBeOneLineIn;
import org.springframework.http.client.ClientHttpResponse;
import org.zalando.riptide.Plugin;
import org.zalando.riptide.RequestArguments;
import org.zalando.riptide.RequestExecution;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BiConsumer;

/**
 * @see CompletableFuture#orTimeout(long, TimeUnit)
 */
@ThisWouldBeOneLineIn(language = "Java 9", toWit = "return () -> execution.execute().orTimeout(timeout, unit)")
public final class TimeoutPlugin implements Plugin {

    private final ScheduledExecutorService scheduler;
    private final long timeout;
    private final TimeUnit unit;

    public TimeoutPlugin(final ScheduledExecutorService scheduler, final long timeout,
            final TimeUnit unit) {
        this.scheduler = scheduler;
        this.timeout = timeout;
        this.unit = unit;
    }

    @Override
    public RequestExecution prepare(final RequestArguments arguments, final RequestExecution execution) {
        return () -> {
            final CompletableFuture<ClientHttpResponse> future = execution.execute();
            final ScheduledFuture<?> scheduledTimeout = delay(timeout(future));
            future.whenComplete(cancel(scheduledTimeout));
            return future;
        };
    }

    private <T> Runnable timeout(final CompletableFuture<T> future) {
        return () -> future.completeExceptionally(new TimeoutException());
    }

    private ScheduledFuture<?> delay(final Runnable runnable) {
        return scheduler.schedule(runnable, timeout, unit);
    }

    private <T> BiConsumer<T, Throwable> cancel(final Future<?> future) {
        return (result, throwable) -> future.cancel(false);
    }

}
