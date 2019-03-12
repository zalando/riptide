package org.zalando.riptide.httpclient;

import lombok.AllArgsConstructor;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.ByteArrayEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpResponse;

import javax.annotation.Nonnull;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;

@AllArgsConstructor
final class BufferingApacheClientHttpRequest implements ClientHttpRequest {

    private final HttpHeaders headers = new HttpHeaders();
    private final ByteArrayOutputStream output = new ByteArrayOutputStream(1024);

    private final HttpClient client;
    private final HttpUriRequest request;

    @Nonnull
    @Override
    public String getMethodValue() {
        return request.getMethod();
    }

    @Nonnull
    @Override
    public URI getURI() {
        return request.getURI();
    }

    @Nonnull
    @Override
    public HttpHeaders getHeaders() {
        return headers;
    }

    @Nonnull
    @Override
    public OutputStream getBody() {
        return output;
    }

    @Override
    public ClientHttpResponse execute() throws IOException {
        Headers.writeHeaders(headers, request);

        if (request instanceof HttpEntityEnclosingRequest) {
            final HttpEntityEnclosingRequest enclosing = (HttpEntityEnclosingRequest) request;
            enclosing.setEntity(new ByteArrayEntity(output.toByteArray()));
        }

        final HttpResponse response = client.execute(request);
        return new ApacheClientHttpResponse(response);
    }

}
