package org.zalando.riptide.compatibility;

import lombok.*;
import lombok.experimental.Delegate;
import org.springframework.http.*;
import org.springframework.http.client.*;
import org.springframework.util.concurrent.*;

import javax.annotation.*;
import java.net.*;

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

    @Nonnull
    @Override
    public URI getURI() {
        throw new UnsupportedOperationException();
    }

}
