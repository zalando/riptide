package org.zalando.riptide.httpclient;

import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.Configurable;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.methods.HttpUriRequest;
import org.apiguardian.api.API;
import org.springframework.core.task.AsyncListenableTaskExecutor;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.AsyncClientHttpRequest;
import org.springframework.http.client.AsyncClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;

import java.io.IOException;
import java.net.URI;

import static org.apiguardian.api.API.Status.STABLE;

@API(status = STABLE)
public final class ApacheClientHttpRequestFactory implements ClientHttpRequestFactory {

    private final ClientHttpRequestFactory factory;

    public ApacheClientHttpRequestFactory(final HttpClient client) {
        final RequestConfig config = Configurable.class.cast(client).getConfig();

        this.factory = new HttpComponentsClientHttpRequestFactory(client) {
            @Override
            protected void postProcessHttpRequest(final HttpUriRequest request) {
                // restore the client's request settings that are incorrectly handled by spring
                HttpRequestBase.class.cast(request).setConfig(config);
            }
        };
    }

    @Override
    public ClientHttpRequest createRequest(final URI uri, final HttpMethod method) throws IOException {
        return new ApacheClientHttpRequest(factory.createRequest(uri, method));
    }

}
