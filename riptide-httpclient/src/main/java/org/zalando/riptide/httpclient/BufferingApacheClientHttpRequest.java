package org.zalando.riptide.httpclient;

import lombok.AllArgsConstructor;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.classic.methods.HttpUriRequest;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.io.entity.ByteArrayEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpResponse;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.Optional;

@AllArgsConstructor
final class BufferingApacheClientHttpRequest implements ClientHttpRequest {

    private final HttpHeaders headers = new HttpHeaders();
    private final ByteArrayOutputStream output = new ByteArrayOutputStream(1024);

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
    public Map<String, Object> getAttributes() {
        throw new UnsupportedOperationException();
    }

    @Nonnull
    @Override
    public HttpHeaders getHeaders() {
        return headers;
    }

    @Nonnull
    @Override
    public OutputStream getBody() {
        return output;
    }

    @Override
    @Nonnull
    public ClientHttpResponse execute() throws IOException {
        Headers.writeHeaders(headers, request);
        request.setEntity(new ByteArrayEntity(output.toByteArray(), toContentType(headers.getContentType())));

        final HttpResponse response = client.executeOpen(null, request, null);
        return new ApacheClientHttpResponse(response);
    }

    @Nullable
    private ContentType toContentType(@Nullable MediaType mediaType) {
        return Optional.ofNullable(mediaType)
                .map(ContentTypeConverter::toContentType)
                .orElse(null);
    }

}
