package org.zalando.riptide;

import lombok.AllArgsConstructor;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.http.client.reactive.ClientHttpRequest;
import org.springframework.util.concurrent.ListenableFuture;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

@AllArgsConstructor
final class NonBlockingIO implements IO {

    private final ClientHttpConnector requestFactory;

    @Override
    public CompletableFuture<ClientHttpResponse> execute(final RequestArguments arguments) throws IOException {
        final URI uri = arguments.getRequestUri();
        final HttpMethod method = arguments.getMethod();

//        requestFactory.connect(method, uri, new Function<ClientHttpRequest, reactor.core.publisher.Mono<Void>>() {
//            @Override
//            public reactor.core.publisher.Mono<Void> apply(ClientHttpRequest clientHttpRequest) {
//                return null;
//            }
//        });
//        final AsyncClientHttpRequest request = requestFactory.createAsyncRequest(uri, method);

//        copyTo(arguments.getHeaders(), request.getHeaders());
//        arguments.getEntity().writeTo(request);

//        return toCompletable(request.executeAsync());
        return CompletableFuture.failedFuture(new IllegalStateException("Not implemented"));
    }

    private <T> CompletableFuture<T> toCompletable(final ListenableFuture<T> original) {
        final CompletableFuture<T> future = new CompletableFuture<>();
        original.addCallback(future::complete, future::completeExceptionally);
        return future;
    }

}
