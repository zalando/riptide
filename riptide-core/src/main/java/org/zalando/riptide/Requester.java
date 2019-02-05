package org.zalando.riptide;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpResponse;
import org.zalando.fauxpas.ThrowingUnaryOperator;

import javax.annotation.Nullable;
import java.io.IOException;
import java.net.URI;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import static org.zalando.fauxpas.FauxPas.throwingFunction;
import static org.zalando.fauxpas.FauxPas.throwingRunnable;

final class Requester extends QueryStage {

    private final Executor executor;
    private final ClientHttpRequestFactory requestFactory;
    private final MessageWorker worker;
    private final RequestArguments arguments;
    private final Plugin plugin;

    private final ImmutableMultimap<String, String> query;
    private final HttpHeaders headers;

    Requester(final Executor executor, final ClientHttpRequestFactory requestFactory,
            final MessageWorker worker, final RequestArguments arguments, final Plugin plugin,
            final ImmutableMultimap<String, String> query,
            final HttpHeaders headers) {
        this.executor = executor;
        this.requestFactory = requestFactory;
        this.worker = worker;
        this.arguments = arguments;
        this.plugin = plugin;
        this.query = query;
        this.headers = headers;
    }

    private Requester(final Requester requester, final ImmutableMultimap<String,String> query,
            final HttpHeaders headers) {
        this(requester.executor, requester.requestFactory, requester.worker, requester.arguments,
                requester.plugin, query, headers);
    }

    @Override
    public QueryStage queryParam(final String name, final String value) {
       return new Requester(this, ImmutableMultimap.<String, String>builder().putAll(query).put(name, value).build(), headers);
    }

    @Override
    public QueryStage queryParams(final Multimap<String, String> params) {
        return new Requester(this, ImmutableMultimap.<String, String>builder().putAll(query).putAll(params).build(), headers);
    }

    @Override
    public HeaderStage accept(final MediaType acceptableMediaType, final MediaType... acceptableMediaTypes) {
        final HttpHeaders headers = copyHeaders();
        headers.setAccept(Lists.asList(acceptableMediaType, acceptableMediaTypes));
        return new Requester(this, query, headers);
    }

    @Override
    public HeaderStage contentType(final MediaType contentType) {
        final HttpHeaders headers = copyHeaders();
        headers.setContentType(contentType);
        return new Requester(this, query, headers);
    }

    @Override
    public HeaderStage ifModifiedSince(final OffsetDateTime since) {
        final HttpHeaders headers = copyHeaders();
        headers.setIfModifiedSince(since.toInstant().toEpochMilli());
        return new Requester(this, query, headers);
    }

    @Override
    public HeaderStage ifUnmodifiedSince(final OffsetDateTime since) {
        final HttpHeaders headers = copyHeaders();
        headers.setIfUnmodifiedSince(since.toInstant().toEpochMilli());
        return new Requester(this, query, headers);
    }

    @Override
    public HeaderStage ifNoneMatch(final String... entityTags) {
        return ifNoneMatch(Arrays.asList(entityTags));
    }

    @Override
    public HeaderStage ifMatch(final String... entityTags) {
        return ifMatch(Arrays.asList(entityTags));
    }

    private HeaderStage ifMatch(final List<String> entityTags) {
        final HttpHeaders headers = copyHeaders();
        headers.setIfMatch(entityTags);
        return new Requester(this, query, headers);
    }

    private HeaderStage ifNoneMatch(final List<String> entityTags) {
        final HttpHeaders headers = copyHeaders();
        headers.setIfNoneMatch(entityTags);
        return new Requester(this, query, headers);
    }

    @Override
    public HeaderStage header(final String name, final String value) {
        final HttpHeaders headers = copyHeaders();
        headers.add(name, value);
        return new Requester(this, query, headers);
    }

    @Override
    public HeaderStage headers(final HttpHeaders headers) {
        final HttpHeaders copy = copyHeaders();
        copy.putAll(headers);
        return new Requester(this, query, copy);
    }

    private HttpHeaders copyHeaders() {
        final HttpHeaders copy = new HttpHeaders();
        copy.putAll(headers);
        return copy;
    }

    @Override
    public CompletableFuture<ClientHttpResponse> call(final Route route) {
        return body(null).call(route);
    }

    @Override
    public <T> DispatchStage body(@Nullable final T body) {
        final ImmutableMultimap.Builder<String, String> builder = ImmutableMultimap.builder();
        headers.forEach(builder::putAll);

        return new ResponseDispatcher(new HttpEntity<>(body, headers), arguments
                .withQueryParams(ImmutableMultimap.copyOf(query))
                .withRequestUri()
                .withHeaders(builder.build())
                .withBody(body)
        );
    }

    private final class ResponseDispatcher extends DispatchStage {

        private final HttpEntity<?> entity;
        private final RequestArguments arguments;

        ResponseDispatcher(final HttpEntity<?> entity, final RequestArguments arguments) {
            this.entity = entity;
            this.arguments = arguments;
        }

        @Override
        public CompletableFuture<ClientHttpResponse> call(final Route route) {
            final RequestExecution before = plugin.beforeSend(this::send);
            final RequestExecution after = plugin.beforeDispatch(dispatch(before, route));

            // TODO get rid of this
            return throwingFunction(after::execute).apply(arguments);
        }

        private CompletableFuture<ClientHttpResponse> send(final RequestArguments arguments) {
            final CompletableFuture<ClientHttpResponse> future = new CompletableFuture<>();

            executor.execute(throwingRunnable(() -> {
                try {
                    final ClientHttpRequest request = createRequest(arguments);

                    worker.write(request, entity);

                    future.complete(request.execute());
                } catch (final Exception e) {
                    future.completeExceptionally(e);
                }
            }));

            return future;
        }

        private ClientHttpRequest createRequest(final RequestArguments arguments) throws IOException {
            final URI requestUri = arguments.getRequestUri();
            final HttpMethod method = arguments.getMethod();
            return requestFactory.createRequest(requestUri, method);
        }

        private RequestExecution dispatch(final RequestExecution execution, final Route route) {
            return arguments -> execution.execute(arguments).thenApply(dispatch(route));
        }

        private ThrowingUnaryOperator<ClientHttpResponse, Exception> dispatch(final Route route) {
            return response -> {
                try {
                    route.execute(response, worker);
                } catch (final NoWildcardException e) {
                    throw new UnexpectedResponseException(response);
                }

                return response;
            };
        }

    }

}
