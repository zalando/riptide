package org.zalando.riptide;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import org.apiguardian.api.API;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.client.AsyncClientHttpRequest;
import org.springframework.http.client.AsyncClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.concurrent.ListenableFuture;
import org.zalando.fauxpas.ThrowingUnaryOperator;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static java.util.Objects.nonNull;
import static org.apiguardian.api.API.Status.STABLE;
import static org.zalando.riptide.CancelableCompletableFuture.preserveCancelability;

@API(status = STABLE)
public final class Requester extends Dispatcher {

    private final AsyncClientHttpRequestFactory requestFactory;
    private final MessageWorker worker;
    private final RequestArguments arguments;
    private final Plugin plugin;

    private final Multimap<String, String> query = LinkedHashMultimap.create();
    private final HttpHeaders headers = new HttpHeaders();

    Requester(final AsyncClientHttpRequestFactory requestFactory, final MessageWorker worker,
            final RequestArguments arguments, final Plugin plugin) {
        this.requestFactory = requestFactory;
        this.worker = worker;
        this.arguments = arguments;
        this.plugin = plugin;
    }

    public Requester queryParam(final String name, final String value) {
        query.put(name, value);
        return this;
    }

    public Requester queryParams(final Multimap<String, String> params) {
        query.putAll(params);
        return this;
    }

    public Requester accept(final MediaType acceptableMediaType, final MediaType... acceptableMediaTypes) {
        headers.setAccept(Lists.asList(acceptableMediaType, acceptableMediaTypes));
        return this;
    }

    public Requester contentType(final MediaType contentType) {
        headers.setContentType(contentType);
        return this;
    }

    public Requester ifModifiedSince(final OffsetDateTime since) {
        headers.setIfModifiedSince(since.toInstant().toEpochMilli());
        return this;
    }

    public Requester ifUnmodifiedSince(final OffsetDateTime since) {
        headers.setIfUnmodifiedSince(since.toInstant().toEpochMilli());
        return this;
    }

    public Requester ifNoneMatch(final String... entityTags) {
        return ifNoneMatch(Arrays.asList(entityTags));
    }

    public Requester ifMatch(final String... entityTags) {
        return ifMatch(Arrays.asList(entityTags));
    }

    private Requester ifMatch(final List<String> entityTags) {
        headers.setIfMatch(entityTags);
        return this;
    }

    private Requester ifNoneMatch(final List<String> entityTags) {
        headers.setIfNoneMatch(entityTags);
        return this;
    }

    public Requester header(final String name, final String value) {
        headers.add(name, value);
        return this;
    }

    public Requester headers(final HttpHeaders headers) {
        this.headers.putAll(headers);
        return this;
    }

    public <T> Dispatcher body(@Nullable final T body) {
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
                final RequestExecution original = this::send;
                final RequestExecution before = plugin.interceptBeforeRouting(arguments, original);
                final RequestExecution after = plugin.interceptAfterRouting(arguments, dispatch(before, route));
                final CompletableFuture<ClientHttpResponse> future = after.execute();

                // TODO why not return CompletableFuture<ClientHttpResponse> here?
                // we need a CompletableFuture<Void>

                // TODO: replace with thenApply call in Java 9
                final CompletableFuture<Void> result = preserveCancelability(future);
                future.whenComplete((response, throwable) -> {
                    if (nonNull(response)) {
                        result.complete(null);
                    }
                    if (nonNull(throwable)) {
                        result.completeExceptionally(throwable);
                    }
                });
                return result;
            } catch (final IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        private CompletableFuture<ClientHttpResponse> send() throws IOException {
            final AsyncClientHttpRequest request = createRequest();
            worker.write(request, entity);
            final ListenableFuture<ClientHttpResponse> original = request.executeAsync();

            final CompletableFuture<ClientHttpResponse> future = preserveCancelability(original);
            original.addCallback(future::complete, future::completeExceptionally);
            return future;
        }

        private AsyncClientHttpRequest createRequest() throws IOException {
            final URI requestUri = arguments.getRequestUri();
            final HttpMethod method = arguments.getMethod();
            return requestFactory.createAsyncRequest(requestUri, method);
        }

        private RequestExecution dispatch(final RequestExecution execution, final Route route) {
            return () -> execution.execute().thenApply(dispatch(route));
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
