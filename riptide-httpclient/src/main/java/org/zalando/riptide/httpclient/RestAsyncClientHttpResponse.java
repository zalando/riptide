package org.zalando.riptide.httpclient;

import org.apache.http.conn.ConnectionReleaseTrigger;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpResponse;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

final class RestAsyncClientHttpResponse implements ClientHttpResponse {

    private final ClientHttpResponse response;

    RestAsyncClientHttpResponse(final ClientHttpResponse response) {
        this.response = response;
    }

    @Override
    public HttpStatus getStatusCode() throws IOException {
        return response.getStatusCode();
    }

    @Override
    public int getRawStatusCode() throws IOException {
        return response.getRawStatusCode();
    }

    @Override
    public String getStatusText() throws IOException {
        return response.getStatusText();
    }

    @Override
    public InputStream getBody() throws IOException {
        final InputStream body = response.getBody();
        return new FilterInputStream(body) {
            @Override
            public void close() throws IOException {
                if (body instanceof ConnectionReleaseTrigger) {
                    // effectively releasing the connection back to the pool in order to prevent starvation
                    ConnectionReleaseTrigger.class.cast(body).abortConnection();
                }
                super.close();
            }
        };
    }

    @Override
    public HttpHeaders getHeaders() {
        return response.getHeaders();
    }

    @Override
    public void close() {
        response.close();
    }

}
