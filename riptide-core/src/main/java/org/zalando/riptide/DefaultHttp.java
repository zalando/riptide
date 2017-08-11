package org.zalando.riptide;

import com.google.common.collect.ImmutableList;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.AsyncClientHttpRequestFactory;
import org.springframework.http.converter.HttpMessageConverter;

import java.net.URI;
import java.util.List;
import java.util.function.Supplier;

import static com.google.common.base.Preconditions.checkNotNull;

final class DefaultHttp implements Http {

    private final AsyncClientHttpRequestFactory requestFactory;
    private final MessageWorker worker;
    private final Supplier<URI> baseUrlProvider;
    private final RequestArguments arguments;
    private final Plugin plugin;

    DefaultHttp(final AsyncClientHttpRequestFactory requestFactory, final List<HttpMessageConverter<?>> converters,
            final Supplier<URI> baseUrlProvider, final UrlResolution resolution, final Plugin plugin) {
        this.requestFactory = checkNotNull(requestFactory, "request factory");
        this.worker = new MessageWorker(converters);
        this.baseUrlProvider = checkNotNull(baseUrlProvider, "base url provider");
        this.arguments = RequestArguments.create().withUrlResolution(resolution);
        this.plugin = plugin;
    }

    @Override
    public final Requester get(final String uriTemplate, final Object... urlVariables) {
        return execute(HttpMethod.GET, uriTemplate, urlVariables);
    }

    @Override
    public final Requester get(final URI uri) {
        return execute(HttpMethod.GET, uri);
    }

    @Override
    public final Requester get() {
        return execute(HttpMethod.GET);
    }

    @Override
    public final Requester head(final String uriTemplate, final Object... urlVariables) {
        return execute(HttpMethod.HEAD, uriTemplate, urlVariables);
    }

    @Override
    public final Requester head(final URI uri) {
        return execute(HttpMethod.HEAD, uri);
    }

    @Override
    public final Requester head() {
        return execute(HttpMethod.HEAD);
    }

    @Override
    public final Requester post(final String uriTemplate, final Object... urlVariables) {
        return execute(HttpMethod.POST, uriTemplate, urlVariables);
    }

    @Override
    public final Requester post(final URI uri) {
        return execute(HttpMethod.POST, uri);
    }

    @Override
    public final Requester post() {
        return execute(HttpMethod.POST);
    }

    @Override
    public final Requester put(final String uriTemplate, final Object... urlVariables) {
        return execute(HttpMethod.PUT, uriTemplate, urlVariables);
    }

    @Override
    public final Requester put(final URI uri) {
        return execute(HttpMethod.PUT, uri);
    }

    @Override
    public final Requester put() {
        return execute(HttpMethod.PUT);
    }

    @Override
    public final Requester patch(final String uriTemplate, final Object... urlVariables) {
        return execute(HttpMethod.PATCH, uriTemplate, urlVariables);
    }

    @Override
    public final Requester patch(final URI uri) {
        return execute(HttpMethod.PATCH, uri);
    }

    @Override
    public final Requester patch() {
        return execute(HttpMethod.PATCH);
    }

    @Override
    public final Requester delete(final String uriTemplate, final Object... urlVariables) {
        return execute(HttpMethod.DELETE, uriTemplate, urlVariables);
    }

    @Override
    public final Requester delete(final URI uri) {
        return execute(HttpMethod.DELETE, uri);
    }

    @Override
    public final Requester delete() {
        return execute(HttpMethod.DELETE);
    }

    @Override
    public final Requester options(final String uriTemplate, final Object... urlVariables) {
        return execute(HttpMethod.OPTIONS, uriTemplate, urlVariables);
    }

    @Override
    public final Requester options(final URI uri) {
        return execute(HttpMethod.OPTIONS, uri);
    }

    @Override
    public final Requester options() {
        return execute(HttpMethod.OPTIONS);
    }

    @Override
    public final Requester trace(final String uriTemplate, final Object... urlVariables) {
        return execute(HttpMethod.TRACE, uriTemplate, urlVariables);
    }

    @Override
    public final Requester trace(final URI uri) {
        return execute(HttpMethod.TRACE, uri);
    }

    @Override
    public final Requester trace() {
        return execute(HttpMethod.TRACE);
    }

    @Override
    public Requester execute(final HttpMethod method, final String uriTemplate, final Object... uriVariables) {
        return execute(arguments
                .withMethod(method)
                .withBaseUrl(baseUrlProvider.get())
                .withUriTemplate(uriTemplate)
                .withUriVariables(ImmutableList.copyOf(uriVariables)));
    }

    @Override
    public Requester execute(final HttpMethod method, final URI uri) {
        return execute(arguments
                .withMethod(method)
                .withBaseUrl(baseUrlProvider.get())
                .withUri(uri));
    }

    @Override
    public Requester execute(final HttpMethod method) {
        return execute(arguments
                .withMethod(method)
                .withBaseUrl(baseUrlProvider.get()));
    }

    private Requester execute(final RequestArguments arguments) {
        return new Requester(requestFactory, worker, arguments, plugin);
    }

}
