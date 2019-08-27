package org.zalando.riptide;

import com.google.common.collect.*;
import org.springframework.http.*;
import org.springframework.http.client.*;

import java.io.*;

abstract class ForwardingClientHttpResponse extends ForwardingObject implements ClientHttpResponse {

    @Override
    protected abstract ClientHttpResponse delegate();

    @Override
    public void close() {
        delegate().close();
    }

    @Override
    public int getRawStatusCode() throws IOException {
        return delegate().getRawStatusCode();
    }

    @Override
    public HttpStatus getStatusCode() throws IOException {
        return delegate().getStatusCode();
    }

    @Override
    public String getStatusText() throws IOException {
        return delegate().getStatusText();
    }

    @Override
    public InputStream getBody() throws IOException {
        return delegate().getBody();
    }

    @Override
    public HttpHeaders getHeaders() {
        return delegate().getHeaders();
    }

}
