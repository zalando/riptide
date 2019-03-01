package org.zalando.riptide;

import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.converter.HttpMessageConverter;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

final class DefaultHttp implements Http {

    private final RequestExecution network;
    private final MessageReader reader;
    private final Supplier<URI> baseUrlProvider;
    private final RequestArguments arguments;
    private final Plugin plugin;

    DefaultHttp(final ClientHttpRequestFactory requestFactory,
            final List<HttpMessageConverter<?>> converters,
            final Supplier<URI> baseUrlProvider, final UrlResolution resolution, final Plugin plugin) {
        this.reader = new DefaultMessageReader(converters);
        this.network = new NetworkRequestExecution(requestFactory, new DefaultMessageWriter(converters));
        this.baseUrlProvider = requireNonNull(baseUrlProvider, "base url provider");
        this.arguments = RequestArguments.create().withUrlResolution(resolution);
        this.plugin = plugin;
    }

    @Override
    public final AttributeStage get(final String uriTemplate, final Object... urlVariables) {
        return execute(HttpMethod.GET, uriTemplate, urlVariables);
    }

    @Override
    public final AttributeStage get(final URI uri) {
        return execute(HttpMethod.GET, uri);
    }

    @Override
    public final AttributeStage get() {
        return execute(HttpMethod.GET);
    }

    @Override
    public final AttributeStage head(final String uriTemplate, final Object... urlVariables) {
        return execute(HttpMethod.HEAD, uriTemplate, urlVariables);
    }

    @Override
    public final AttributeStage head(final URI uri) {
        return execute(HttpMethod.HEAD, uri);
    }

    @Override
    public final AttributeStage head() {
        return execute(HttpMethod.HEAD);
    }

    @Override
    public final AttributeStage post(final String uriTemplate, final Object... urlVariables) {
        return execute(HttpMethod.POST, uriTemplate, urlVariables);
    }

    @Override
    public final AttributeStage post(final URI uri) {
        return execute(HttpMethod.POST, uri);
    }

    @Override
    public final AttributeStage post() {
        return execute(HttpMethod.POST);
    }

    @Override
    public final AttributeStage put(final String uriTemplate, final Object... urlVariables) {
        return execute(HttpMethod.PUT, uriTemplate, urlVariables);
    }

    @Override
    public final AttributeStage put(final URI uri) {
        return execute(HttpMethod.PUT, uri);
    }

    @Override
    public final AttributeStage put() {
        return execute(HttpMethod.PUT);
    }

    @Override
    public final AttributeStage patch(final String uriTemplate, final Object... urlVariables) {
        return execute(HttpMethod.PATCH, uriTemplate, urlVariables);
    }

    @Override
    public final AttributeStage patch(final URI uri) {
        return execute(HttpMethod.PATCH, uri);
    }

    @Override
    public final AttributeStage patch() {
        return execute(HttpMethod.PATCH);
    }

    @Override
    public final AttributeStage delete(final String uriTemplate, final Object... urlVariables) {
        return execute(HttpMethod.DELETE, uriTemplate, urlVariables);
    }

    @Override
    public final AttributeStage delete(final URI uri) {
        return execute(HttpMethod.DELETE, uri);
    }

    @Override
    public final AttributeStage delete() {
        return execute(HttpMethod.DELETE);
    }

    @Override
    public final AttributeStage options(final String uriTemplate, final Object... urlVariables) {
        return execute(HttpMethod.OPTIONS, uriTemplate, urlVariables);
    }

    @Override
    public final AttributeStage options(final URI uri) {
        return execute(HttpMethod.OPTIONS, uri);
    }

    @Override
    public final AttributeStage options() {
        return execute(HttpMethod.OPTIONS);
    }

    @Override
    public final AttributeStage trace(final String uriTemplate, final Object... urlVariables) {
        return execute(HttpMethod.TRACE, uriTemplate, urlVariables);
    }

    @Override
    public final AttributeStage trace(final URI uri) {
        return execute(HttpMethod.TRACE, uri);
    }

    @Override
    public final AttributeStage trace() {
        return execute(HttpMethod.TRACE);
    }

    @Override
    public AttributeStage execute(final HttpMethod method, final String uriTemplate, final Object... uriVariables) {
        return execute(arguments
                .withMethod(method)
                .withBaseUrl(baseUrlProvider.get())
                .withUriTemplate(uriTemplate)
                .replaceUriVariables(Arrays.asList(uriVariables)));
    }

    @Override
    public AttributeStage execute(final HttpMethod method, final URI uri) {
        return execute(arguments
                .withMethod(method)
                .withBaseUrl(baseUrlProvider.get())
                .withUri(uri));
    }

    @Override
    public AttributeStage execute(final HttpMethod method) {
        return execute(arguments
                .withMethod(method)
                .withBaseUrl(baseUrlProvider.get()));
    }

    private AttributeStage execute(final RequestArguments arguments) {
        return new Requester(network, reader, arguments, plugin);
    }

}
