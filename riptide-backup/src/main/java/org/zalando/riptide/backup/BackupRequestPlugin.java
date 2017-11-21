package org.zalando.riptide.backup;

import org.springframework.http.client.ClientHttpResponse;
import org.zalando.fauxpas.ThrowingRunnable;
import org.zalando.riptide.AbstractCancelableCompletableFuture;
import org.zalando.riptide.Plugin;
import org.zalando.riptide.RequestArguments;
import org.zalando.riptide.RequestExecution;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

import static org.zalando.riptide.CancelableCompletableFuture.forwardTo;

public final class BackupRequestPlugin implements Plugin {

    private final ScheduledExecutorService scheduler;
    private final long delay;
    private final TimeUnit unit;

    public BackupRequestPlugin(final ScheduledExecutorService scheduler, final long delay, final TimeUnit unit) {
        this.scheduler = scheduler;
        this.delay = delay;
        this.unit = unit;
    }

    @Override
    public RequestExecution prepare(final RequestArguments arguments, final RequestExecution execution) {
        switch (arguments.getMethod()) {
            case GET:
            case HEAD:
                return withBackup(execution);
            default:
                return execution;
        }
    }

    private RequestExecution withBackup(final RequestExecution execution) {
        return () -> {
            final CompletableFuture<ClientHttpResponse> original = execution.execute();
            final CompletableFuture<ClientHttpResponse> backup = new CompletableFuture<>();

            final Future<?> scheduledBackup = delay(backup(execution, backup));

            original.whenComplete(cancel(scheduledBackup));
            backup.whenComplete(cancel(original));

            return anyOf(original, backup);
        };
    }

    private ThrowingRunnable<IOException> backup(final RequestExecution execution,
            final CompletableFuture<ClientHttpResponse> target) {
        return () -> execution.execute().whenComplete(forwardTo(target));
    }

    private ScheduledFuture<?> delay(final Runnable task) {
        return scheduler.schedule(task, delay, unit);
    }

    private <T> BiConsumer<T, Throwable> cancel(final Future<?> future) {
        return (result, throwable) -> future.cancel(true);
    }

    @SafeVarargs
    private static <T> CompletableFuture<T> anyOf(final CompletableFuture<? extends T>... futures) {
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
            future.whenComplete(forwardTo(any));
        }

        return any;
    }

}
