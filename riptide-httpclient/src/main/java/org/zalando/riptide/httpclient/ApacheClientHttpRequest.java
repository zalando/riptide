package org.zalando.riptide.httpclient;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;

final class ApacheClientHttpRequest implements ClientHttpRequest {

    private final ClientHttpRequest request;

    ApacheClientHttpRequest(final ClientHttpRequest request) {
        this.request = request;
    }

    @Override
    public ClientHttpResponse execute() throws IOException {
        return new ApacheClientHttpResponse(request.execute());
    }

    @Override
    public OutputStream getBody() throws IOException {
        return request.getBody();
    }

    @Override
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

}
