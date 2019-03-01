package org.zalando.riptide;

import org.apiguardian.api.API;
import org.springframework.http.HttpMethod;
import org.springframework.web.util.UriComponentsBuilder;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkArgument;
import static org.apiguardian.api.API.Status.STABLE;
import static org.springframework.web.util.UriComponentsBuilder.fromUriString;
import static org.springframework.web.util.UriUtils.encode;
import static org.zalando.fauxpas.FauxPas.throwingBiConsumer;

@API(status = STABLE)
public interface RequestArguments {

    URI getBaseUrl();

    UrlResolution getUrlResolution();

    HttpMethod getMethod();

    String getUriTemplate();

    List<Object> getUriVariables();

    URI getUri();

    <T> Optional<T> getAttribute(Attribute<T> attribute);

    Map<String, List<String>> getQueryParams();

    URI getRequestUri();

    Map<String, List<String>> getHeaders();

    Object getBody();

    RequestArguments withBaseUrl(@Nullable URI baseUrl);

    RequestArguments withUrlResolution(@Nullable UrlResolution resolution);

    RequestArguments withMethod(@Nullable HttpMethod method);

    RequestArguments withUriTemplate(@Nullable String uriTemplate);

    RequestArguments replaceUriVariables(List<Object> uriVariables);

    RequestArguments withUri(@Nullable URI uri);

    <T> RequestArguments withAttribute(Attribute<T> attribute, T value);

    RequestArguments withQueryParam(String name, String value);

    RequestArguments withQueryParams(Map<String, ? extends Collection<String>> queryParams);

    RequestArguments withoutQueryParam(String name);

    RequestArguments replaceQueryParams(Map<String, ? extends Collection<String>> queryParams);

    RequestArguments withRequestUri(@Nullable URI requestUri);

    RequestArguments withHeader(String name, String value);

    RequestArguments withHeaders(Map<String, ? extends Collection<String>> headers);

    RequestArguments withoutHeader(String name);

    RequestArguments replaceHeaders(Map<String, ? extends Collection<String>> headers);

    RequestArguments withBody(@Nullable Object body);

    default RequestArguments withRequestUri() {
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

        return withRequestUri(requestUri);
    }

    static RequestArguments create() {
        return new DefaultRequestArguments();
    }

}
