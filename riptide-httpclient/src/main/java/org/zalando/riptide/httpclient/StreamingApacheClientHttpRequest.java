package org.zalando.riptide.httpclient;

import lombok.AllArgsConstructor;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.message.BasicHeader;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.StreamingHttpOutputMessage;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpResponse;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.Objects;
import java.util.Optional;

import static org.springframework.http.HttpHeaders.CONTENT_ENCODING;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;

@AllArgsConstructor
final class StreamingApacheClientHttpRequest implements ClientHttpRequest, StreamingHttpOutputMessage {

    private final HttpHeaders headers = new HttpHeaders();

    private final HttpClient client;
    private final HttpUriRequest request;

    @Nonnull
    @SuppressWarnings("WeakerAccess")
    // TODO(Spring 5) @Override
    public String getMethodValue() {
        return request.getMethod();
    }

    // TODO(Spring 4) @Override
    public HttpMethod getMethod() {
        return HttpMethod.valueOf(getMethodValue());
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
        throw new UnsupportedOperationException();
    }

    @Override
    public void setBody(final Body body) {
        if (request instanceof HttpEntityEnclosingRequest) {
            final HttpEntityEnclosingRequest enclosing = (HttpEntityEnclosingRequest) request;
            enclosing.setEntity(new StreamingHttpEntity(body));
        } else {
            throw new IllegalStateException(getMethodValue() + " doesn't support a body");
        }
    }

    @Override
    public ClientHttpResponse execute() throws IOException {
        Headers.writeHeaders(headers, request);
        final HttpResponse response = client.execute(request);
        return new ApacheClientHttpResponse(response);
    }

    @AllArgsConstructor
    private class StreamingHttpEntity implements HttpEntity {

        private final Body body;

        @Override
        public boolean isRepeatable() {
            return false;
        }

        @Override
        public boolean isChunked() {
            return false;
        }

        @Override
        public long getContentLength() {
            return headers.getContentLength();
        }

        @Override
        @Nullable
        public Header getContentType() {
            return Optional.ofNullable(headers.getContentType())
                    .map(Objects::toString)
                    .map(type -> new BasicHeader(CONTENT_TYPE, type))
                    .orElse(null);
        }

        @Override
        @Nullable
        public Header getContentEncoding() {
            return Optional.ofNullable(headers.getFirst(CONTENT_ENCODING))
                    .map(encoding -> new BasicHeader(CONTENT_ENCODING, encoding))
                    .orElse(null);
        }

        @Override
        public InputStream getContent() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void writeTo(final OutputStream stream) throws IOException {
            body.writeTo(stream);
        }

        @Override
        public boolean isStreaming() {
            return false;
        }

        @Override
        @Deprecated
        public void consumeContent() {
            throw new UnsupportedOperationException();
        }

    }

}
