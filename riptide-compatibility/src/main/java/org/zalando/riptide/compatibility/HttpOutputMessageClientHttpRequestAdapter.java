package org.zalando.riptide.compatibility;

import lombok.*;
import lombok.experimental.Delegate;
import org.springframework.http.*;
import org.springframework.http.client.*;

import javax.annotation.*;
import java.net.*;

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
    public String getMethodValue() {
        throw new UnsupportedOperationException();
    }

    @Nonnull
    @Override
    public URI getURI() {
        throw new UnsupportedOperationException();
    }

}
