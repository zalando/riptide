package org.zalando.riptide.backup;

import lombok.*;
import org.apiguardian.api.*;
import org.springframework.http.client.*;
import org.zalando.fauxpas.*;
import org.zalando.riptide.*;
import org.zalando.riptide.idempotency.*;

import java.io.*;
import java.util.concurrent.*;
import java.util.function.*;

import static lombok.AccessLevel.*;
import static org.apiguardian.api.API.Status.*;
import static org.zalando.riptide.CompletableFutures.*;

@API(status = EXPERIMENTAL)
@AllArgsConstructor(access = PRIVATE)
public final class BackupRequestPlugin implements Plugin {

    private final ScheduledExecutorService scheduler;
    private final long delay;
    private final TimeUnit unit;
    private final Predicate<RequestArguments> predicate;
    private final Executor executor;

    public BackupRequestPlugin(final ScheduledExecutorService scheduler, final long delay, final TimeUnit unit) {
        this(scheduler, delay, unit, new IdempotencyPredicate(), Runnable::run);
    }

    public BackupRequestPlugin withPredicate(final Predicate<RequestArguments> predicate) {
        return new BackupRequestPlugin(scheduler, delay, unit, predicate, executor);
    }

    public BackupRequestPlugin withExecutor(final Executor executor) {
        return new BackupRequestPlugin(scheduler, delay, unit, predicate, executor);
    }

    @Override
    public RequestExecution aroundAsync(final RequestExecution execution) {
        return arguments -> {
            if (predicate.test(arguments)) {
                return withBackup(execution, arguments);
            }

            return execution.execute(arguments);
        };
    }

    private CompletableFuture<ClientHttpResponse> withBackup(final RequestExecution execution,
            final RequestArguments arguments) throws IOException {
        final CompletableFuture<ClientHttpResponse> original = execution.execute(arguments);
        final CompletableFuture<ClientHttpResponse> backup = new CompletableFuture<>();

        final Future<?> scheduledBackup = delay(backup(execution, arguments, backup));

        original.whenCompleteAsync(cancel(scheduledBackup), executor);
        backup.whenCompleteAsync(cancel(original), executor);

        return anyOf(original, backup);
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
        final CompletableFuture<T> any = new CompletableFuture<>();

        for (final CompletableFuture<? extends T> future : futures) {
            future.whenCompleteAsync(forwardTo(any), executor);
        }

        return any;
    }

}
