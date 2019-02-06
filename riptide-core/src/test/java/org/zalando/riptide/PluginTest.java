package org.zalando.riptide;

import org.junit.Test;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;
import java.util.NoSuchElementException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.UnaryOperator;

import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.zalando.fauxpas.FauxPas.partially;
import static org.zalando.riptide.Plugin.compound;

public final class PluginTest {

    private final Plugin state = new Plugin() {
        @Override
        public RequestExecution beforeSend(final RequestExecution execution) {
            return applyTo(execution);
        }

        @Override
        public RequestExecution beforeDispatch(final RequestExecution execution) {
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
        public RequestExecution beforeSend(final RequestExecution execution) {
            return applyTo(execution);
        }

        @Override
        public RequestExecution beforeDispatch(final RequestExecution execution) {
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
    public void shouldApplyInCorrectOrder() throws IOException {
        shouldRunInCorrectOrder(arguments -> compound(state, argument).beforeSend(arguments));
    }

    @Test
    public void shouldPrepareInCorrectOrder() throws IOException {
        shouldRunInCorrectOrder(compound(state, argument)::beforeDispatch);
    }

    private void shouldRunInCorrectOrder(
            final UnaryOperator<RequestExecution> function) throws IOException {

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

}
