package org.zalando.riptide;

import com.google.common.collect.ImmutableList;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.AsyncClientHttpRequestFactory;
import org.springframework.http.converter.HttpMessageConverter;

import javax.annotation.Nullable;
import java.net.URI;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

public final class Rest {

    private final AsyncClientHttpRequestFactory requestFactory;
    private final MessageWorker worker;
    private final RequestArguments arguments;
    private final Plugin plugin;

    Rest(final AsyncClientHttpRequestFactory requestFactory, final List<HttpMessageConverter<?>> converters,
            @Nullable final URI baseUrl, final Plugin plugin) {
        this.requestFactory = checkNotNull(requestFactory, "request factory");
        this.worker = new MessageWorker(converters);
        this.arguments = RequestArguments.create().withBaseUrl(baseUrl);
        this.plugin = plugin;
    }

    public final Requester get(final String uriTemplate, final Object... urlVariables) {
        return execute(HttpMethod.GET, uriTemplate, urlVariables);
    }

    public final Requester get(final URI uri) {
        return execute(HttpMethod.GET, uri);
    }

    public final Requester head(final String uriTemplate, final Object... urlVariables) {
        return execute(HttpMethod.HEAD, uriTemplate, urlVariables);
    }

    public final Requester head(final URI uri) {
        return execute(HttpMethod.HEAD, uri);
    }

    public final Requester post(final String uriTemplate, final Object... urlVariables) {
        return execute(HttpMethod.POST, uriTemplate, urlVariables);
    }

    public final Requester post(final URI uri) {
        return execute(HttpMethod.POST, uri);
    }

    public final Requester put(final String uriTemplate, final Object... urlVariables) {
        return execute(HttpMethod.PUT, uriTemplate, urlVariables);
    }

    public final Requester put(final URI uri) {
        return execute(HttpMethod.PUT, uri);
    }

    public final Requester patch(final String uriTemplate, final Object... urlVariables) {
        return execute(HttpMethod.PATCH, uriTemplate, urlVariables);
    }

    public final Requester patch(final URI uri) {
        return execute(HttpMethod.PATCH, uri);
    }

    public final Requester delete(final String uriTemplate, final Object... urlVariables) {
        return execute(HttpMethod.DELETE, uriTemplate, urlVariables);
    }

    public final Requester delete(final URI uri) {
        return execute(HttpMethod.DELETE, uri);
    }

    public final Requester options(final String uriTemplate, final Object... urlVariables) {
        return execute(HttpMethod.OPTIONS, uriTemplate, urlVariables);
    }

    public final Requester options(final URI uri) {
        return execute(HttpMethod.OPTIONS, uri);
    }

    public final Requester trace(final String uriTemplate, final Object... urlVariables) {
        return execute(HttpMethod.TRACE, uriTemplate, urlVariables);
    }

    public final Requester trace(final URI uri) {
        return execute(HttpMethod.TRACE, uri);
    }

    private Requester execute(final HttpMethod method, final URI uri) {
        return execute(arguments
                .withMethod(method)
                .withUri(uri));
    }

    private Requester execute(final HttpMethod method, final String uriTemplate, final Object... uriVariables) {
        return execute(arguments
                .withMethod(method)
                .withUriTemplate(uriTemplate)
                .withUriVariables(ImmutableList.copyOf(uriVariables)));
    }

    private Requester execute(final RequestArguments arguments) {
        return new Requester(requestFactory, worker, arguments, plugin);
    }

    public static RestBuilder builder() {
        return new RestBuilder();
    }

}
