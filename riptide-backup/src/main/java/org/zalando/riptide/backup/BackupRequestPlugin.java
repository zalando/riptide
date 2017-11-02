package org.zalando.riptide.backup;

import com.google.gag.annotation.remark.Facepalm;
import org.springframework.http.client.ClientHttpResponse;
import org.zalando.fauxpas.ThrowingRunnable;
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

import static java.util.Objects.nonNull;

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
            final ScheduledFuture<?> scheduledBackup = delay(backup(execution, backup));
            original.whenComplete(cancel(scheduledBackup));
            return anyOf(original, backup);
        };
    }

    private ThrowingRunnable<IOException> backup(final RequestExecution execution,
            final CompletableFuture<ClientHttpResponse> future) {
        return () -> execution.execute().whenComplete(forwardTo(future));
    }

    private static <T> BiConsumer<T, Throwable> forwardTo(final CompletableFuture<T> future) {
        return (response, throwable) -> {
            if (nonNull(response)) {
                future.complete(response);
            }
            if (nonNull(throwable)) {
                future.completeExceptionally(throwable);
            }
        };
    }

    private ScheduledFuture<?> delay(final Runnable runnable) {
        return scheduler.schedule(runnable, delay, unit);
    }

    private <T> BiConsumer<T, Throwable> cancel(final Future<?> future) {
        return (result, throwable) -> future.cancel(false);
    }

    @Facepalm("Not exactly sure who at Oracle thought that the signature of anyOf would be of any use...")
    @SuppressWarnings("unchecked")
    @SafeVarargs
    private static <T> CompletableFuture<T> anyOf(final CompletableFuture<? extends T>... futures) {
        return (CompletableFuture) CompletableFuture.anyOf(futures);
    }

}
