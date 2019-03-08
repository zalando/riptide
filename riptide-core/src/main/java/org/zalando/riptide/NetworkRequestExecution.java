package org.zalando.riptide;

import lombok.AllArgsConstructor;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.CompletableFuture;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.springframework.util.CollectionUtils.toMultiValueMap;

@AllArgsConstructor
final class NetworkRequestExecution implements RequestExecution {

    private final ClientHttpRequestFactory requestFactory;

    @Override
    public CompletableFuture<ClientHttpResponse> execute(final RequestArguments arguments) throws IOException {
        final URI requestUri = arguments.getRequestUri();
        final HttpMethod method = arguments.getMethod();

        final ClientHttpRequest request = requestFactory.createRequest(requestUri, method);

        request.getHeaders().addAll(toMultiValueMap(arguments.getHeaders()));
        arguments.getEntity().writeTo(request);

        return completedFuture(request.execute());
    }

}
