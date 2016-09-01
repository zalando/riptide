package org.zalando.riptide;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

final class CompletionAdapter<T> implements Completion<T> {

    private final CompletableFuture<T> future;

    CompletionAdapter(final CompletableFuture<T> future) {
        this.future = future;
    }

    @Override
    public <U> Completion<U> thenApply(final Function<? super T, ? extends U> fn) {
        return Completion.valueOf(future.thenApply(fn));
    }

    @Override
    public <U> Completion<U> thenApplyAsync(final Function<? super T, ? extends U> fn) {
        return Completion.valueOf(future.thenApplyAsync(fn));
    }

    @Override
    public <U> Completion<U> thenApplyAsync(final Function<? super T, ? extends U> fn, final Executor executor) {
        return Completion.valueOf(future.thenApplyAsync(fn, executor));
    }

    @Override
    public Completion<Void> thenAccept(final Consumer<? super T> action) {
        return Completion.valueOf(future.thenAccept(action));
    }

    @Override
    public Completion<Void> thenAcceptAsync(final Consumer<? super T> action) {
        return Completion.valueOf(future.thenAcceptAsync(action));
    }

    @Override
    public Completion<Void> thenAcceptAsync(final Consumer<? super T> action, final Executor executor) {
        return Completion.valueOf(future.thenAcceptAsync(action, executor));
    }

    @Override
    public Completion<Void> thenRun(final Runnable action) {
        return Completion.valueOf(future.thenRun(action));
    }

    @Override
    public Completion<Void> thenRunAsync(final Runnable action) {
        return Completion.valueOf(future.thenRunAsync(action));
    }

    @Override
    public Completion<Void> thenRunAsync(final Runnable action, final Executor executor) {
        return Completion.valueOf(future.thenRunAsync(action, executor));
    }

    @Override
    public <U, V> Completion<V> thenCombine(final CompletionStage<? extends U> other, final BiFunction<? super T, ? super U, ? extends V> fn) {
        return Completion.valueOf(future.thenCombine(other, fn));
    }

    @Override
    public <U, V> Completion<V> thenCombineAsync(final CompletionStage<? extends U> other, final BiFunction<? super T, ? super U, ? extends V> fn) {
        return Completion.valueOf(future.thenCombineAsync(other, fn));
    }

    @Override
    public <U, V> Completion<V> thenCombineAsync(final CompletionStage<? extends U> other, final BiFunction<? super T, ? super U, ? extends V> fn, final Executor executor) {
        return Completion.valueOf(future.thenCombineAsync(other, fn, executor));
    }

    @Override
    public <U> Completion<Void> thenAcceptBoth(final CompletionStage<? extends U> other, final BiConsumer<? super T, ? super U> action) {
        return Completion.valueOf(future.thenAcceptBoth(other, action));
    }

    @Override
    public <U> Completion<Void> thenAcceptBothAsync(final CompletionStage<? extends U> other, final BiConsumer<? super T, ? super U> action) {
        return Completion.valueOf(future.thenAcceptBothAsync(other, action));
    }

    @Override
    public <U> Completion<Void> thenAcceptBothAsync(final CompletionStage<? extends U> other, final BiConsumer<? super T, ? super U> action, final Executor executor) {
        return Completion.valueOf(future.thenAcceptBothAsync(other, action, executor));
    }

    @Override
    public Completion<Void> runAfterBoth(final CompletionStage<?> other, final Runnable action) {
        return Completion.valueOf(future.runAfterBoth(other, action));
    }

    @Override
    public Completion<Void> runAfterBothAsync(final CompletionStage<?> other, final Runnable action) {
        return Completion.valueOf(future.runAfterBothAsync(other, action));
    }

    @Override
    public Completion<Void> runAfterBothAsync(final CompletionStage<?> other, final Runnable action, final Executor executor) {
        return Completion.valueOf(future.runAfterBothAsync(other, action, executor));
    }

    @Override
    public <U> Completion<U> applyToEither(final CompletionStage<? extends T> other, final Function<? super T, U> fn) {
        return Completion.valueOf(future.applyToEither(other, fn));
    }

    @Override
    public <U> Completion<U> applyToEitherAsync(final CompletionStage<? extends T> other, final Function<? super T, U> fn) {
        return Completion.valueOf(future.applyToEitherAsync(other, fn));
    }

    @Override
    public <U> Completion<U> applyToEitherAsync(final CompletionStage<? extends T> other, final Function<? super T, U> fn, final Executor executor) {
        return Completion.valueOf(future.applyToEitherAsync(other, fn, executor));
    }

    @Override
    public Completion<Void> acceptEither(final CompletionStage<? extends T> other, final Consumer<? super T> action) {
        return Completion.valueOf(future.acceptEither(other, action));
    }

    @Override
    public Completion<Void> acceptEitherAsync(final CompletionStage<? extends T> other, final Consumer<? super T> action) {
        return Completion.valueOf(future.acceptEitherAsync(other, action));
    }

    @Override
    public Completion<Void> acceptEitherAsync(final CompletionStage<? extends T> other, final Consumer<? super T> action, final Executor executor) {
        return Completion.valueOf(future.acceptEitherAsync(other, action, executor));
    }

    @Override
    public Completion<Void> runAfterEither(final CompletionStage<?> other, final Runnable action) {
        return Completion.valueOf(future.runAfterEither(other, action));
    }

    @Override
    public Completion<Void> runAfterEitherAsync(final CompletionStage<?> other, final Runnable action) {
        return Completion.valueOf(future.runAfterEitherAsync(other, action));
    }

    @Override
    public Completion<Void> runAfterEitherAsync(final CompletionStage<?> other, final Runnable action, final Executor executor) {
        return Completion.valueOf(future.runAfterEitherAsync(other, action, executor));
    }

    @Override
    public <U> Completion<U> thenCompose(final Function<? super T, ? extends CompletionStage<U>> fn) {
        return Completion.valueOf(future.thenCompose(fn));
    }

    @Override
    public <U> Completion<U> thenComposeAsync(final Function<? super T, ? extends CompletionStage<U>> fn) {
        return Completion.valueOf(future.thenComposeAsync(fn));
    }

    @Override
    public <U> Completion<U> thenComposeAsync(final Function<? super T, ? extends CompletionStage<U>> fn, final Executor executor) {
        return Completion.valueOf(future.thenComposeAsync(fn, executor));
    }

    @Override
    public Completion<T> exceptionally(final Function<Throwable, ? extends T> fn) {
        return Completion.valueOf(future.exceptionally(fn));
    }

    @Override
    public Completion<T> whenComplete(final BiConsumer<? super T, ? super Throwable> action) {
        return Completion.valueOf(future.whenComplete(action));
    }

    @Override
    public Completion<T> whenCompleteAsync(final BiConsumer<? super T, ? super Throwable> action) {
        return Completion.valueOf(future.whenCompleteAsync(action));
    }

    @Override
    public Completion<T> whenCompleteAsync(final BiConsumer<? super T, ? super Throwable> action, final Executor executor) {
        return Completion.valueOf(future.whenCompleteAsync(action, executor));
    }

    @Override
    public <U> Completion<U> handle(final BiFunction<? super T, Throwable, ? extends U> fn) {
        return Completion.valueOf(future.handle(fn));
    }

    @Override
    public <U> Completion<U> handleAsync(final BiFunction<? super T, Throwable, ? extends U> fn) {
        return Completion.valueOf(future.handleAsync(fn));
    }

    @Override
    public <U> Completion<U> handleAsync(final BiFunction<? super T, Throwable, ? extends U> fn, final Executor executor) {
        return Completion.valueOf(future.handleAsync(fn, executor));
    }

    @Override
    public CompletableFuture<T> toCompletableFuture() {
        return future;
    }

    @Override
    public boolean cancel(final boolean mayInterruptIfRunning) {
        return future.cancel(mayInterruptIfRunning);
    }

    @Override
    public boolean isCancelled() {
        return future.isCancelled();
    }

    @Override
    public boolean isDone() {
        return future.isDone();
    }

    @Override
    public T get() throws InterruptedException, ExecutionException {
        return future.get();
    }

    @Override
    public T get(final long timeout, final TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return future.get(timeout, unit);
    }

    @Override
    public T join() {
        return future.join();
    }

    @Override
    public T getNow(final T valueIfAbsent) {
        return future.getNow(valueIfAbsent);
    }

}
