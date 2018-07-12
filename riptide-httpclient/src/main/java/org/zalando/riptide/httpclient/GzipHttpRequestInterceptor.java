package org.zalando.riptide.httpclient;

import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.client.entity.GzipCompressingEntity;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;
import org.apiguardian.api.API;

import java.util.Optional;

import static org.apiguardian.api.API.Status.EXPERIMENTAL;

@API(status = EXPERIMENTAL)
public final class GzipHttpRequestInterceptor implements HttpRequestInterceptor {

    @Override
    public void process(final HttpRequest original, final HttpContext context) {
        detectCompressibility(original).ifPresent(request -> {
            final HttpEntity compressed = compress(request.getEntity());
            request.setEntity(compressed);
            updateHeaders(request, compressed);
        });
    }

    private Optional<HttpEntityEnclosingRequest> detectCompressibility(final HttpRequest request) {
        return Optional.of(request)
                .filter(HttpEntityEnclosingRequest.class::isInstance)
                .map(HttpEntityEnclosingRequest.class::cast)
                .filter(this::hasNonEmptyBody);
    }

    private boolean hasNonEmptyBody(final HttpEntityEnclosingRequest request) {
        return Optional.ofNullable(request.getEntity())
                .filter(entity -> entity.getContentLength() > 0)
                .isPresent();
    }

    private HttpEntity compress(final HttpEntity entity) {
        return new GzipCompressingEntity(entity);
    }

    void updateHeaders(final HttpRequest request, final HttpEntity entity) {
        request.removeHeaders(HTTP.CONTENT_LEN);

        request.removeHeaders(HTTP.CONTENT_ENCODING);
        request.addHeader(entity.getContentEncoding());

        request.removeHeaders(HTTP.TRANSFER_ENCODING);
        request.setHeader(HTTP.TRANSFER_ENCODING, HTTP.CHUNK_CODING);
    }

}
