package org.zalando.riptide.hystrix;

import com.google.common.base.Throwables;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpResponse;
import org.zalando.riptide.DefaultRequestArguments;
import org.zalando.riptide.RequestExecution;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hobsoft.hamcrest.compose.ComposeMatchers.hasFeature;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public final class HystrixPluginTest {

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    private final DefaultRequestArguments arguments = new DefaultRequestArguments()
            .withMethod(HttpMethod.GET)
            .withRequestUri(URI.create("https://api.example.com/"));

    private final RequestExecution execution = mock(RequestExecution.class);

    @Test
    public void shouldWrapIOException() throws IOException {
        when(execution.execute()).thenThrow(IOException.class);

        final RequestExecution unit = new HystrixPlugin().prepare(arguments, execution);

        exception.expect(CompletionException.class);
        exception.expect(hasFeature(Throwables::getCausalChain, hasItem(instanceOf(UncheckedIOException.class))));

        unit.execute().join();
    }

    @Test
    public void shouldCancel() throws IOException {
        when(execution.execute()).thenReturn(new CompletableFuture<>());

        final RequestExecution unit = new HystrixPlugin().prepare(arguments, execution);

        final CompletableFuture<ClientHttpResponse> future = unit.execute();
        future.cancel(true);

        assertThat(future.isCancelled(), is(true));
    }

}