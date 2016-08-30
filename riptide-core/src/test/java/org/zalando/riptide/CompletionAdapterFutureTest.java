package org.zalando.riptide;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.zalando.fauxpas.ThrowingConsumer;

import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@RunWith(Parameterized.class)
public final class CompletionAdapterFutureTest {

    @SuppressWarnings("unchecked")
    private final CompletableFuture<Void> delegate = mock(CompletableFuture.class);
    private final Completion<Void> unit = Completion.valueOf(delegate);

    private final Tester tester;

    public CompletionAdapterFutureTest(final Tester tester) {
        this.tester = tester;
    }

    @FunctionalInterface
    private interface Tester extends ThrowingConsumer<Future<Void>, Exception> {

    }

    @Parameters(name = "{index}")
    public static Iterable<Object[]> data() {
        return Arrays.asList(new Tester[][]{
                {future -> future.cancel(true)},
                {Future::isCancelled},
                {Future::isDone},
                {Future::get},
                {future -> future.get(10, TimeUnit.SECONDS)}
        });
    }

    @Test
    public void shouldDelegate() throws Exception {
        tester.accept(unit);
        tester.accept(verify(delegate));
    }

}