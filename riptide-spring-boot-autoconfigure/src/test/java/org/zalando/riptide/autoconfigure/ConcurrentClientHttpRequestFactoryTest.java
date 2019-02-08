package org.zalando.riptide.autoconfigure;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.task.AsyncListenableTaskExecutor;
import org.springframework.http.client.AsyncClientHttpRequest;
import org.springframework.http.client.AsyncClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.mock.http.client.MockClientHttpResponse;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.Callable;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpStatus.OK;

@ExtendWith(MockitoExtension.class)
final class ConcurrentClientHttpRequestFactoryTest {

    @Captor
    private ArgumentCaptor<Callable<ClientHttpResponse>> captor;

    private final ClientHttpRequestFactory delegate = mock(ClientHttpRequestFactory.class);
    private final AsyncListenableTaskExecutor executor = mock(AsyncListenableTaskExecutor.class);
    private final AsyncClientHttpRequestFactory unit = new ConcurrentClientHttpRequestFactory(delegate, executor);

    @Test
    void shouldDelegate() throws IOException {
        final ClientHttpRequest original = new MockClientHttpRequest();
        when(delegate.createRequest(any(), any())).thenReturn(original);

        final AsyncClientHttpRequest request = unit.createAsyncRequest(URI.create("/"), GET);

        assertThat(request.getMethod(), is(sameInstance(original.getMethod())));
        assertThat(request.getMethodValue(), is(sameInstance(original.getMethodValue())));
        assertThat(request.getURI(), is(sameInstance(original.getURI())));
        assertThat(request.getHeaders(), is(sameInstance(original.getHeaders())));
        assertThat(request.getBody(), is(sameInstance(original.getBody())));
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldExecute() throws Exception {
        final MockClientHttpRequest original = new MockClientHttpRequest();
        final MockClientHttpResponse response = new MockClientHttpResponse(new byte[0], OK);

        original.setResponse(response);
        when(delegate.createRequest(any(), any())).thenReturn(original);

        unit.createAsyncRequest(URI.create("/"), GET).executeAsync();

        verify(executor).submitListenable(captor.capture());

        assertThat(captor.getValue().call(), is(sameInstance(response)));
    }

}
