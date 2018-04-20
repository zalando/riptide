package org.zalando.riptide;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMultimap;
import org.apiguardian.api.API;
import org.springframework.http.HttpMethod;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.net.URI;

import static com.google.common.base.Preconditions.checkArgument;
import static org.apiguardian.api.API.Status.STABLE;
import static org.springframework.web.util.UriComponentsBuilder.fromUri;
import static org.springframework.web.util.UriComponentsBuilder.fromUriString;

@API(status = STABLE)
public interface RequestArguments {

    URI getBaseUrl();

    UrlResolution getUrlResolution();

    HttpMethod getMethod();

    String getUriTemplate();

    ImmutableList<Object> getUriVariables();

    URI getUri();

    ImmutableMultimap<String, String> getQueryParams();

    URI getRequestUri();

    ImmutableMultimap<String, String> getHeaders();

    Object getBody();

    RequestArguments withBaseUrl(@Nullable URI baseUrl);

    RequestArguments withUrlResolution(@Nullable UrlResolution resolution);

    RequestArguments withMethod(@Nullable HttpMethod method);

    RequestArguments withUriTemplate(@Nullable String uriTemplate);

    RequestArguments withUriVariables(@Nullable ImmutableList<Object> uriVariables);

    RequestArguments withUri(@Nullable URI uri);

    RequestArguments withQueryParams(@Nullable ImmutableMultimap<String, String> queryParams);

    RequestArguments withRequestUri(@Nullable URI requestUri);

    RequestArguments withHeaders(@Nullable ImmutableMultimap<String, String> headers);

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

        // encode query params
        final MultiValueMap<String, String> queryParams;
        {
            final UriComponentsBuilder components = UriComponentsBuilder.newInstance();
            getQueryParams().entries().forEach(entry ->
                    components.queryParam(entry.getKey(), entry.getValue()));
            queryParams = components.build().encode().getQueryParams();
        }

        // build request uri
        final URI requestUri = fromUri(resolvedUri)
                .queryParams(queryParams)
                .build(true).normalize().toUri();

        checkArgument(requestUri.isAbsolute(), "Request URI is not absolute");

        return withRequestUri(requestUri);
    }

    static RequestArguments create() {
        return new DefaultRequestArguments(null, null, null, null, ImmutableList.of(), null, ImmutableMultimap.of(),
                null, ImmutableMultimap.of(), null);
    }

}
