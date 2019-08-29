package org.zalando.riptide;

import lombok.AllArgsConstructor;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.AsyncClientHttpRequest;
import org.springframework.http.client.AsyncClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.concurrent.ListenableFuture;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.CompletableFuture;

@AllArgsConstructor
final class NonBlockingIO implements IO {

    private final AsyncClientHttpRequestFactory requestFactory;

    @Override
    public CompletableFuture<ClientHttpResponse> execute(final RequestArguments arguments) throws IOException {
        final URI uri = arguments.getRequestUri();
        final HttpMethod method = arguments.getMethod();

        final AsyncClientHttpRequest request = requestFactory.createAsyncRequest(uri, method);

        copyTo(arguments.getHeaders(), request.getHeaders());
        arguments.getEntity().writeTo(request);

        return toCompletable(request.executeAsync());
    }

    private <T> CompletableFuture<T> toCompletable(final ListenableFuture<T> original) {
        final CompletableFuture<T> future = new CompletableFuture<>();
        original.addCallback(future::complete, future::completeExceptionally);
        return future;
    }

}
