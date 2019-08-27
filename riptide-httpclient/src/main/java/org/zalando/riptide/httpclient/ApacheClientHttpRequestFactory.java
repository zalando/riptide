package org.zalando.riptide.httpclient;

import com.google.common.collect.*;
import lombok.*;
import org.apache.http.client.*;
import org.apache.http.client.methods.*;
import org.apiguardian.api.*;
import org.springframework.beans.factory.*;
import org.springframework.http.*;
import org.springframework.http.client.*;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.function.*;

import static org.apiguardian.api.API.Status.*;
import static org.springframework.http.HttpMethod.*;

@API(status = STABLE)
@AllArgsConstructor
public final class ApacheClientHttpRequestFactory implements ClientHttpRequestFactory, DisposableBean {

    public enum Mode {
        STREAMING, BUFFERING
    }

    private final Map<HttpMethod, Function<URI, HttpUriRequest>> methods = ImmutableMap.<HttpMethod, Function<URI, HttpUriRequest>>builder()
            .put(GET, HttpGet::new)
            .put(HEAD, HttpHead::new)
            .put(POST, HttpPost::new)
            .put(PUT, HttpPut::new)
            .put(PATCH, HttpPatch::new)
            .put(DELETE, HttpDelete::new)
            .put(OPTIONS, HttpOptions::new)
            .put(TRACE, HttpTrace::new)
            .build();

    private final HttpClient client;
    private final Mode mode;

    public ApacheClientHttpRequestFactory(final HttpClient client) {
        this(client, Mode.STREAMING);
    }

    @Override
    public ClientHttpRequest createRequest(final URI uri, final HttpMethod method) {
        final HttpUriRequest request = methods.get(method).apply(uri);

        if (mode == Mode.STREAMING) {
            return new StreamingApacheClientHttpRequest(client, request);
        } else {
            return new BufferingApacheClientHttpRequest(client, request);
        }
    }

    @Override
    public void destroy() throws IOException {
        if (client instanceof Closeable) {
            final Closeable closeable = (Closeable) this.client;
            closeable.close();
        }
    }

}
