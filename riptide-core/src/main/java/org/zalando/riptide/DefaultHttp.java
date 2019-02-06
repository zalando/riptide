package org.zalando.riptide;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMultimap;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.converter.HttpMessageConverter;

import java.net.URI;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;
import static org.springframework.http.HttpHeaders.readOnlyHttpHeaders;

final class DefaultHttp implements Http {

    private static final HttpHeaders EMPTY = readOnlyHttpHeaders(new HttpHeaders());

    private final Executor executor;
    private final ClientHttpRequestFactory requestFactory;
    private final MessageWorker worker;
    private final Supplier<URI> baseUrlProvider;
    private final RequestArguments arguments;
    private final Plugin plugin;

    DefaultHttp(final Executor executor, final ClientHttpRequestFactory requestFactory,
            final List<HttpMessageConverter<?>> converters,
            final Supplier<URI> baseUrlProvider, final UrlResolution resolution, final Plugin plugin) {
        this.executor = requireNonNull(executor, "executor");
        this.requestFactory = requireNonNull(requestFactory, "request factory");
        this.worker = new MessageWorker(converters);
        this.baseUrlProvider = requireNonNull(baseUrlProvider, "base url provider");
        this.arguments = RequestArguments.create().withUrlResolution(resolution);
        this.plugin = plugin;
    }

    @Override
    public final QueryStage get(final String uriTemplate, final Object... urlVariables) {
        return execute(HttpMethod.GET, uriTemplate, urlVariables);
    }

    @Override
    public final QueryStage get(final URI uri) {
        return execute(HttpMethod.GET, uri);
    }

    @Override
    public final QueryStage get() {
        return execute(HttpMethod.GET);
    }

    @Override
    public final QueryStage head(final String uriTemplate, final Object... urlVariables) {
        return execute(HttpMethod.HEAD, uriTemplate, urlVariables);
    }

    @Override
    public final QueryStage head(final URI uri) {
        return execute(HttpMethod.HEAD, uri);
    }

    @Override
    public final QueryStage head() {
        return execute(HttpMethod.HEAD);
    }

    @Override
    public final QueryStage post(final String uriTemplate, final Object... urlVariables) {
        return execute(HttpMethod.POST, uriTemplate, urlVariables);
    }

    @Override
    public final QueryStage post(final URI uri) {
        return execute(HttpMethod.POST, uri);
    }

    @Override
    public final QueryStage post() {
        return execute(HttpMethod.POST);
    }

    @Override
    public final QueryStage put(final String uriTemplate, final Object... urlVariables) {
        return execute(HttpMethod.PUT, uriTemplate, urlVariables);
    }

    @Override
    public final QueryStage put(final URI uri) {
        return execute(HttpMethod.PUT, uri);
    }

    @Override
    public final QueryStage put() {
        return execute(HttpMethod.PUT);
    }

    @Override
    public final QueryStage patch(final String uriTemplate, final Object... urlVariables) {
        return execute(HttpMethod.PATCH, uriTemplate, urlVariables);
    }

    @Override
    public final QueryStage patch(final URI uri) {
        return execute(HttpMethod.PATCH, uri);
    }

    @Override
    public final QueryStage patch() {
        return execute(HttpMethod.PATCH);
    }

    @Override
    public final QueryStage delete(final String uriTemplate, final Object... urlVariables) {
        return execute(HttpMethod.DELETE, uriTemplate, urlVariables);
    }

    @Override
    public final QueryStage delete(final URI uri) {
        return execute(HttpMethod.DELETE, uri);
    }

    @Override
    public final QueryStage delete() {
        return execute(HttpMethod.DELETE);
    }

    @Override
    public final QueryStage options(final String uriTemplate, final Object... urlVariables) {
        return execute(HttpMethod.OPTIONS, uriTemplate, urlVariables);
    }

    @Override
    public final QueryStage options(final URI uri) {
        return execute(HttpMethod.OPTIONS, uri);
    }

    @Override
    public final QueryStage options() {
        return execute(HttpMethod.OPTIONS);
    }

    @Override
    public final QueryStage trace(final String uriTemplate, final Object... urlVariables) {
        return execute(HttpMethod.TRACE, uriTemplate, urlVariables);
    }

    @Override
    public final QueryStage trace(final URI uri) {
        return execute(HttpMethod.TRACE, uri);
    }

    @Override
    public final QueryStage trace() {
        return execute(HttpMethod.TRACE);
    }

    @Override
    public QueryStage execute(final HttpMethod method, final String uriTemplate, final Object... uriVariables) {
        return execute(arguments
                .withMethod(method)
                .withBaseUrl(baseUrlProvider.get())
                .withUriTemplate(uriTemplate)
                .withUriVariables(ImmutableList.copyOf(uriVariables)));
    }

    @Override
    public QueryStage execute(final HttpMethod method, final URI uri) {
        return execute(arguments
                .withMethod(method)
                .withBaseUrl(baseUrlProvider.get())
                .withUri(uri));
    }

    @Override
    public QueryStage execute(final HttpMethod method) {
        return execute(arguments
                .withMethod(method)
                .withBaseUrl(baseUrlProvider.get()));
    }

    private QueryStage execute(final RequestArguments arguments) {
        return new Requester(executor, requestFactory, worker, arguments, plugin, ImmutableMultimap.of(), EMPTY);
    }

}
