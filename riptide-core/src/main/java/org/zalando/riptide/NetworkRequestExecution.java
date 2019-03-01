package org.zalando.riptide;

import lombok.AllArgsConstructor;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpResponse;

import java.net.URI;
import java.util.concurrent.CompletableFuture;

import static java.util.concurrent.CompletableFuture.completedFuture;

@AllArgsConstructor
final class NetworkRequestExecution implements RequestExecution {

    private final ClientHttpRequestFactory requestFactory;
    private final MessageWriter writer;

    @Override
    public CompletableFuture<ClientHttpResponse> execute(final RequestArguments arguments) {
        try {
            final URI requestUri = arguments.getRequestUri();
            final HttpMethod method = arguments.getMethod();

            final ClientHttpRequest request = requestFactory.createRequest(requestUri, method);

            arguments.getHeaders().forEach((name, values) ->
                    values.forEach(value -> request.getHeaders().add(name, value)));

            writer.write(request, arguments);

            return completedFuture(request.execute());
        } catch (final Exception e) {
            final CompletableFuture<ClientHttpResponse> future = new CompletableFuture<>();
            future.completeExceptionally(e);
            return future;
        }
    }

}
