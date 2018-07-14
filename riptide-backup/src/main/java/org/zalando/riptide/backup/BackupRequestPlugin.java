package org.zalando.riptide.backup;

import lombok.AllArgsConstructor;
import org.apiguardian.api.API;
import org.springframework.http.client.ClientHttpResponse;
import org.zalando.fauxpas.ThrowingRunnable;
import org.zalando.riptide.AbstractCancelableCompletableFuture;
import org.zalando.riptide.DefaultSafeMethodDetector;
import org.zalando.riptide.MethodDetector;
import org.zalando.riptide.OverrideSafeMethodDetector;
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

import static lombok.AccessLevel.PRIVATE;
import static org.apiguardian.api.API.Status.EXPERIMENTAL;
import static org.zalando.riptide.CancelableCompletableFuture.forwardTo;

@API(status = EXPERIMENTAL)
@AllArgsConstructor(access = PRIVATE)
public final class BackupRequestPlugin implements Plugin {

    private final MethodDetector safe;
    private final ScheduledExecutorService scheduler;
    private final long delay;
    private final TimeUnit unit;
    private final Executor executor;

    public BackupRequestPlugin(final ScheduledExecutorService scheduler, final long delay, final TimeUnit unit) {
        this(scheduler, delay, unit, Runnable::run);
    }

    // TODO replace with withExecutor(..) method
    public BackupRequestPlugin(final ScheduledExecutorService scheduler, final long delay, final TimeUnit unit,
            final Executor executor) {
        this(MethodDetector.compound(new DefaultSafeMethodDetector(), new OverrideSafeMethodDetector()),
                scheduler, delay, unit, executor);
    }

    public BackupRequestPlugin withSafeMethodDetector(final MethodDetector detector) {
        return new BackupRequestPlugin(detector, scheduler, delay, unit, executor);
    }

    @Override
    public RequestExecution prepare(final RequestArguments arguments, final RequestExecution execution) {
        if (safe.test(arguments)) {
            return withBackup(execution);
        }

        return execution;
    }

    private RequestExecution withBackup(final RequestExecution execution) {
        return () -> {
            final CompletableFuture<ClientHttpResponse> original = execution.execute();
            final CompletableFuture<ClientHttpResponse> backup = new CompletableFuture<>();

            final Future<?> scheduledBackup = delay(backup(execution, backup));

            original.whenCompleteAsync(cancel(scheduledBackup), executor);
            backup.whenCompleteAsync(cancel(original), executor);

            return anyOf(original, backup);
        };
    }

    private ThrowingRunnable<IOException> backup(final RequestExecution execution,
            final CompletableFuture<ClientHttpResponse> target) {
        return () -> execution.execute().whenCompleteAsync(forwardTo(target), executor);
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
