package org.zalando.riptide;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@RunWith(Parameterized.class)
public final class CompletionAdapterCompletionStageTest {

    @SuppressWarnings("unchecked")
    private final CompletableFuture<Void> delegate = mock(CompletableFuture.class);
    private final Completion<Void> unit = Completion.valueOf(delegate);

    private final Tester tester;

    public CompletionAdapterCompletionStageTest(final Tester tester) {
        this.tester = tester;
    }

    @FunctionalInterface
    private interface Tester extends Consumer<CompletionStage<Void>> {

    }

    @Parameters(name = "{index}")
    public static Iterable<Object[]> data() {
        final CompletableFuture<Void> dependency = completedFuture(null);
        final Executor executor = mock(Executor.class);

        return Arrays.asList(new Tester[][]{
                {stage -> stage.thenApply(v -> v)},
                {stage -> stage.thenApplyAsync(v -> v)},
                {stage -> stage.thenApplyAsync(v -> v, executor)},
                {stage -> stage.thenAccept(v -> {})},
                {stage -> stage.thenAcceptAsync(v -> {})},
                {stage -> stage.thenAcceptAsync(v -> {}, executor)},
                {stage -> stage.thenRun(() -> {})},
                {stage -> stage.thenRunAsync(() -> {})},
                {stage -> stage.thenRunAsync(() -> {}, executor)},
                {stage -> stage.thenCombine(dependency, (v, w) -> v)},
                {stage -> stage.thenCombineAsync(dependency, (v, w) -> v)},
                {stage -> stage.thenCombineAsync(dependency, (v, w) -> v, executor)},
                {stage -> stage.thenAcceptBoth(dependency, (v, w) -> {})},
                {stage -> stage.thenAcceptBothAsync(dependency, (v, w) -> {})},
                {stage -> stage.thenAcceptBothAsync(dependency, (v, w) -> {}, executor)},
                {stage -> stage.runAfterBoth(dependency, () -> {})},
                {stage -> stage.runAfterBothAsync(dependency, () -> {})},
                {stage -> stage.runAfterBothAsync(dependency, () -> {}, executor)},
                {stage -> stage.applyToEither(dependency, v -> v)},
                {stage -> stage.applyToEitherAsync(dependency, v -> v)},
                {stage -> stage.applyToEitherAsync(dependency, v -> v, executor)},
                {stage -> stage.acceptEither(dependency, v -> {})},
                {stage -> stage.acceptEitherAsync(dependency, v -> {})},
                {stage -> stage.acceptEitherAsync(dependency, v -> {}, executor)},
                {stage -> stage.runAfterEither(dependency, () -> {})},
                {stage -> stage.runAfterEitherAsync(dependency, () -> {})},
                {stage -> stage.runAfterEitherAsync(dependency, () -> {}, executor)},
                {stage -> stage.thenCompose(CompletableFuture::completedFuture)},
                {stage -> stage.thenComposeAsync(CompletableFuture::completedFuture)},
                {stage -> stage.thenComposeAsync(CompletableFuture::completedFuture, executor)},
                {stage -> stage.exceptionally(e -> null)},
                {stage -> stage.whenComplete((v, e) -> {})},
                {stage -> stage.whenCompleteAsync((v, e) -> {})},
                {stage -> stage.whenCompleteAsync((v, e) -> {}, executor)},
                {stage -> stage.handle((v, e) -> v)},
                {stage -> stage.handleAsync((v, e) -> v)},
                {stage -> stage.handleAsync((v, e) -> v, executor)},
                // TODO toCompletionFuture
                // TODO Future methods
        });
    }

    @Test
    public void shouldDelegate() {
        tester.accept(unit);
        tester.accept(verify(delegate));
    }

}