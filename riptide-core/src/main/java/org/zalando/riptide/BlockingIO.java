package org.zalando.riptide;

import lombok.*;
import org.springframework.http.*;
import org.springframework.http.client.*;

import java.io.*;
import java.net.*;
import java.util.concurrent.*;

import static java.util.concurrent.CompletableFuture.*;
import static org.springframework.util.CollectionUtils.*;

@AllArgsConstructor
final class BlockingIO implements IO {

    private final ClientHttpRequestFactory requestFactory;

    @Override
    public CompletableFuture<ClientHttpResponse> execute(final RequestArguments arguments) throws IOException {
        final URI uri = arguments.getRequestUri();
        final HttpMethod method = arguments.getMethod();

        final ClientHttpRequest request = requestFactory.createRequest(uri, method);

        request.getHeaders().addAll(toMultiValueMap(arguments.getHeaders()));
        arguments.getEntity().writeTo(request);

        return completedFuture(request.execute());
    }

}
