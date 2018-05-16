package org.zalando.riptide.httpclient;

import org.junit.Test;
import org.springframework.core.task.AsyncListenableTaskExecutor;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpRequest;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class RestAsyncClientHttpRequestTest {

    @Test
    public void getMethodValue() {
        final ClientHttpRequest request = mock(ClientHttpRequest.class);
        final AsyncListenableTaskExecutor executor = mock(AsyncListenableTaskExecutor.class);
        final RestAsyncClientHttpRequest unit = new RestAsyncClientHttpRequest(request, executor);

        when(request.getMethod()).thenReturn(HttpMethod.POST);

        assertThat(unit.getMethod(), is(HttpMethod.POST));
        assertThat(unit.getMethodValue(), is("POST"));
    }
}
