package org.zalando.riptide;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.client.AsyncClientHttpRequest;
import org.springframework.http.client.AsyncClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpResponse;
import org.zalando.fauxpas.ThrowingUnaryOperator;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.util.concurrent.CompletableFuture;

import static com.google.common.base.Preconditions.checkArgument;

public final class Requester extends Dispatcher {

    private final AsyncClientHttpRequestFactory requestFactory;
    private final MessageWorker worker;
    private final RequestArguments arguments;
    private final Plugin plugin;

    private final Multimap<String, String> query = LinkedHashMultimap.create();
    private final HttpHeaders headers = new HttpHeaders();

    public Requester(final AsyncClientHttpRequestFactory requestFactory, final MessageWorker worker,
            final RequestArguments arguments, final Plugin plugin) {
        this.requestFactory = requestFactory;
        this.worker = worker;
        this.arguments = arguments;
        this.plugin = plugin;
    }

    public final Requester queryParam(final String name, final String value) {
        query.put(name, value);
        return this;
    }

    public final Requester queryParams(final Multimap<String, String> params) {
        query.putAll(params);
        return this;
    }

    public final Requester header(final String name, final String value) {
        headers.add(name, value);
        return this;
    }

    public final Requester headers(final HttpHeaders headers) {
        this.headers.putAll(headers);
        return this;
    }

    public final Requester accept(final MediaType acceptableMediaType, final MediaType... acceptableMediaTypes) {
        headers.setAccept(Lists.asList(acceptableMediaType, acceptableMediaTypes));
        return this;
    }

    public final Requester contentType(final MediaType contentType) {
        headers.setContentType(contentType);
        return this;
    }

    public final <T> Dispatcher body(@Nullable final T body) {
        return execute(body);
    }

    @Override
    public CompletableFuture<Void> call(final Route route) {
        return execute(null).call(route);
    }

    private <T> Dispatcher execute(final @Nullable T body) {
        final ImmutableMultimap.Builder<String, String> builder = ImmutableMultimap.builder();
        headers.forEach(builder::putAll);

        return new ResponseDispatcher(new HttpEntity<>(body, headers), arguments
                .withQueryParams(ImmutableMultimap.copyOf(query))
                .withRequestUri()
                .withHeaders(builder.build())
                .withBody(body)
        );
    }

    private final class ResponseDispatcher extends Dispatcher {

        private final HttpEntity<?> entity;
        private final RequestArguments arguments;

        ResponseDispatcher(final HttpEntity<?> entity, final RequestArguments arguments) {
            this.entity = entity;
            this.arguments = arguments;
        }

        @Override
        public CompletableFuture<Void> call(final Route route) {
            try {
                final RequestExecution execution = plugin.prepare(arguments, () ->
                        execute(entity).thenApply(dispatch(route)));

                final CompletableFuture<ClientHttpResponse> future = execution.execute();

                // we need a CompletableFuture<Void>
                return future.thenApply(response -> null);
            } catch (final IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        private <T> CompletableFuture<ClientHttpResponse> execute(final HttpEntity<T> entity) throws IOException {
            final URI requestUri = arguments.getRequestUri();
            checkArgument(requestUri.isAbsolute(), "Request URI is not absolute");
            final HttpMethod method = arguments.getMethod();
            final AsyncClientHttpRequest request = requestFactory.createAsyncRequest(requestUri, method);
            worker.write(request, entity);
            return new ListenableCompletableFutureAdapter<>(request.executeAsync());
        }

        private ThrowingUnaryOperator<ClientHttpResponse, Exception> dispatch(final Route route) {
            return response -> {
                try {
                    route.execute(response, worker);
                } catch (final NoWildcardException e) {
                    throw new NoRouteException(response);
                }

                return response;
            };
        }

    }

}
