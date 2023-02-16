package org.zalando.riptide.failsafe;

import dev.failsafe.AbstractExecution;
import dev.failsafe.spi.ExecutionResult;
import dev.failsafe.spi.FailsafeFuture;
import dev.failsafe.spi.PolicyExecutor;
import dev.failsafe.spi.Scheduler;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import static org.zalando.riptide.CompletableFutures.forwardTo;

final class BackupRequestExecutor<R> extends PolicyExecutor<BackupRequest<R>> {

    BackupRequestExecutor(final BackupRequest<R> policy, final AbstractExecution execution) {
        super(policy, execution);
    }

    @Override
    protected Supplier<CompletableFuture<ExecutionResult>> supplyAsync(
            final Supplier<CompletableFuture<ExecutionResult>> supplier,
            final Scheduler scheduler,
            final FailsafeFuture<Object> future) {

        return () -> {
            final CompletableFuture<ExecutionResult> original = supplier.get();
            final CompletableFuture<ExecutionResult> backup = new CompletableFuture<>();

            final Future<?> scheduledBackup = delay(scheduler, backup(supplier, backup));

            original.whenComplete(cancel(scheduledBackup));
            backup.whenComplete(cancel(original));

            return anyOf(original, backup);
        };
    }

    private Callable<CompletableFuture<ExecutionResult>> backup(
            final Supplier<CompletableFuture<ExecutionResult>> supplier,
            final CompletableFuture<ExecutionResult> target) {

        return () -> supplier.get().whenComplete(forwardTo(target));
    }

    @SuppressWarnings("unchecked")
    private <T> ScheduledFuture<T> delay(
            final Scheduler scheduler,
            final Callable<T> callable) {

        final long delay = policy.getDelay();
        final TimeUnit unit = policy.getUnit();
        return (ScheduledFuture<T>) scheduler.schedule(callable, delay, unit);
    }

    private <T> BiConsumer<T, Throwable> cancel(final Future<?> future) {
        return (result, throwable) -> future.cancel(true);
    }

    @SafeVarargs
    private final <T> CompletableFuture<T> anyOf(final CompletableFuture<? extends T>... futures) {
        final CompletableFuture<T> any = new CompletableFuture<>();

        for (final CompletableFuture<? extends T> future : futures) {
            future.whenComplete(forwardTo(any));
        }

        return any;
    }

}
