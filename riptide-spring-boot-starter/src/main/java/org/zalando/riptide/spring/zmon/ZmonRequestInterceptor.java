package org.zalando.riptide.spring.zmon;

import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.RequestLine;
import org.apache.http.client.methods.HttpRequestWrapper;
import org.apache.http.protocol.HttpContext;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.System.currentTimeMillis;
import static java.net.URI.create;

public class ZmonRequestInterceptor implements HttpRequestInterceptor {

    @Override
    public void process(final HttpRequest request, final HttpContext context) throws HttpException, IOException {
        try {
            context.setAttribute(Timing.ATTRIBUTE, assemble(request));
        } catch (final Exception ignored) {
            // ignore
        }
    }

    private Timing assemble(final HttpRequest request) throws URISyntaxException {
        final RequestLine requestLine = request.getRequestLine();
        return new Timing(requestLine.getMethod(), getHost(request), currentTimeMillis());
    }

    private String getHost(final HttpRequest request) throws URISyntaxException {
        final URI uri = getUri(request);
        final int port = uri.getPort();
        return checkNotNull(uri.getHost()) + (port == -1 ? "" : ":" + port);
    }

    private URI getUri(final HttpRequest request) {
        if (request instanceof HttpRequestWrapper) {
            return getUri(((HttpRequestWrapper) request).getOriginal());
        }
        return create(request.getRequestLine().getUri());
    }
}
