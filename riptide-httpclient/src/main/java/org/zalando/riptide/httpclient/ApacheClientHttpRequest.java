package org.zalando.riptide.httpclient;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.StreamingHttpOutputMessage;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpResponse;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;

final class ApacheClientHttpRequest implements ClientHttpRequest, StreamingHttpOutputMessage {

    private final ClientHttpRequest request;

    ApacheClientHttpRequest(final ClientHttpRequest request) {
        this.request = request;
    }

    @Override
    public ClientHttpResponse execute() throws IOException {
        final ClientHttpResponse execute = request.execute();
        return new ApacheClientHttpResponse(execute);
    }

    @Nonnull
    @Override
    public OutputStream getBody() {
        throw new UnsupportedOperationException();
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

    @Override
    public void setBody(final Body body) {
        ((StreamingHttpOutputMessage) request).setBody(body);
    }

}
