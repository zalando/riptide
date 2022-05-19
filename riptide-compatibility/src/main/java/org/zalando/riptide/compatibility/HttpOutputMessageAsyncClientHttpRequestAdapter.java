package org.zalando.riptide.compatibility;

import lombok.AllArgsConstructor;
import lombok.experimental.Delegate;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.client.AsyncClientHttpRequest;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.concurrent.ListenableFuture;

import javax.annotation.Nonnull;
import java.net.URI;

@AllArgsConstructor
final class HttpOutputMessageAsyncClientHttpRequestAdapter implements AsyncClientHttpRequest {

    @Delegate
    private final HttpOutputMessage message;

    @Nonnull
    @Override
    public ListenableFuture<ClientHttpResponse> executeAsync() {
        throw new UnsupportedOperationException();
    }

    @Nonnull
    @Override
    public String getMethodValue() {
        throw new UnsupportedOperationException();
    }

    @Override
    public HttpMethod getMethod() {
        throw new UnsupportedOperationException();
    }

    @Nonnull
    @Override
    public URI getURI() {
        throw new UnsupportedOperationException();
    }

}
