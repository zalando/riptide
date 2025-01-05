package org.zalando.riptide.compatibility;

import lombok.AllArgsConstructor;
import lombok.experimental.Delegate;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpResponse;

import javax.annotation.Nonnull;
import java.net.URI;
import java.util.Map;

@AllArgsConstructor
final class HttpOutputMessageClientHttpRequestAdapter implements ClientHttpRequest {

    @Delegate
    private final HttpOutputMessage message;

    @Nonnull
    @Override
    public ClientHttpResponse execute() {
        throw new UnsupportedOperationException();
    }

    @Nonnull
    @Override
    public HttpMethod getMethod() {
        throw new UnsupportedOperationException();
    }

    @Nonnull
    @Override
    public URI getURI() {
        throw new UnsupportedOperationException();
    }

    @Nonnull
    @Override
    public Map<String, Object> getAttributes() {
        throw new UnsupportedOperationException();
    }
}
