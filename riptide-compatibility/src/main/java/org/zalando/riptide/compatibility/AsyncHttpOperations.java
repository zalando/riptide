package org.zalando.riptide.compatibility;

import com.google.common.reflect.TypeToken;
import lombok.AllArgsConstructor;
import org.apiguardian.api.API;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus.Series;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.web.client.AsyncRequestCallback;
import org.springframework.web.client.AsyncRestOperations;
import org.springframework.web.client.ResponseExtractor;
import org.springframework.web.client.RestOperations;
import org.zalando.riptide.Http;
import org.zalando.riptide.RequestArguments.Entity;
import org.zalando.riptide.Route;
import org.zalando.riptide.RoutingTree;
import org.zalando.riptide.capture.Capture;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.Type;
import java.net.URI;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import static lombok.AccessLevel.PRIVATE;
import static org.apiguardian.api.API.Status.EXPERIMENTAL;
import static org.springframework.http.HttpMethod.DELETE;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.HEAD;
import static org.springframework.http.HttpMethod.OPTIONS;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpMethod.PUT;
import static org.springframework.http.HttpStatus.Series.SUCCESSFUL;
import static org.zalando.riptide.Bindings.anySeries;
import static org.zalando.riptide.Bindings.on;
import static org.zalando.riptide.Navigators.series;
import static org.zalando.riptide.PassRoute.pass;
import static org.zalando.riptide.Route.call;
import static org.zalando.riptide.RoutingTree.dispatch;
import static org.zalando.riptide.Types.responseEntityOf;
import static org.zalando.riptide.compatibility.ExtractRoute.extractTo;
import static org.zalando.riptide.compatibility.UriVariables.extract;
import static org.zalando.riptide.problem.ProblemRoute.problemHandling;

@Nonnull
@API(status = EXPERIMENTAL)
@AllArgsConstructor(access = PRIVATE)
@SuppressWarnings({
        "deprecation", // usage of AsyncRestOperations
        "UnstableApiUsage", // usage of TypeToken
})
public final class AsyncHttpOperations implements AsyncRestOperations {

    private final Http http;
    private final RoutingTree<Series> defaultRoutingTree;

    public AsyncHttpOperations(final Http http) {
        this(http, dispatch(series(),
                anySeries().call(problemHandling())));
    }

    public AsyncHttpOperations withDefaultRoutingTree(final RoutingTree<Series> defaultRoutingTree) {
        return new AsyncHttpOperations(http, defaultRoutingTree);
    }

    @Override
    public RestOperations getRestOperations() {
        return new HttpOperations(http);
    }

    @Override
    public <T> ListenableFuture<ResponseEntity<T>> getForEntity(final String url, final Class<T> responseType,
            final Object... uriVariables) {
        return exchange(url, GET, null, responseType, uriVariables);
    }

    @Override
    public <T> ListenableFuture<ResponseEntity<T>> getForEntity(final String url, final Class<T> responseType,
            final Map<String, ?> uriVariables) {
        return exchange(url, GET, null, responseType, uriVariables);
    }

    @Override
    public <T> ListenableFuture<ResponseEntity<T>> getForEntity(final URI url, final Class<T> responseType) {
        return exchange(url, GET, null, responseType);
    }

    @Override
    public ListenableFuture<HttpHeaders> headForHeaders(final String url, final Object... uriVariables) {
        return execute(url, HEAD, pass(), ClientHttpResponse::getHeaders, uriVariables);
    }

    @Nonnull
    @Override
    public ListenableFuture<HttpHeaders> headForHeaders(final String url,
            final Map<String, ?> uriVariables) {
        return execute(url, HEAD, pass(), ClientHttpResponse::getHeaders, extract(url, uriVariables));
    }

    @Override
    public ListenableFuture<HttpHeaders> headForHeaders(final URI url) {
        return execute(url, HEAD, pass(), ClientHttpResponse::getHeaders);
    }

    @Override
    public ListenableFuture<URI> postForLocation(final String url, @Nullable final HttpEntity<?> entity,
            final Object... uriVariables) {
        return execute(url, POST, entity, pass(), response -> response.getHeaders().getLocation(), uriVariables);
    }

    @Nonnull
    @Override
    public ListenableFuture<URI> postForLocation(final String url, @Nullable final HttpEntity<?> entity,
            final Map<String, ?> uriVariables) {
        return execute(url, POST, entity, pass(), response -> response.getHeaders().getLocation(),
                extract(url, uriVariables));
    }

    @Override
    public ListenableFuture<URI> postForLocation(final URI url, @Nullable final HttpEntity<?> entity) {
        return execute(url, POST, entity, pass(), response -> response.getHeaders().getLocation());
    }

    @Override
    public <T> ListenableFuture<ResponseEntity<T>> postForEntity(final String url, @Nullable final HttpEntity<?> entity,
            final Class<T> responseType, final Object... uriVariables) {
        final Capture<ResponseEntity<T>> capture = Capture.empty();
        return execute(url, POST, entity, call(responseEntityOf(responseType), capture), capture, uriVariables);
    }

    @Nonnull
    @Override
    public <T> ListenableFuture<ResponseEntity<T>> postForEntity(final String url,
            @Nullable final HttpEntity<?> entity, final Class<T> responseType, final Map<String, ?> uriVariables) {
        final Capture<ResponseEntity<T>> capture = Capture.empty();
        return execute(url, POST, entity, call(responseEntityOf(responseType), capture), capture, extract(url, uriVariables));
    }

    @Override
    public <T> ListenableFuture<ResponseEntity<T>> postForEntity(final URI url, @Nullable final HttpEntity<?> entity,
            final Class<T> responseType) {
        return exchange(url, POST, entity, responseType);
    }

    @Override
    public ListenableFuture<?> put(final String url, @Nullable final HttpEntity<?> entity,
            final Object... uriVariables) {
        return exchange(url, PUT, entity, Void.class, uriVariables);
    }

    @Override
    public ListenableFuture<?> put(final String url, @Nullable final HttpEntity<?> entity,
            final Map<String, ?> uriVariables) {
        return exchange(url, PUT, entity, Void.class, uriVariables);
    }

    @Override
    public ListenableFuture<?> put(final URI url, @Nullable final HttpEntity<?> entity) {
        return exchange(url, PUT, entity, Void.class);
    }

    @Override
    public ListenableFuture<?> delete(final String url, final Object... uriVariables) {
        return execute(url, DELETE, pass(), response -> null, uriVariables);
    }

    @Override
    public ListenableFuture<?> delete(final String url, final Map<String, ?> uriVariables) {
        return execute(url, DELETE, pass(), response -> null, extract(url, uriVariables));
    }

    @Override
    public ListenableFuture<?> delete(final URI url) {
        return execute(url, DELETE, pass(), response -> null);
    }

    @Override
    public ListenableFuture<Set<HttpMethod>> optionsForAllow(final String url, final Object... uriVariables) {
        return execute(url, OPTIONS, pass(), response -> response.getHeaders().getAllow(), uriVariables);
    }

    @Override
    public ListenableFuture<Set<HttpMethod>> optionsForAllow(final String url, final Map<String, ?> uriVariables) {
        return execute(url, OPTIONS, pass(), response -> response.getHeaders().getAllow(), extract(url, uriVariables));
    }

    @Override
    public ListenableFuture<Set<HttpMethod>> optionsForAllow(final URI url) {
        return execute(url, OPTIONS, pass(), response -> response.getHeaders().getAllow());
    }

    @Override
    public <T> ListenableFuture<ResponseEntity<T>> exchange(final String url, final HttpMethod method,
            @Nullable final HttpEntity<?> entity, final Class<T> responseType, final Object... uriVariables) {
        return exchange(url, method, entity, forType(responseType), uriVariables);
    }

    @Override
    public <T> ListenableFuture<ResponseEntity<T>> exchange(final String url, final HttpMethod method,
            @Nullable final HttpEntity<?> entity, final Class<T> responseType, final Map<String, ?> uriVariables) {
        return exchange(url, method, entity, forType(responseType), uriVariables);
    }

    @Override
    public <T> ListenableFuture<ResponseEntity<T>> exchange(final URI url, final HttpMethod method,
            @Nullable final HttpEntity<?> entity, final Class<T> responseType) {

        return exchange(url, method, entity, forType(responseType));
    }

    @Override
    public <T> ListenableFuture<ResponseEntity<T>> exchange(final String url, final HttpMethod method,
            @Nullable final HttpEntity<?> entity, final ParameterizedTypeReference<T> responseType,
            final Object... uriVariables) {

        @SuppressWarnings("unchecked")
        final TypeToken<T> type = (TypeToken) TypeToken.of(responseType.getType());
        return exchange(url, method, entity, type, uriVariables);
    }

    private <T> ListenableFuture<ResponseEntity<T>> exchange(final String url, final HttpMethod method,
            @Nullable final HttpEntity<?> entity, final TypeToken<T> type, final Object... uriVariables) {
        final Capture<ResponseEntity<T>> capture = Capture.empty();
        return execute(url, method, entity, call(responseEntityOf(type), capture), capture, uriVariables);
    }

    @Override
    public <T> ListenableFuture<ResponseEntity<T>> exchange(final String url, final HttpMethod method,
            @Nullable final HttpEntity<?> entity, final ParameterizedTypeReference<T> responseType,
            final Map<String, ?> uriVariables) {
        return exchange(url, method, entity, responseType, extract(url, uriVariables));
    }

    @Override
    public <T> ListenableFuture<ResponseEntity<T>> exchange(final URI url, final HttpMethod method,
            @Nullable final HttpEntity<?> entity, final ParameterizedTypeReference<T> responseType) {

        @SuppressWarnings("unchecked")
        final TypeToken<T> type = (TypeToken) TypeToken.of(responseType.getType());
        return exchange(url, method, entity, type);
    }

    private <T> ListenableFuture<ResponseEntity<T>> exchange(final URI url, final HttpMethod method,
            @Nullable final HttpEntity<?> entity, final TypeToken<T> type) {
        final Capture<ResponseEntity<T>> capture = Capture.empty();
        return execute(url, method, entity, call(responseEntityOf(type), capture), capture);
    }

    @Override
    public <T> ListenableFuture<T> execute(final String url, final HttpMethod method,
            @Nullable final AsyncRequestCallback callback, @Nullable final ResponseExtractor<T> extractor,
            final Object... uriVariables) {

        final Capture<T> capture = Capture.empty();
        return execute(url, method, toEntity(callback), route(extractTo(extractor, capture)), capture, uriVariables);
    }

    @Nonnull
    @Override
    public <T> ListenableFuture<T> execute(final String url, final HttpMethod method,
            @Nullable final AsyncRequestCallback callback, @Nullable final ResponseExtractor<T> extractor,
            final Map<String, ?> uriVariables) {
        return execute(url, method, callback, extractor, extract(url, uriVariables));
    }

    @Nonnull
    @Override
    public <T> ListenableFuture<T> execute(final URI url, final HttpMethod method,
            @Nullable final AsyncRequestCallback callback, @Nullable final ResponseExtractor<T> extractor) {

        final Capture<T> capture = Capture.empty();
        return execute(url, method, toEntity(callback), route(extractTo(extractor, capture)), capture);
    }

    private static <T> ParameterizedTypeReference<T> forType(final Type type) {
        return new ParameterizedTypeReference<T>() {
            @Override
            public Type getType() {
                return type;
            }
        };
    }

    private <T> ListenableFuture<T> execute(final String url, final HttpMethod method,
            final Route route, final Function<ClientHttpResponse, T> function, final Object[] uriVariables) {
        return execute(url, method, (HttpEntity) null, route, function, uriVariables);
    }

    private <T> ListenableFuture<T> execute(final String url, final HttpMethod method,
            @Nullable final HttpEntity<?> entity, final Route route, final Function<ClientHttpResponse, T> function,
            final Object[] uriVariables) {

        final CompletableFuture<T> future = http.execute(method, url, uriVariables)
                .headers(getHeaders(entity))
                .body(getBody(entity))
                .call(route(route))
                .thenApply(function);

        return new CompletableToListenableFutureAdapter<>(future);
    }

    private <T> ListenableFuture<T> execute(final String url, final HttpMethod method,
            @Nullable final Entity entity, final Route route, final Function<ClientHttpResponse, T> function,
            final Object[] uriVariables) {

        final CompletableFuture<T> future = http.execute(method, url, uriVariables)
                .body(entity)
                .call(route(route))
                .thenApply(function);

        return new CompletableToListenableFutureAdapter<>(future);
    }

    private <T> ListenableFuture<T> execute(final URI url, final HttpMethod method,
            final Route route, final Function<ClientHttpResponse, T> function) {
        return execute(url, method, (HttpEntity) null, route, function);
    }

    private <T> ListenableFuture<T> execute(final URI url, final HttpMethod method,
            @Nullable final HttpEntity<?> entity, final Route route, final Function<ClientHttpResponse, T> function) {

        final CompletableFuture<T> future = http.execute(method, url)
                .headers(getHeaders(entity))
                .body(getBody(entity))
                .call(route(route))
                .thenApply(function);

        return new CompletableToListenableFutureAdapter<>(future);
    }

    private <T> ListenableFuture<T> execute(final URI url, final HttpMethod method,
            @Nullable final Entity entity, final Route route, final Function<ClientHttpResponse, T> function) {

        final CompletableFuture<T> future = http.execute(method, url)
                .body(entity)
                .call(route(route))
                .thenApply(function);

        return new CompletableToListenableFutureAdapter<>(future);
    }

    private HttpHeaders getHeaders(@Nullable final HttpEntity<?> entity) {
        return entity == null ? new HttpHeaders() : entity.getHeaders();
    }

    private Object getBody(@Nullable final HttpEntity<?> entity) {
        return entity == null ? null : entity.getBody();
    }

    private Route route(final Route route) {
        return defaultRoutingTree
                .merge(on(SUCCESSFUL).call(route));
    }

    @Nullable
    private Entity toEntity(@Nullable final AsyncRequestCallback callback) {
        if (callback == null) {
            return null;
        }

        return message ->
                callback.doWithRequest(new HttpOutputMessageAsyncClientHttpRequestAdapter(message));
    }

}
