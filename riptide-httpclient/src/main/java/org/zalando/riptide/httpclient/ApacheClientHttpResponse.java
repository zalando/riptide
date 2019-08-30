package org.zalando.riptide.httpclient;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.conn.ConnectionReleaseTrigger;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.AbstractClientHttpResponse;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

import static org.zalando.fauxpas.FauxPas.throwingConsumer;
import static org.zalando.fauxpas.FauxPas.throwingRunnable;
import static org.zalando.riptide.httpclient.EmptyInputStream.EMPTY;

final class ApacheClientHttpResponse extends AbstractClientHttpResponse {

    private final HttpHeaders headers = new HttpHeaders();
    private final HttpResponse response;
    private final InputStream body;

    ApacheClientHttpResponse(final HttpResponse response) throws IOException {
        this.response = response;
        this.body = getBody(response);

        for (final Header header : response.getAllHeaders()) {
            this.headers.add(header.getName(), header.getValue());
        }
    }

    private static InputStream getBody(final HttpResponse response) throws IOException {
        @Nullable final HttpEntity entity = response.getEntity();

        if (entity == null) {
            return EMPTY;
        }

        return new EndOfStreamAwareInputStream(entity.getContent(), (body, endOfStreamDetected) -> {
            if (body instanceof ConnectionReleaseTrigger) {
                // effectively releasing the connection back to the pool in order to prevent starvation
                final ConnectionReleaseTrigger trigger = (ConnectionReleaseTrigger) body;

                if (endOfStreamDetected) {
                    // Stream was fully consumed, connection can therefore be reused.
                    trigger.releaseConnection();
                } else {
                    // Stream was not fully consumed, connection needs to be discarded.
                    // We can't just consume the remaining bytes since the stream could be endless.
                    trigger.abortConnection();
                }
            }
            body.close();
        });
    }

    @Override
    public int getRawStatusCode() {
        return response.getStatusLine().getStatusCode();
    }

    @Nonnull
    @Override
    public String getStatusText() {
        return response.getStatusLine().getReasonPhrase();
    }

    @Nonnull
    @Override
    public InputStream getBody() {
        return body;
    }

    @Nonnull
    @Override
    public HttpHeaders getHeaders() {
        return headers;
    }

    @Override
    public void close() {
        throwingRunnable(body::close).run();
        Optional.of(response)
                .filter(Closeable.class::isInstance)
                .map(Closeable.class::cast)
                .ifPresent(throwingConsumer(Closeable::close));
    }

}
