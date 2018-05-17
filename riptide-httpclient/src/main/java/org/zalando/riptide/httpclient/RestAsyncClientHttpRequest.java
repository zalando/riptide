package org.zalando.riptide.httpclient;

import org.springframework.core.task.AsyncListenableTaskExecutor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.AsyncClientHttpRequest;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.concurrent.ListenableFuture;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;

final class RestAsyncClientHttpRequest implements AsyncClientHttpRequest {

    private final ClientHttpRequest request;
    private final AsyncListenableTaskExecutor executor;

    RestAsyncClientHttpRequest(final ClientHttpRequest request, final AsyncListenableTaskExecutor executor) {
        this.request = request;
        this.executor = executor;
    }

    @Override
    public ListenableFuture<ClientHttpResponse> executeAsync() throws IOException {
        return executor.submitListenable(this::execute);
    }

    private RestAsyncClientHttpResponse execute() throws IOException {
        return new RestAsyncClientHttpResponse(request.execute());
    }

    @Override
    public OutputStream getBody() throws IOException {
        return request.getBody();
    }

    @Override
    public HttpMethod getMethod() {
        return request.getMethod();
    }

    // TODO @Override as soon as we no longer support Spring 4
    public String getMethodValue() {
        return request.getMethod().toString();
    }

    @Override
    public URI getURI() {
        return request.getURI();
    }

    @Override
    public HttpHeaders getHeaders() {
        return request.getHeaders();
    }

}
