package org.zalando.riptide;

import lombok.AllArgsConstructor;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.util.concurrent.CompletableFuture;

import static java.util.concurrent.CompletableFuture.completedFuture;

@AllArgsConstructor
final class NetworkRequestExecution implements RequestExecution {

    private final ClientHttpRequestFactory requestFactory;

    @Override
    public CompletableFuture<ClientHttpResponse> execute(final RequestArguments arguments) throws IOException {
        final URI requestUri = arguments.getRequestUri();
        final HttpMethod method = arguments.getMethod();

        final ClientHttpRequest request = requestFactory.createRequest(requestUri, method);

        arguments.getHeaders().forEach((name, values) ->
                values.forEach(value -> request.getHeaders().add(name, value)));

        final OutputStream stream = request.getBody();
        stream.write(arguments.getEntity());
        stream.flush();

        return completedFuture(request.execute());
    }

}
