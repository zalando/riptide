package org.zalando.riptide.backup;

import lombok.AllArgsConstructor;
import org.springframework.http.client.ClientHttpResponse;
import org.zalando.fauxpas.ThrowingRunnable;
import org.zalando.riptide.AbstractCancelableCompletableFuture;
import org.zalando.riptide.Plugin;
import org.zalando.riptide.RequestArguments;
import org.zalando.riptide.RequestExecution;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

import static org.zalando.riptide.CancelableCompletableFuture.forwardTo;

@AllArgsConstructor
public final class BackupRequestPlugin implements Plugin {

    private final ScheduledExecutorService scheduler;
    private final long delay;
    private final TimeUnit unit;
    private final Executor executor;

    public BackupRequestPlugin(final ScheduledExecutorService scheduler, final long delay, final TimeUnit unit) {
        this(scheduler, delay, unit, Runnable::run);
    }

    @Override
    public RequestExecution beforeDispatch(final RequestArguments arguments, final RequestExecution execution) {
        switch (arguments.getMethod()) {
            case GET:
            case HEAD:
                return withBackup(execution);
            default:
                return execution;
        }
    }

    private RequestExecution withBackup(final RequestExecution execution) {
        return arguments -> {
            final CompletableFuture<ClientHttpResponse> original = execution.execute(arguments);
            final CompletableFuture<ClientHttpResponse> backup = new CompletableFuture<>();

            final Future<?> scheduledBackup = delay(backup(execution, arguments, backup));

            original.whenComplete(cancel(scheduledBackup));

            return anyOf(original, backup)
                    .whenCompleteAsync(cancel(original), executor);
        };
    }

    private ThrowingRunnable<IOException> backup(final RequestExecution execution,
            final RequestArguments arguments, final CompletableFuture<ClientHttpResponse> target) {
        return () -> execution.execute(arguments).whenCompleteAsync(forwardTo(target), executor);
    }

    private ScheduledFuture<?> delay(final Runnable task) {
        return scheduler.schedule(task, delay, unit);
    }

    private <T> BiConsumer<T, Throwable> cancel(final Future<?> future) {
        return (result, throwable) -> future.cancel(true);
    }

    @SafeVarargs
    private final <T> CompletableFuture<T> anyOf(final CompletableFuture<? extends T>... futures) {
        final CompletableFuture<T> any = new AbstractCancelableCompletableFuture<T>() {
            @Override
            public boolean cancel(final boolean mayInterruptIfRunning) {
                for (final CompletableFuture<? extends T> future : futures) {
                    future.cancel(mayInterruptIfRunning);
                }

                return super.cancel(mayInterruptIfRunning);
            }
        };

        for (final CompletableFuture<? extends T> future : futures) {
            future.whenCompleteAsync(forwardTo(any), executor);
        }

        return any;
    }

}
