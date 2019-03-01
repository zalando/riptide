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

import javax.annotation.Nullable;
import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.lang.String.CASE_INSENSITIVE_ORDER;
import static org.apiguardian.api.API.Status.INTERNAL;

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

    @Getter
    @Wither
    URI requestUri;

    @Getter
    BaseMap<String, List<String>> headers;

    @Getter
    @Wither
    Object body;

    public DefaultRequestArguments() {
        this(null, null, null, null, PersistentVector.empty(), null, PersistentHashMap.empty(),
                PersistentHashMap.empty(), null, PersistentTreeMap.empty(CASE_INSENSITIVE_ORDER), null);
    }

    @Override
    public <T> Optional<T> getAttribute(final Attribute<T> attribute) {
        @SuppressWarnings("unchecked")
        @Nullable final T value = (T) attributes.get(attribute);
        return Optional.ofNullable(value);
    }

    @Override
    public RequestArguments replaceUriVariables(final List<Object> additionalUriVariables) {
        return new DefaultRequestArguments(
                method, baseUrl, urlResolution, uriTemplate, PersistentVector.ofIter(additionalUriVariables), uri,
                attributes, queryParams, requestUri, headers, body);
    }

    @Override
    public <T> RequestArguments withAttribute(final Attribute<T> attribute, final T value) {
        return new DefaultRequestArguments(
                method, baseUrl, urlResolution, uriTemplate, uriVariables, uri, attributes.assoc(attribute, value),
                queryParams, requestUri, headers, body);
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
                queryParams, requestUri, headers, body);
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
                requestUri, headers, body);
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
