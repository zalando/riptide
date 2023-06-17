package org.zalando.riptide.httpclient;

import lombok.AllArgsConstructor;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.classic.methods.HttpUriRequest;
import org.apache.hc.core5.function.Supplier;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.HttpResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.StreamingHttpOutputMessage;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpResponse;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static org.springframework.http.HttpHeaders.CONTENT_ENCODING;

@AllArgsConstructor
final class StreamingApacheClientHttpRequest implements ClientHttpRequest, StreamingHttpOutputMessage {

    private final HttpHeaders headers = new HttpHeaders();

    private final HttpClient client;
    private final HttpUriRequest request;

    @Override
    @Nonnull
    public HttpMethod getMethod() {
        return HttpMethod.valueOf(request.getMethod());
    }

    @Nonnull
    @Override
    public URI getURI() {
        try {
            return request.getUri();
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(e);
        }
    }

    @Nonnull
    @Override
    public HttpHeaders getHeaders() {
        return headers;
    }

    @Nonnull
    @Override
    public OutputStream getBody() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setBody(final Body body) {
        try (StreamingHttpEntity streamingHttpEntity = new StreamingHttpEntity(body)) {
            request.setEntity(streamingHttpEntity);
        }
    }

    @Override
    @Nonnull
    public ClientHttpResponse execute() throws IOException {
        Headers.writeHeaders(headers, request);
        final HttpResponse response = client.execute(request);
        return new ApacheClientHttpResponse(response);
    }

    @AllArgsConstructor
    private class StreamingHttpEntity implements HttpEntity {

        private final Body body;

        @Override
        public boolean isRepeatable() {
            return false;
        }

        @Override
        public boolean isChunked() {
            return false;
        }

        @Override
        public Set<String> getTrailerNames() {
            return null;
        }

        @Override
        public long getContentLength() {
            return headers.getContentLength();
        }

        @Override
        @Nullable
        public String getContentType() {
            return Objects.toString(headers.getContentType());
        }

        @Override
        @Nullable
        public String getContentEncoding() {
            return headers.getFirst(CONTENT_ENCODING);
        }

        @Override
        public InputStream getContent() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void writeTo(final OutputStream stream) throws IOException {
            body.writeTo(stream);
        }

        @Override
        public boolean isStreaming() {
            return false;
        }

        @Override
        public Supplier<List<? extends Header>> getTrailers() {
            return null;
        }

        @Override
        public void close() {

        }

    }

}
