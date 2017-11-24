package org.zalando.riptide.spring.testing;

import org.springframework.http.HttpMethod;
import org.springframework.http.client.AsyncClientHttpRequest;
import org.springframework.http.client.AsyncClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.mock.http.client.MockAsyncClientHttpRequest;
import org.springframework.test.web.client.RequestExpectationManager;

import java.io.IOException;
import java.net.URI;

public class ExpectingHttpRequestFactory implements AsyncClientHttpRequestFactory, ClientHttpRequestFactory {

    private final RequestExpectationManager expectationManager;

    ExpectingHttpRequestFactory(final RequestExpectationManager expectationManager) {
        this.expectationManager = expectationManager;
    }

    @Override
    public AsyncClientHttpRequest createAsyncRequest(final URI uri, final HttpMethod httpMethod) throws IOException {
        return createRequestInternal(uri, httpMethod);
    }

    @Override
    public ClientHttpRequest createRequest(final URI uri, final HttpMethod httpMethod) throws IOException {
        return createRequestInternal(uri, httpMethod);
    }


    private MockAsyncClientHttpRequest createRequestInternal(final URI uri, final HttpMethod httpMethod) throws IOException {
        return new MockAsyncClientHttpRequest(httpMethod, uri) {

            @Override
            protected ClientHttpResponse executeInternal() throws IOException {
                ClientHttpResponse response = expectationManager.validateRequest(this);
                setResponse(response);
                return response;
            }
        };
    }

}
