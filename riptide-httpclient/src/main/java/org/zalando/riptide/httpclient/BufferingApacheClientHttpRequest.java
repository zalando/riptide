package org.zalando.riptide.httpclient;

import lombok.*;
import org.apache.http.*;
import org.apache.http.client.*;
import org.apache.http.client.methods.*;
import org.apache.http.entity.*;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.*;

import javax.annotation.*;
import java.io.*;
import java.net.*;

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
