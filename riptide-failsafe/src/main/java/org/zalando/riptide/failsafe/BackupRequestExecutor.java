package org.zalando.riptide.failsafe;

import dev.failsafe.spi.AsyncExecutionInternal;
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
import java.util.function.Function;

import static org.zalando.riptide.CompletableFutures.forwardTo;

final class BackupRequestExecutor<R> extends PolicyExecutor<R> {

    //TODO: correct usage?
    private final BackupRequest<R> policy;

    BackupRequestExecutor(final BackupRequest<R> policy, int policyIndex) {
        super(policy, policyIndex);
        this.policy = policy;
    }

    @Override
    public Function<AsyncExecutionInternal<R>, CompletableFuture<ExecutionResult<R>>> applyAsync(
            Function<AsyncExecutionInternal<R>, CompletableFuture<ExecutionResult<R>>> innerFn,
            final Scheduler scheduler,
            final FailsafeFuture<R> future) {

        return (asyncExecutionInternal) -> {
            final CompletableFuture<ExecutionResult<R>> original = innerFn.apply(asyncExecutionInternal);
            final CompletableFuture<ExecutionResult<R>> backup = new CompletableFuture<>();

            final Future<?> scheduledBackup = delay(scheduler, backup(innerFn, asyncExecutionInternal, backup));

            original.whenComplete(cancel(scheduledBackup));
            backup.whenComplete(cancel(original));

            return anyOf(original, backup);
        };
    }

    private Callable<CompletableFuture<ExecutionResult<R>>> backup(
            final Function<AsyncExecutionInternal<R>, CompletableFuture<ExecutionResult<R>>> innerFn,
            final AsyncExecutionInternal<R> asyncExecutionInternal,
            final CompletableFuture<ExecutionResult<R>> target) {

        return () -> innerFn.apply(asyncExecutionInternal).whenComplete(forwardTo(target));
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
