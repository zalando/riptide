package org.zalando.riptide.autoconfigure;

import lombok.AllArgsConstructor;
import org.springframework.core.task.AsyncListenableTaskExecutor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.AsyncClientHttpRequest;
import org.springframework.http.client.AsyncClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.lang.Nullable;
import org.springframework.util.concurrent.ListenableFuture;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;

// TODO better name!
// TODO move somewhere else?
@AllArgsConstructor
final class ConcurrentClientHttpRequestFactory implements AsyncClientHttpRequestFactory {

    private final ClientHttpRequestFactory requestFactory;
    private final AsyncListenableTaskExecutor executor;

    @Override
    public AsyncClientHttpRequest createAsyncRequest(final URI uri, final HttpMethod httpMethod) throws IOException {
        return new ConcurrentClientHttpRequest(requestFactory.createRequest(uri, httpMethod));
    }

    @AllArgsConstructor
    private final class ConcurrentClientHttpRequest implements AsyncClientHttpRequest {

        private final ClientHttpRequest request;

        @Override
        @Nullable
        public HttpMethod getMethod() {
            return request.getMethod();
        }

        @Override
        public String getMethodValue() {
            return request.getMethodValue();
        }

        @Override
        public URI getURI() {
            return request.getURI();
        }

        @Override
        public HttpHeaders getHeaders() {
            return request.getHeaders();
        }

        @Override
        public OutputStream getBody() throws IOException {
            return request.getBody();
        }

        @Override
        public ListenableFuture<ClientHttpResponse> executeAsync() {
            return executor.submitListenable(request::execute);
        }

    }

}
