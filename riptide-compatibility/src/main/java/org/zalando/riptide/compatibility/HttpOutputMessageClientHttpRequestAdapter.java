package org.zalando.riptide.compatibility;

import lombok.AllArgsConstructor;
import lombok.experimental.Delegate;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpResponse;

import javax.annotation.Nonnull;
import java.net.URI;

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
    // TODO(Spring 5): @Override
    public String getMethodValue() {
        throw new UnsupportedOperationException();
    }

    // TODO(Spring 4): @Override
    public HttpMethod getMethod() {
        throw new UnsupportedOperationException();
    }

    @Nonnull
    @Override
    public URI getURI() {
        throw new UnsupportedOperationException();
    }

}
