package org.zalando.riptide;

import com.google.common.collect.ImmutableList;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.AsyncClientHttpRequestFactory;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.client.AsyncRestTemplate;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.List;
import java.util.function.Supplier;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Central class for actual asynchronous HTTP-based communication. Rest is loosely modeled after the HTTP protocol,
 * i.e. you start with a method and by a URL and optionally followed query parameters, headers and a body:
 *
 * <pre>{@code http.get("/users")
 *     .queryParam("active", "true")
 *     .accept(APPLICATION_JSON)
 *     .dispatch(..)}</pre>
 *
 * @see RestTemplate
 * @see AsyncRestTemplate
 */
// TODO rename to Http?
public final class Rest {

    private final AsyncClientHttpRequestFactory requestFactory;
    private final MessageWorker worker;
    private final Supplier<URI> baseUrlProvider;
    private final RequestArguments arguments;
    private final Plugin plugin;

    Rest(final AsyncClientHttpRequestFactory requestFactory, final List<HttpMessageConverter<?>> converters,
            final Supplier<URI> baseUrlProvider, final UrlResolution resolution, final Plugin plugin) {
        this.requestFactory = checkNotNull(requestFactory, "request factory");
        this.worker = new MessageWorker(converters);
        this.baseUrlProvider = checkNotNull(baseUrlProvider, "base url provider");
        this.arguments = RequestArguments.create().withUrlResolution(resolution);
        this.plugin = plugin;
    }

    public final Requester get(final String uriTemplate, final Object... urlVariables) {
        return execute(HttpMethod.GET, uriTemplate, urlVariables);
    }

    public final Requester get(final URI uri) {
        return execute(HttpMethod.GET, uri);
    }

    public final Requester get() {
        return execute(HttpMethod.GET);
    }

    public final Requester head(final String uriTemplate, final Object... urlVariables) {
        return execute(HttpMethod.HEAD, uriTemplate, urlVariables);
    }

    public final Requester head(final URI uri) {
        return execute(HttpMethod.HEAD, uri);
    }

    public final Requester head() {
        return execute(HttpMethod.HEAD);
    }

    public final Requester post(final String uriTemplate, final Object... urlVariables) {
        return execute(HttpMethod.POST, uriTemplate, urlVariables);
    }

    public final Requester post(final URI uri) {
        return execute(HttpMethod.POST, uri);
    }

    public final Requester post() {
        return execute(HttpMethod.POST);
    }

    public final Requester put(final String uriTemplate, final Object... urlVariables) {
        return execute(HttpMethod.PUT, uriTemplate, urlVariables);
    }

    public final Requester put(final URI uri) {
        return execute(HttpMethod.PUT, uri);
    }

    public final Requester put() {
        return execute(HttpMethod.PUT);
    }

    public final Requester patch(final String uriTemplate, final Object... urlVariables) {
        return execute(HttpMethod.PATCH, uriTemplate, urlVariables);
    }

    public final Requester patch(final URI uri) {
        return execute(HttpMethod.PATCH, uri);
    }

    public final Requester patch() {
        return execute(HttpMethod.PATCH);
    }

    public final Requester delete(final String uriTemplate, final Object... urlVariables) {
        return execute(HttpMethod.DELETE, uriTemplate, urlVariables);
    }

    public final Requester delete(final URI uri) {
        return execute(HttpMethod.DELETE, uri);
    }

    public final Requester delete() {
        return execute(HttpMethod.DELETE);
    }

    public final Requester options(final String uriTemplate, final Object... urlVariables) {
        return execute(HttpMethod.OPTIONS, uriTemplate, urlVariables);
    }

    public final Requester options(final URI uri) {
        return execute(HttpMethod.OPTIONS, uri);
    }

    public final Requester options() {
        return execute(HttpMethod.OPTIONS);
    }

    public final Requester trace(final String uriTemplate, final Object... urlVariables) {
        return execute(HttpMethod.TRACE, uriTemplate, urlVariables);
    }

    public final Requester trace(final URI uri) {
        return execute(HttpMethod.TRACE, uri);
    }

    public final Requester trace() {
        return execute(HttpMethod.TRACE);
    }

    public Requester execute(final HttpMethod method, final String uriTemplate, final Object... uriVariables) {
        return execute(arguments
                .withMethod(method)
                .withBaseUrl(baseUrlProvider.get())
                .withUriTemplate(uriTemplate)
                .withUriVariables(ImmutableList.copyOf(uriVariables)));
    }

    public Requester execute(final HttpMethod method, final URI uri) {
        return execute(arguments
                .withMethod(method)
                .withBaseUrl(baseUrlProvider.get())
                .withUri(uri));
    }

    public Requester execute(final HttpMethod method) {
        return execute(arguments
                .withMethod(method)
                .withBaseUrl(baseUrlProvider.get()));
    }

    private Requester execute(final RequestArguments arguments) {
        return new Requester(requestFactory, worker, arguments, plugin);
    }

    public static RestBuilder builder() {
        return new RestBuilder();
    }

}
