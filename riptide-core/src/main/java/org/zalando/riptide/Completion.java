package org.zalando.riptide;

import javax.annotation.Nullable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

public interface Completion<T> extends Future<T>, CompletionStage<T> {

    @Override
    <U> Completion<U> thenApply(Function<? super T, ? extends U> fn);

    @Override
    <U> Completion<U> thenApplyAsync(Function<? super T, ? extends U> fn);

    @Override
    <U> Completion<U> thenApplyAsync(Function<? super T, ? extends U> fn, Executor executor);

    @Override
    Completion<Void> thenAccept(Consumer<? super T> action);

    @Override
    Completion<Void> thenAcceptAsync(Consumer<? super T> action);

    @Override
    Completion<Void> thenAcceptAsync(Consumer<? super T> action, Executor executor);

    @Override
    Completion<Void> thenRun(Runnable action);

    @Override
    Completion<Void> thenRunAsync(Runnable action);

    @Override
    Completion<Void> thenRunAsync(Runnable action, Executor executor);

    @Override
    <U, V> Completion<V> thenCombine(CompletionStage<? extends U> other, BiFunction<? super T, ? super U, ? extends V> fn);

    @Override
    <U, V> Completion<V> thenCombineAsync(CompletionStage<? extends U> other, BiFunction<? super T, ? super U, ? extends V> fn);

    @Override
    <U, V> Completion<V> thenCombineAsync(CompletionStage<? extends U> other, BiFunction<? super T, ? super U, ? extends V> fn, Executor executor);

    @Override
    <U> Completion<Void> thenAcceptBoth(CompletionStage<? extends U> other, BiConsumer<? super T, ? super U> action);

    @Override
    <U> Completion<Void> thenAcceptBothAsync(CompletionStage<? extends U> other, BiConsumer<? super T, ? super U> action);

    @Override
    <U> Completion<Void> thenAcceptBothAsync(CompletionStage<? extends U> other, BiConsumer<? super T, ? super U> action, Executor executor);

    @Override
    Completion<Void> runAfterBoth(CompletionStage<?> other, Runnable action);

    @Override
    Completion<Void> runAfterBothAsync(CompletionStage<?> other, Runnable action);

    @Override
    Completion<Void> runAfterBothAsync(CompletionStage<?> other, Runnable action, Executor executor);

    @Override
    <U> Completion<U> applyToEither(CompletionStage<? extends T> other, Function<? super T, U> fn);

    @Override
    <U> Completion<U> applyToEitherAsync(CompletionStage<? extends T> other, Function<? super T, U> fn);

    @Override
    <U> Completion<U> applyToEitherAsync(CompletionStage<? extends T> other, Function<? super T, U> fn, Executor executor);

    @Override
    Completion<Void> acceptEither(CompletionStage<? extends T> other, Consumer<? super T> action);

    @Override
    Completion<Void> acceptEitherAsync(CompletionStage<? extends T> other, Consumer<? super T> action);

    @Override
    Completion<Void> acceptEitherAsync(CompletionStage<? extends T> other, Consumer<? super T> action, Executor executor);

    @Override
    Completion<Void> runAfterEither(CompletionStage<?> other, Runnable action);

    @Override
    Completion<Void> runAfterEitherAsync(CompletionStage<?> other, Runnable action);

    @Override
    Completion<Void> runAfterEitherAsync(CompletionStage<?> other, Runnable action, Executor executor);

    @Override
    <U> Completion<U> thenCompose(Function<? super T, ? extends CompletionStage<U>> fn);

    @Override
    <U> Completion<U> thenComposeAsync(Function<? super T, ? extends CompletionStage<U>> fn);

    @Override
    <U> Completion<U> thenComposeAsync(Function<? super T, ? extends CompletionStage<U>> fn, Executor executor);

    @Override
    Completion<T> exceptionally(Function<Throwable, ? extends T> fn);

    @Override
    Completion<T> whenComplete(BiConsumer<? super T, ? super Throwable> action);

    @Override
    Completion<T> whenCompleteAsync(BiConsumer<? super T, ? super Throwable> action);

    @Override
    Completion<T> whenCompleteAsync(BiConsumer<? super T, ? super Throwable> action, Executor executor);

    @Override
    <U> Completion<U> handle(BiFunction<? super T, Throwable, ? extends U> fn);

    @Override
    <U> Completion<U> handleAsync(BiFunction<? super T, Throwable, ? extends U> fn);

    @Override
    <U> Completion<U> handleAsync(BiFunction<? super T, Throwable, ? extends U> fn, Executor executor);

    T join() throws CancellationException, CompletionException;

    T getNow(@Nullable T valueIfAbsent) throws CancellationException, CompletionException;

    static <T> Completion<T> valueOf(final CompletableFuture<T> future) {
        return new CompletionAdapter<>(future);
    }

}
