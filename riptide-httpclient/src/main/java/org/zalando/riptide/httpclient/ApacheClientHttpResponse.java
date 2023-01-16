package org.zalando.riptide.httpclient;

import lombok.extern.slf4j.Slf4j;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.HttpEntityContainer;
import org.apache.hc.core5.http.HttpResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.AbstractClientHttpResponse;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;

import static org.zalando.riptide.httpclient.Closing.closeQuietly;
import static org.zalando.riptide.httpclient.EmptyInputStream.EMPTY;

@Slf4j
final class ApacheClientHttpResponse extends AbstractClientHttpResponse {

    private final HttpHeaders headers = new HttpHeaders();
    private final HttpResponse response;
    private final InputStream body;

    ApacheClientHttpResponse(final HttpResponse response) throws IOException {
        this.response = response;
        this.body = getBody(response);
        for (final Header header : response.getHeaders()) {
            this.headers.add(header.getName(), header.getValue());
        }
    }

    private static InputStream getBody(final HttpResponse response) throws IOException {

        @Nullable HttpEntity entity = null;
        if (response instanceof HttpEntityContainer) {
            entity = ((HttpEntityContainer) response).getEntity();
        }

        if (entity == null) {
            return EMPTY;
        }

        return new EndOfStreamAwareInputStream(entity.getContent(), (body, endOfStreamDetected) -> {
            body.close();
        });
    }

    @Override
    public int getRawStatusCode() {
        return response.getCode();
    }

    @Nonnull
    @Override
    public String getStatusText() {
        return response.getReasonPhrase();
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
        closeQuietly(body);
        closeQuietly(response);
    }

}
