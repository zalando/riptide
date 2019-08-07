package org.zalando.riptide;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Singular;
import lombok.experimental.FieldDefaults;
import lombok.experimental.Wither;
import org.apiguardian.api.API;
import org.organicdesign.fp.collections.BaseMap;
import org.organicdesign.fp.collections.ImList;
import org.organicdesign.fp.collections.PersistentHashMap;
import org.organicdesign.fp.collections.PersistentTreeMap;
import org.organicdesign.fp.collections.PersistentVector;
import org.springframework.http.HttpMethod;
import org.springframework.web.util.UriComponentsBuilder;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static com.google.common.base.Preconditions.checkArgument;
import static java.lang.String.CASE_INSENSITIVE_ORDER;
import static org.apiguardian.api.API.Status.INTERNAL;
import static org.springframework.web.util.UriComponentsBuilder.fromUriString;
import static org.springframework.web.util.UriUtils.encode;
import static org.zalando.fauxpas.FauxPas.throwingBiConsumer;

@API(status = INTERNAL)
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@AllArgsConstructor
final class DefaultRequestArguments implements RequestArguments {

    @Getter
    @Wither
    HttpMethod method;

    @Getter
    @Wither
    URI baseUrl;

    @Getter
    @Wither
    UrlResolution urlResolution;

    @Getter
    @Wither
    String uriTemplate;

    @Getter
    @Singular
    ImList<Object> uriVariables;

    @Getter
    @Wither
    URI uri;

    BaseMap<Attribute<?>, Object> attributes;

    @Getter
    BaseMap<String, List<String>> queryParams;

    AtomicReference<URI> requestUri = new AtomicReference<>();

    @Getter
    BaseMap<String, List<String>> headers;

    @Getter
    @Wither
    Object body;

    @Getter
    @Wither
    Entity entity;

    @Getter
    @Wither
    Route route;

    DefaultRequestArguments() {
        this(null, null, null, null, PersistentVector.empty(), null, PersistentHashMap.empty(),
                PersistentHashMap.empty(), PersistentTreeMap.empty(CASE_INSENSITIVE_ORDER), null, null, null);
    }

    @Override
    public <T> Optional<T> getAttribute(final Attribute<T> attribute) {
        @SuppressWarnings("unchecked")
        @Nullable final T value = (T) attributes.get(attribute);
        return Optional.ofNullable(value);
    }

    public URI getRequestUri() {
        /*
         * The construction of the request URI is deferred until someone actually needs it and then it's cached. From
         * the perspective of users of RequestArguments it's effectively immutable.
         *
         * This pattern gives us two benefits:
         *
         * 1. Plugins may inspect the current request URI and will always see the latest version.
         * 2. The URI is only constructed when needed, i.e. when nobody needs it it will be constructed exactly once
         *    during the network phase when the actual request is being executed.
         */
        return requestUri.updateAndGet(previous -> {
            if (previous != null) {
                return previous;
            }

            @Nullable final URI uri = getUri();
            @Nullable final URI unresolvedUri;

            if (uri == null) {
                final String uriTemplate = getUriTemplate();
                if (uriTemplate == null || uriTemplate.isEmpty()) {
                    unresolvedUri = null;
                } else {
                    // expand uri template
                    unresolvedUri = fromUriString(uriTemplate)
                            .buildAndExpand(getUriVariables().toArray())
                            .encode()
                            .toUri();
                }
            } else {
                unresolvedUri = uri;
            }

            @Nullable final URI baseUrl = getBaseUrl();
            @Nonnull final URI resolvedUri;

            if (unresolvedUri == null) {
                checkArgument(baseUrl != null, "Either Base URL or absolute Request URI is required");
                resolvedUri = baseUrl;
            } else if (baseUrl == null || unresolvedUri.isAbsolute()) {
                resolvedUri = unresolvedUri;
            } else {
                resolvedUri = getUrlResolution().resolve(baseUrl, unresolvedUri);
            }

            final UriComponentsBuilder components = UriComponentsBuilder.newInstance();
            // encode query params
            getQueryParams().forEach(throwingBiConsumer((key, values) ->
                    values.forEach(value ->
                            components.queryParam(key, encode(value, "UTF-8")))));

            // build request uri
            final URI requestUri = components.uri(resolvedUri)
                    .build(true).normalize().toUri();

            checkArgument(requestUri.isAbsolute(), "Request URI is not absolute");

            return requestUri;
        });
    }

    @Override
    public RequestArguments replaceUriVariables(final List<Object> additionalUriVariables) {
        return new DefaultRequestArguments(
                method, baseUrl, urlResolution, uriTemplate, PersistentVector.ofIter(additionalUriVariables), uri,
                attributes, queryParams, headers, body, entity, route);
    }

    @Override
    public <T> RequestArguments withAttribute(final Attribute<T> attribute, final T value) {
        return new DefaultRequestArguments(
                method, baseUrl, urlResolution, uriTemplate, uriVariables, uri, attributes.assoc(attribute, value),
                queryParams, headers, body, entity, route);
    }

    @Override
    public RequestArguments withQueryParam(final String name, final String value) {
        return queryParams(queryParams.assoc(name, getOrDefault(queryParams, name).append(value)));
    }

    @Override
    public RequestArguments withQueryParams(final Map<String, ? extends Collection<String>> additionalQueryParams) {
        return queryParams(merge(queryParams, additionalQueryParams));
    }

    @Override
    public RequestArguments withoutQueryParam(final String name) {
        return queryParams(queryParams.without(name));
    }

    @Override
    public RequestArguments replaceQueryParams(final Map<String, ? extends Collection<String>> queryParams) {
        return queryParams(merge(PersistentHashMap.empty(), queryParams));
    }

    private DefaultRequestArguments queryParams(final BaseMap<String, List<String>> queryParams) {
        return new DefaultRequestArguments(
                method, baseUrl, urlResolution, uriTemplate, uriVariables, uri, attributes,
                queryParams, headers, body, entity, route);
    }

    @Override
    public RequestArguments withHeader(final String name, final String value) {
        return headers(headers.assoc(name, getOrDefault(headers, name).append(value)));
    }

    @Override
    public RequestArguments withHeaders(final Map<String, ? extends Collection<String>> additionalHeaders) {
        return headers(merge(headers, additionalHeaders));
    }

    @Override
    public RequestArguments withoutHeader(final String name) {
        return headers(headers.without(name));
    }

    @Override
    public RequestArguments replaceHeaders(final Map<String, ? extends Collection<String>> headers) {
        return headers(merge(PersistentHashMap.empty(), headers));
    }

    private DefaultRequestArguments headers(final BaseMap<String, List<String>> headers) {
        return new DefaultRequestArguments(
                method, baseUrl, urlResolution, uriTemplate, uriVariables, uri, attributes, queryParams,
                headers, body, entity, route);
    }

    private BaseMap<String, List<String>> merge(final BaseMap<String, List<String>> map,
            final Map<String, ? extends Collection<String>> additions) {

        BaseMap<String, List<String>> result = map;

        for (final Map.Entry<String, ? extends Collection<String>> entry : additions.entrySet()) {
            result = result.assoc(entry.getKey(), getOrDefault(result, entry.getKey()).concat(entry.getValue()));
        }

        return result;
    }

    private ImList<String> getOrDefault(final BaseMap<String, List<String>> map, final String key) {
        return (ImList<String>) map.getOrDefault(key, PersistentVector.empty());
    }

}
