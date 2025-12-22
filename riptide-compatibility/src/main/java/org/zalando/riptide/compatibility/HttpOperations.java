package org.zalando.riptide.compatibility;

import com.google.common.reflect.TypeToken;
import lombok.AllArgsConstructor;
import org.apiguardian.api.API;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus.Series;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.Assert;
import org.springframework.web.client.RequestCallback;
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
import java.util.function.Function;

import static lombok.AccessLevel.PRIVATE;
import static org.apiguardian.api.API.Status.EXPERIMENTAL;
import static org.springframework.http.HttpMethod.DELETE;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.HEAD;
import static org.springframework.http.HttpMethod.OPTIONS;
import static org.springframework.http.HttpMethod.PATCH;
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
import static org.zalando.riptide.compatibility.UriVariables.extract;
import static org.zalando.riptide.problem.ProblemRoute.problemHandling;

@Nonnull
@API(status = EXPERIMENTAL)
@AllArgsConstructor(access = PRIVATE)
@SuppressWarnings("UnstableApiUsage") // usage of TypeToken
public final class HttpOperations implements RestOperations {

    private final Http http;
    private final RoutingTree<Series> defaultRoutingTree;

    public HttpOperations(final Http http) {
        this(http, dispatch(series(),
                anySeries().call(problemHandling())));
    }

    public HttpOperations withDefaultRoutingTree(final RoutingTree<Series> defaultRoutingTree) {
        return new HttpOperations(http, defaultRoutingTree);
    }

    @Override
    public <T> T getForObject(final String url, final Class<T> responseType, final Object... uriVariables) {
        return getForEntity(url, responseType, uriVariables).getBody();
    }

    @Override
    public <T> T getForObject(final String url, final Class<T> responseType, final Map<String, ?> uriVariables) {
        return getForEntity(url, responseType, uriVariables).getBody();
    }

    @Override
    public <T> T getForObject(final URI url, final Class<T> responseType) {
        return getForEntity(url, responseType).getBody();
    }

    @Override
    public <T> ResponseEntity<T> getForEntity(final String url, final Class<T> responseType,
            final Object... uriVariables) {
        return exchange(url, GET, null, responseType, uriVariables);
    }

    @Override
    public <T> ResponseEntity<T> getForEntity(final String url, final Class<T> responseType,
            final Map<String, ?> uriVariables) {
        return exchange(url, GET, null, responseType, uriVariables);
    }

    @Override
    public <T> ResponseEntity<T> getForEntity(final URI url, final Class<T> responseType) {
        return exchange(url, GET, null, responseType);
    }

    @Nonnull
    @Override
    public HttpHeaders headForHeaders(final String url, final Object... uriVariables) {
        return execute(url, HEAD, (HttpEntity) null, pass(), ClientHttpResponse::getHeaders, uriVariables);
    }

    @Nonnull
    @Override
    public HttpHeaders headForHeaders(final String url, final Map<String, ?> uriVariables) {
        return execute(url, HEAD, (HttpEntity) null, pass(), ClientHttpResponse::getHeaders,
                extract(url, uriVariables));
    }

    @Nonnull
    @Override
    public HttpHeaders headForHeaders(final URI url) {
        return execute(url, HEAD, (HttpEntity) null, pass(), ClientHttpResponse::getHeaders);
    }

    @Override
    public URI postForLocation(final String url, @Nullable final Object body, final Object... uriVariables) {
        return postForEntity(url, body, Void.class, uriVariables).getHeaders().getLocation();
    }

    @Override
    public URI postForLocation(final String url, @Nullable final Object body, final Map<String, ?> uriVariables) {
        return postForEntity(url, body, Void.class, uriVariables).getHeaders().getLocation();
    }

    @Override
    public URI postForLocation(final URI url, @Nullable final Object body) {
        return postForEntity(url, body, Void.class).getHeaders().getLocation();
    }

    @Override
    public <T> T postForObject(final String url, @Nullable final Object body, final Class<T> responseType,
            final Object... uriVariables) {
        return postForEntity(url, body, responseType, uriVariables).getBody();
    }

    @Override
    public <T> T postForObject(final String url, @Nullable final Object body, final Class<T> responseType,
            final Map<String, ?> uriVariables) {
        return postForEntity(url, body, responseType, uriVariables).getBody();
    }

    @Override
    public <T> T postForObject(final URI url, @Nullable final Object body, final Class<T> responseType) {
        return postForEntity(url, body, responseType).getBody();
    }

    @Nonnull
    @Override
    public <T> ResponseEntity<T> postForEntity(final String url, @Nullable final Object body,
            final Class<T> responseType, final Object... uriVariables) {
        return exchange(url, POST, new HttpEntity<>(body ), responseType, uriVariables);
    }

    @Nonnull
    @Override
    public <T> ResponseEntity<T> postForEntity(final String url, @Nullable final Object body,
            final Class<T> responseType, final Map<String, ?> uriVariables) {
        return exchange(url, POST, new HttpEntity<>(body), responseType, uriVariables);
    }

    @Nonnull
    @Override
    public <T> ResponseEntity<T> postForEntity(final URI url, @Nullable final Object body,
            final Class<T> responseType) {
        return exchange(url, POST, new HttpEntity<>(body), responseType);
    }

    @Override
    public void put(final String url, @Nullable final Object body, final Object... uriVariables) {
        exchange(url, PUT, new HttpEntity<>(body), Void.class, uriVariables);
    }

    @Override
    public void put(final String url, @Nullable final Object body, final Map<String, ?> uriVariables) {
        exchange(url, PUT, new HttpEntity<>(body), Void.class, uriVariables);
    }

    @Override
    public void put(final URI url, @Nullable final Object body) {
        exchange(url, PUT, new HttpEntity<>(body), Void.class);
    }

    @Override
    public <T> T patchForObject(final String url, @Nullable final Object body, final Class<T> responseType,
            final Object... uriVariables) {
        return exchange(url, PATCH, new HttpEntity<>(body), responseType, uriVariables).getBody();
    }

    @Override
    public <T> T patchForObject(final String url, @Nullable final Object body, final Class<T> responseType,
            final Map<String, ?> uriVariables) {
        return exchange(url, PATCH, new HttpEntity<>(body), responseType, uriVariables).getBody();
    }

    @Override
    public <T> T patchForObject(final URI url, @Nullable final Object body, final Class<T> responseType) {
        return exchange(url, PATCH, new HttpEntity<>(body), responseType).getBody();
    }

    @Override
    public void delete(final String url, final Object... uriVariables) {
        exchange(url, DELETE, null, Void.class, uriVariables);
    }

    @Override
    public void delete(final String url, final Map<String, ?> uriVariables) {
        exchange(url, DELETE, null, Void.class, uriVariables);
    }

    @Override
    public void delete(final URI url) {
        exchange(url, DELETE, null, Void.class);
    }

    @Nonnull
    @Override
    public Set<HttpMethod> optionsForAllow(final String url, final Object... uriVariables) {
        return execute(url, OPTIONS, (HttpEntity) null, pass(), response -> response.getHeaders().getAllow(),
                uriVariables);
    }

    @Nonnull
    @Override
    public Set<HttpMethod> optionsForAllow(final String url, final Map<String, ?> uriVariables) {
        return execute(url, OPTIONS, (HttpEntity) null, pass(), response -> response.getHeaders().getAllow(),
                extract(url, uriVariables));
    }

    @Nonnull
    @Override
    public Set<HttpMethod> optionsForAllow(final URI url) {
        return execute(url, OPTIONS, (HttpEntity) null, pass(), response -> response.getHeaders().getAllow());
    }

    @Nonnull
    @Override
    public <T> ResponseEntity<T> exchange(final String url, final HttpMethod method,
            @Nullable final HttpEntity<?> entity, final Class<T> responseType, final Object... uriVariables) {
        return exchange(url, method, entity, forType(responseType), uriVariables);
    }

    @Nonnull
    @Override
    public <T> ResponseEntity<T> exchange(final String url, final HttpMethod method,
            @Nullable final HttpEntity<?> entity, final Class<T> responseType, final Map<String, ?> uriVariables) {
        return exchange(url, method, entity, forType(responseType), uriVariables);
    }

    @Nonnull
    @Override
    public <T> ResponseEntity<T> exchange(final URI url, final HttpMethod method, @Nullable final HttpEntity<?> entity,
            final Class<T> responseType) {

        return exchange(url, method, entity, forType(responseType));
    }

    @Nonnull
    @Override
    public <T> ResponseEntity<T> exchange(final String url, final HttpMethod method,
            @Nullable final HttpEntity<?> entity, final ParameterizedTypeReference<T> responseType,
            final Object... uriVariables) {

        @SuppressWarnings("unchecked") final TypeToken<T> type = (TypeToken) TypeToken.of(responseType.getType());
        return exchange(url, method, entity, type, uriVariables);
    }

    @Nonnull
    @Override
    public <T> ResponseEntity<T> exchange(final String url, final HttpMethod method,
            @Nullable final HttpEntity<?> entity, final ParameterizedTypeReference<T> responseType,
            final Map<String, ?> uriVariables) {

        @SuppressWarnings("unchecked") final TypeToken<T> type = (TypeToken) TypeToken.of(responseType.getType());
        return exchange(url, method, entity, type, extract(url, uriVariables));
    }

    @Nonnull
    @Override
    public <T> ResponseEntity<T> exchange(final URI url, final HttpMethod method, @Nullable final HttpEntity<?> entity,
            final ParameterizedTypeReference<T> responseType) {
        @SuppressWarnings("unchecked") final TypeToken<T> type = (TypeToken<T>) TypeToken.of(responseType.getType());
        return exchange(url, method, entity, type);
    }

    @Nonnull
    @Override
    public <T> ResponseEntity<T> exchange(final RequestEntity<?> entity, final Class<T> responseType) {
        return exchange(entity, forType(responseType));
    }

    @Nonnull
    @Override
    public <T> ResponseEntity<T> exchange(final RequestEntity<?> entity,
            final ParameterizedTypeReference<T> responseType) {
        final HttpMethod method = entity.getMethod();
        Assert.notNull(method, "HttpMethod is required");
        @SuppressWarnings("unchecked") final TypeToken<T> type = (TypeToken<T>) TypeToken.of(responseType.getType());
        return exchange(entity.getUrl(), method, entity, type);
    }

    @Override
    public <T> T execute(final String url, final HttpMethod method, @Nullable final RequestCallback callback,
            @Nullable final ResponseExtractor<T> extractor, final Object... uriVariables) {
        final Capture<T> capture = Capture.empty();
        return execute(url, method, toEntity(callback), ExtractRoute.extractTo(extractor, capture), capture, uriVariables);
    }

    @Override
    public <T> T execute(final String url, final HttpMethod method, @Nullable final RequestCallback callback,
            @Nullable final ResponseExtractor<T> extractor, final Map<String, ?> uriVariables) {
        final Capture<T> capture = Capture.empty();
        return execute(url, method, toEntity(callback), ExtractRoute.extractTo(extractor, capture), capture,
                extract(url, uriVariables));
    }

    @Override
    public <T> T execute(final URI url, final HttpMethod method, @Nullable final RequestCallback callback,
            @Nullable final ResponseExtractor<T> extractor) {
        final Capture<T> capture = Capture.empty();
        return execute(url, method, toEntity(callback), ExtractRoute.extractTo(extractor, capture), capture);
    }

    private static <T> ParameterizedTypeReference<T> forType(final Type type) {
        return new ParameterizedTypeReference<T>() {
            @Override
            public Type getType() {
                return type;
            }
        };
    }

    private <T> ResponseEntity<T> exchange(final String url, final HttpMethod method,
            @Nullable final HttpEntity<?> entity, final TypeToken<T> type, final Object[] uriVariables) {
        final Capture<ResponseEntity<T>> capture = Capture.empty();
        return execute(url, method, entity, call(responseEntityOf(type), capture), capture, uriVariables);
    }

    private <T> ResponseEntity<T> exchange(final URI url, final HttpMethod method, @Nullable final HttpEntity<?> entity,
            final TypeToken<T> type) {
        final Capture<ResponseEntity<T>> capture = Capture.empty();
        return execute(url, method, entity, call(responseEntityOf(type), capture), capture);
    }

    private <T> T execute(final String url, final HttpMethod method,
            @Nullable final HttpEntity<?> entity, final Route route, final Function<ClientHttpResponse, T> function,
            final Object[] uriVariables) {

        return http.execute(method, url, uriVariables)
                .headers(getHeaders(entity).asMultiValueMap())
                .body(getBody(entity))
                .call(route(route))
                .thenApply(function).join();
    }

    private <T> T execute(final String url, final HttpMethod method,
            @Nullable final Entity entity, final Route route, final Function<ClientHttpResponse, T> function,
            final Object[] uriVariables) {

        return http.execute(method, url, uriVariables)
                .body(entity)
                .call(route(route))
                .thenApply(function).join();
    }

    private <T> T execute(final URI url, final HttpMethod method,
            @Nullable final Entity entity, final Route route, final Function<ClientHttpResponse, T> function) {

        return http.execute(method, url)
                .body(entity)
                .call(route(route))
                .thenApply(function).join();
    }

    private <T> T execute(final URI url, final HttpMethod method,
            @Nullable final HttpEntity<?> entity, final Route route, final Function<ClientHttpResponse, T> function) {

        return http.execute(method, url)
                .headers(getHeaders(entity).asMultiValueMap())
                .body(getBody(entity))
                .call(route(route))
                .thenApply(function).join();
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
    private Entity toEntity(@Nullable final RequestCallback callback) {
        if (callback == null) {
            return null;
        }

        return message ->
                callback.doWithRequest(new HttpOutputMessageClientHttpRequestAdapter(message));
    }

}
