package org.zalando.riptide;

import com.google.common.net.UrlEscapers;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Singular;
import lombok.With;
import lombok.experimental.FieldDefaults;
import org.apiguardian.api.API;
import org.organicdesign.fp.collections.BaseMap;
import org.organicdesign.fp.collections.ImList;
import org.organicdesign.fp.collections.PersistentHashMap;
import org.organicdesign.fp.collections.PersistentTreeMap;
import org.organicdesign.fp.collections.PersistentVector;
import org.springframework.http.HttpMethod;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UriUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static com.google.common.base.Preconditions.checkArgument;
import static java.lang.String.CASE_INSENSITIVE_ORDER;
import static org.apiguardian.api.API.Status.INTERNAL;
import static org.springframework.web.util.UriComponentsBuilder.fromUriString;
import static org.springframework.web.util.UriUtils.encodeQueryParam;
import static org.zalando.fauxpas.FauxPas.throwingBiConsumer;
import static org.zalando.fauxpas.FauxPas.throwingConsumer;
import static org.zalando.riptide.UrlResolution.RFC;

@API(status = INTERNAL)
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@AllArgsConstructor
final class DefaultRequestArguments implements RequestArguments {

    @Getter
    @With
    HttpMethod method;

    @Getter
    @With
    URI baseUrl;

    @Getter
    @With
    UrlResolution urlResolution;

    @Getter
    @With
    String uriTemplate;

    @Getter
    @Singular
    ImList<Object> uriVariables;

    @Getter
    @With
    URI uri;

    BaseMap<Attribute<?>, Object> attributes;

    @Getter
    BaseMap<String, List<String>> queryParams;

    AtomicReference<URI> requestUri = new AtomicReference<>();

    @Getter
    BaseMap<String, List<String>> headers;

    @Getter
    @With
    Object body;

    @Getter
    @With
    Entity entity;

    @Getter
    @With
    Route route;

    DefaultRequestArguments() {
        this(null, null, RFC, null, PersistentVector.empty(), null, PersistentHashMap.empty(),
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
                    values.forEach(throwingConsumer(value ->
                            components.queryParam(key, encode(value))))));

            // build request uri
            final URI requestUri = components.uri(resolvedUri)
                    .build(true).normalize().toUri();

            checkArgument(requestUri.isAbsolute(), "Request URI is not absolute");

            return requestUri;
        });
    }

    /**
     * Older spring versions don't allow {@code +} signs in query parameters even though that is technically
     * allowed and valid. In order to have a consistent behavior across different Spring versions
     * they will be encoded as {@code %2B}.
     *
     * @see UriUtils#encodeQueryParam(String, String)
     * @see URLEncoder
     * @see UrlEscapers#urlFragmentEscaper()
     * @see <a href="https://tools.ietf.org/html/rfc3986">RFC 3986: Uniform Resource Identifier (URI): Generic Syntax</a>
     * @see <a href="https://www.w3.org/TR/html4/interact/forms.html#h-17.13.4">HTML 4 specification: Form content types</a>
     * @see <a href="https://github.com/spring-projects/spring-framework/issues/20750">spring-projects/spring-framework#20750</a>
     * @see <a href="https://github.com/spring-projects/spring-framework/issues/21259">spring-projects/spring-framework#21259</a>
     * @param value the query parameter value
     * @return the encoded value
     */
    private String encode(final String value) throws UnsupportedEncodingException {
        return encodeQueryParam(value, "UTF-8").replace("+", "%2B");
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
