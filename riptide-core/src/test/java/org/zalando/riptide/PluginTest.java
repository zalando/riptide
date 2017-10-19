package org.zalando.riptide;

import org.junit.Test;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.zalando.fauxpas.FauxPas.partially;

public final class PluginTest {

    private final Plugin state = (arguments, execution) ->
            () -> execution.execute()
                    .exceptionally(partially(e -> {
                        throw new IllegalStateException(e);
                    }));

    private final Plugin argument = (arguments, execution) ->
            () -> execution.execute()
                    .exceptionally(partially(e -> {
                        throw new IllegalArgumentException(e);
                    }));

    @Test
    public void shouldCombineInCorrectOrder() throws IOException {
        final Plugin unit = Plugin.compound(state, argument);

        final RequestArguments arguments = mock(RequestArguments.class);
        final RequestExecution execution = () -> {
            final CompletableFuture<ClientHttpResponse> future = new CompletableFuture<>();
            future.completeExceptionally(new NullPointerException());
            return future;
        };

        final CompletableFuture<ClientHttpResponse> future = unit.prepare(arguments, execution).execute();

        try {
            future.join();
            fail("Expected exception");
        } catch (final CompletionException e) {
            final Throwable throwable = e.getCause();

            assertThat(throwable, is(instanceOf(IllegalArgumentException.class)));
            assertThat(throwable.getCause(), is(instanceOf(IllegalStateException.class)));
            assertThat(throwable.getCause().getCause(), is(instanceOf(NullPointerException.class)));
        }
    }

    @Test
    public void identityShouldReturnAsIs() {
        final RequestArguments arguments = mock(RequestArguments.class);
        final RequestExecution expected = mock(RequestExecution.class);

        final RequestExecution actual = Plugin.IDENTITY.prepare(arguments, expected);

        assertThat(actual, is(sameInstance(expected)));
    }

}
