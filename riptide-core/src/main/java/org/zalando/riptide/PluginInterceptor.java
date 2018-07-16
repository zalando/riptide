package org.zalando.riptide;

import com.google.common.collect.ImmutableMultimap;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import org.apiguardian.api.API;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.AsyncClientHttpRequestExecution;
import org.springframework.http.client.AsyncClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.MultiValueMap;
import org.springframework.util.concurrent.CompletableToListenableFutureAdapter;
import org.springframework.util.concurrent.ListenableFuture;
import org.zalando.fauxpas.ThrowingSupplier;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import static org.apiguardian.api.API.Status.EXPERIMENTAL;
import static org.zalando.riptide.CancelableCompletableFuture.preserveCancelability;

/**
 * Adapts a {@link Plugin plugin} to the {@link ClientHttpRequestInterceptor} and
 * {@link AsyncClientHttpRequestInterceptor} interfaces in order to allow plugins to be used with ordinary
 * {@link org.springframework.web.client.RestTemplate}s and {@link org.springframework.web.client.AsyncRestTemplate}s
 * without any modifications.
 *
 * This allows users which prefer templates over {@link Http} can still utilize all plugins provided by Riptide.
 *
 * @see Plugin
 * @see ClientHttpRequestInterceptor
 * @see AsyncClientHttpRequestInterceptor
 * @see OriginalStackTracePlugin
 */
@API(status = EXPERIMENTAL)
@AllArgsConstructor
public final class PluginInterceptor implements ClientHttpRequestInterceptor, AsyncClientHttpRequestInterceptor {

    private final Plugin plugin;

    @Nonnull
    @SneakyThrows
    @Override
    public ClientHttpResponse intercept(final HttpRequest request, final byte[] body,
            final ClientHttpRequestExecution execution) {

        final RequestExecution requestExecution = arguments ->
                pack(() -> execution.execute(request, body));


        // since there is no routing to be done, we just call the plugin twice in succession
        final RequestExecution before = plugin.beforeSend(requestExecution);
        final RequestExecution after = plugin.beforeDispatch(before);

        final RequestArguments arguments = toArguments(request, body);

        try {
            return after.execute(arguments).join();
        } catch (final CompletionException e) {
            throw e.getCause();
        }
    }

    @Override
    public ListenableFuture<ClientHttpResponse> intercept(final HttpRequest request, final byte[] body,
            final AsyncClientHttpRequestExecution execution) throws IOException {

        final RequestExecution requestExecution = arguments ->
                packAsync(() -> execution.executeAsync(request, body));

        // since there is no routing to be done, we just call the plugin twice in succession
        final RequestExecution before = plugin.beforeSend(requestExecution);
        final RequestExecution after = plugin.beforeDispatch(before);

        final RequestArguments arguments = toArguments(request, body);
        return new CompletableToListenableFutureAdapter<>(after.execute(arguments));
    }

    private static <T> CompletableFuture<T> pack(final ThrowingSupplier<T, Exception> supplier) {
        final CompletableFuture<T> future = new CompletableFuture<>();

        try {
            future.complete(supplier.tryGet());
        } catch (final Exception e) {
            // TODO just throw?
            future.completeExceptionally(e);
        }

        return future;
    }

    private static <T> CompletableFuture<T> packAsync(final ThrowingSupplier<ListenableFuture<T>, Exception> supplier) {
        try {
            final ListenableFuture<T> original = supplier.tryGet();
            final CompletableFuture<T> future = preserveCancelability(original);
            original.addCallback(future::complete, future::completeExceptionally);
            return future;
        } catch (final Exception e) {
            // TODO can't we just throw here?
            // for issues that occur before the future was successfully created, i.e. request being sent
            final CompletableFuture<T> future = new CompletableFuture<>();
            future.completeExceptionally(e);
            return future;
        }
    }

    /**
     * Takes an existing {@link HttpRequest request} and a body and tries to adapts them to
     * {@link RequestArguments arguments}.
     *
     * @param request original request
     * @param body    serialized request body
     * @return derived request arguments
     * @see org.springframework.web.util.AbstractUriTemplateHandler#insertBaseUrl(java.net.URI)
     */
    private RequestArguments toArguments(final HttpRequest request, final byte[] body) {
        return RequestArguments.create()
                .withMethod(request.getMethod())
                .withRequestUri(request.getURI())
                .withHeaders(copy(request.getHeaders()))
                /*
                 * Plugins and AsyncClientHttpRequestInterceptors are conceptually working on different logical
                 * levels. Plugins are pre-serialization and interceptors are post-serialization. Passing the
                 * serialized body to plugin implementations is therefore totally wrong, but in a best-effort
                 * attempt, we decided to do it anyway.
                 */
                .withBody(body);
    }

    private ImmutableMultimap<String, String> copy(final MultiValueMap<String, String> values) {
        final ImmutableMultimap.Builder<String, String> copy = ImmutableMultimap.builder();
        values.forEach(copy::putAll);
        return copy.build();
    }

}
