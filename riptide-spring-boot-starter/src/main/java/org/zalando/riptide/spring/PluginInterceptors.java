package org.zalando.riptide.spring;

import com.google.common.collect.ImmutableMultimap;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.AsyncClientHttpRequestExecution;
import org.springframework.http.client.AsyncClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.concurrent.CompletableToListenableFutureAdapter;
import org.springframework.util.concurrent.ListenableFuture;
import org.zalando.riptide.Plugin;
import org.zalando.riptide.RequestArguments;
import org.zalando.riptide.RequestExecution;
import org.zalando.riptide.capture.Completion;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.CompletableFuture;

import static org.zalando.riptide.CancelableCompletableFuture.preserveCancelability;

final class PluginInterceptors {

    @SuppressWarnings("unused") // by Spring
    static Adapter adapt(final Plugin plugin) {
        return new Adapter(plugin);
    }

    @AllArgsConstructor
    static final class Adapter implements ClientHttpRequestInterceptor, AsyncClientHttpRequestInterceptor {

        private final Plugin plugin;

        @Override
        public ClientHttpResponse intercept(final HttpRequest request, final byte[] body,
                final ClientHttpRequestExecution execution) throws IOException {

            final RequestArguments arguments = toArguments(request, body);
            final RequestExecution requestExecution = () -> {
                final CompletableFuture<ClientHttpResponse> future = new CompletableFuture<>();

                try {
                    future.complete(execution.execute(request, body));
                } catch (final Exception e) {
                    future.completeExceptionally(e);
                }

                return future;
            };

            // since there is no routing to be done, we just call the plugin twice in succession
            final RequestExecution before = plugin.interceptBeforeRouting(arguments, requestExecution);
            final RequestExecution after = plugin.interceptAfterRouting(arguments, before);

            return Completion.join(after.execute());
        }

        @Override
        public ListenableFuture<ClientHttpResponse> intercept(final HttpRequest request, final byte[] body,
                final AsyncClientHttpRequestExecution execution) throws IOException {

            final RequestArguments arguments = toArguments(request, body);

            final RequestExecution requestExecution = () -> {
                try {

                    final ListenableFuture<ClientHttpResponse> original = execution.executeAsync(request, body);
                    final CompletableFuture<ClientHttpResponse> future = preserveCancelability(original);
                    original.addCallback(future::complete, future::completeExceptionally);
                    return future;
                } catch (final Exception e) {
                    // this branch is just for issues inside of executeAsync, not during the actual execution
                    final CompletableFuture<ClientHttpResponse> future = new CompletableFuture<>();
                    future.completeExceptionally(e);
                    return future;
                }
            };

            // since there is no routing to be done, we just call the plugin twice in succession
            final RequestExecution before = plugin.interceptBeforeRouting(arguments, requestExecution);
            final RequestExecution after = plugin.interceptAfterRouting(arguments, before);

            return new CompletableToListenableFutureAdapter<>(after.execute());
        }

        /**
         * Takes an existing {@link HttpRequest request} and a body and tries to adapts them to
         * {@link RequestArguments arguments}.
         *
         * @see org.springframework.web.util.AbstractUriTemplateHandler#insertBaseUrl(URI)
         * @param request original request
         * @param body serialized request body
         * @return derived request arguments
         */
        static RequestArguments toArguments(final HttpRequest request, final byte[] body) {
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

        private static ImmutableMultimap<String, String> copy(final HttpHeaders headers) {
            final ImmutableMultimap.Builder<String, String> copy = ImmutableMultimap.builder();
            headers.forEach(copy::putAll);
            return copy.build();
        }

    }

}
