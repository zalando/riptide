package org.zalando.riptide;

import org.junit.jupiter.api.Test;
import org.springframework.http.client.ClientHttpResponse;

import java.util.NoSuchElementException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.UnaryOperator;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.mock;
import static org.zalando.fauxpas.FauxPas.partially;
import static org.zalando.riptide.Plugin.composite;

final class PluginTest {

    private final Plugin state = new Plugin() {
        @Override
        public RequestExecution aroundNetwork(final RequestExecution execution) {
            return applyTo(execution);
        }

        @Override
        public RequestExecution aroundDispatch(final RequestExecution execution) {
            return applyTo(execution);
        }

        private RequestExecution applyTo(final RequestExecution execution) {
            return arguments -> execution.execute(arguments)
                    .exceptionally(partially(e -> {
                        throw new IllegalStateException(e);
                    }));
        }
    };

    private final Plugin argument = new Plugin() {
        @Override
        public RequestExecution aroundNetwork(final RequestExecution execution) {
            return applyTo(execution);
        }

        @Override
        public RequestExecution aroundDispatch(final RequestExecution execution) {
            return applyTo(execution);
        }

        private RequestExecution applyTo(final RequestExecution execution) {
            return arguments -> execution.execute(arguments)
                    .exceptionally(partially(e -> {
                        throw new IllegalArgumentException(e);
                    }));
        }
    };

    @Test
    void shouldApplyInCorrectOrder() {
        shouldRunInCorrectOrder(arguments -> composite(state, argument).aroundNetwork(arguments));
    }

    @Test
    void shouldPrepareInCorrectOrder() {
        shouldRunInCorrectOrder(composite(state, argument)::aroundDispatch);
    }

    private void shouldRunInCorrectOrder(
            final UnaryOperator<RequestExecution> function) {

        try {
            final RequestExecution execution = arguments -> {
                final CompletableFuture<ClientHttpResponse> future = new CompletableFuture<>();
                future.completeExceptionally(new NoSuchElementException());
                return future;
            };

            final RequestArguments arguments = mock(RequestArguments.class);
            function.apply(execution).execute(arguments).join();

            fail("Expected exception");
        } catch (final CompletionException e) {
            final Throwable throwable = e.getCause();

            assertThat(throwable, is(instanceOf(IllegalArgumentException.class)));
            assertThat(throwable.getCause(), is(instanceOf(IllegalStateException.class)));
            assertThat(throwable.getCause().getCause(), is(instanceOf(NoSuchElementException.class)));
        }
    }

    private final Plugin malicious = new Plugin() {
        @Override
        public RequestExecution aroundAsync(final RequestExecution execution) {
            return arguments -> {
                throw new UnsupportedOperationException();
            };
        }
    };

    @Test
    void shouldWrapExceptionInExceptionallyCompletedCompletableFuture() {
        final Plugin plugin = composite(malicious, new AsyncPlugin(Runnable::run));
        final CompletableFuture<ClientHttpResponse> future = plugin.aroundAsync(arguments -> completedFuture(null))
                .execute(mock(RequestArguments.class));

        final CompletionException exception = assertThrows(CompletionException.class, future::join);
        assertThat(exception.getCause(), is(instanceOf(UnsupportedOperationException.class)));
    }

}
