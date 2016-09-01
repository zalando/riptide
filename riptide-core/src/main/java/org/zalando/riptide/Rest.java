package org.zalando.riptide;

import org.springframework.http.HttpMethod;
import org.springframework.http.client.AsyncClientHttpRequestFactory;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.util.UriComponentsBuilder;

import javax.annotation.Nullable;
import java.net.URI;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.springframework.web.util.UriComponentsBuilder.fromUri;
import static org.springframework.web.util.UriComponentsBuilder.fromUriString;

public final class Rest {

    private final AsyncClientHttpRequestFactory requestFactory;
    private final MessageWorker worker;
    private final String baseUrl;

    Rest(final AsyncClientHttpRequestFactory requestFactory, final List<HttpMessageConverter<?>> converters,
            @Nullable final String baseUrl) {
        this.requestFactory = checkNotNull(requestFactory, "request factory");
        this.worker = new MessageWorker(converters);
        this.baseUrl = baseUrl;
    }

    public final Requester get(final String urlTemplate, final Object... urlVariables) {
        return execute(HttpMethod.GET, urlTemplate, urlVariables);
    }

    public final Requester get(final URI url) {
        return execute(HttpMethod.GET, url);
    }

    public final Requester head(final String urlTemplate, final Object... urlVariables) {
        return execute(HttpMethod.HEAD, urlTemplate, urlVariables);
    }

    public final Requester head(final URI url) {
        return execute(HttpMethod.HEAD, url);
    }

    public final Requester post(final String urlTemplate, final Object... urlVariables) {
        return execute(HttpMethod.POST, urlTemplate, urlVariables);
    }

    public final Requester post(final URI url) {
        return execute(HttpMethod.POST, url);
    }

    public final Requester put(final String urlTemplate, final Object... urlVariables) {
        return execute(HttpMethod.PUT, urlTemplate, urlVariables);
    }

    public final Requester put(final URI url) {
        return execute(HttpMethod.PUT, url);
    }

    public final Requester patch(final String urlTemplate, final Object... urlVariables) {
        return execute(HttpMethod.PATCH, urlTemplate, urlVariables);
    }

    public final Requester patch(final URI url) {
        return execute(HttpMethod.PATCH, url);
    }

    public final Requester delete(final String urlTemplate, final Object... urlVariables) {
        return execute(HttpMethod.DELETE, urlTemplate, urlVariables);
    }

    public final Requester delete(final URI url) {
        return execute(HttpMethod.DELETE, url);
    }

    public final Requester options(final String urlTemplate, final Object... urlVariables) {
        return execute(HttpMethod.OPTIONS, urlTemplate, urlVariables);
    }

    public final Requester options(final URI url) {
        return execute(HttpMethod.OPTIONS, url);
    }

    public final Requester trace(final String urlTemplate, final Object... urlVariables) {
        return execute(HttpMethod.TRACE, urlTemplate, urlVariables);
    }

    public final Requester trace(final URI url) {
        return execute(HttpMethod.TRACE, url);
    }

    private Requester execute(final HttpMethod method, final String uriTemplate,
            final Object... uriVariables) {
        return new Requester(requestFactory, worker, method, mergeUrl(baseUrl, uriTemplate), uriVariables);
    }

    private Requester execute(final HttpMethod method, final URI url) {
        return new Requester(requestFactory, worker, method, fromUri(url));
    }

    private static UriComponentsBuilder mergeUrl(@Nullable final String baseUrl, final String uriTemplate) {
        final UriComponentsBuilder builder = fromUriString(uriTemplate);

        if (baseUrl != null && builder.build().getHost() == null) {
            return fromUriString(baseUrl + uriTemplate);
        }
        return builder;
    }

    public static RestBuilder builder() {
        return new RestBuilder();
    }

}
