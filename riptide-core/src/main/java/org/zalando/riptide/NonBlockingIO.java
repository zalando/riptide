package org.zalando.riptide;

import lombok.*;
import org.springframework.http.*;
import org.springframework.http.client.*;
import org.springframework.util.concurrent.*;

import java.io.*;
import java.net.*;
import java.util.concurrent.*;

import static org.springframework.util.CollectionUtils.*;

@AllArgsConstructor
final class NonBlockingIO implements IO {

    private final AsyncClientHttpRequestFactory requestFactory;

    @Override
    public CompletableFuture<ClientHttpResponse> execute(final RequestArguments arguments) throws IOException {
        final URI uri = arguments.getRequestUri();
        final HttpMethod method = arguments.getMethod();

        final AsyncClientHttpRequest request = requestFactory.createAsyncRequest(uri, method);

        request.getHeaders().addAll(toMultiValueMap(arguments.getHeaders()));
        arguments.getEntity().writeTo(request);

        return toCompletable(request.executeAsync());
    }

    private <T> CompletableFuture<T> toCompletable(final ListenableFuture<T> original) {
        final CompletableFuture<T> future = new CompletableFuture<>();
        original.addCallback(future::complete, future::completeExceptionally);
        return future;
    }

}
