package org.zalando.riptide.httpclient;

import org.apache.hc.client5.http.classic.methods.HttpUriRequest;
import org.junit.jupiter.api.Test;

import java.net.URISyntaxException;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BufferingApacheClientHttpRequestTest {

    @Test
    void shouldThrowIllegalArgumentException() throws URISyntaxException {
        final HttpUriRequest httpUriRequest = mock(HttpUriRequest.class);
        when(httpUriRequest.getUri()).thenThrow(URISyntaxException.class);

        final BufferingApacheClientHttpRequest request = new BufferingApacheClientHttpRequest(null, httpUriRequest);
        assertThrows(IllegalArgumentException.class, request::getURI);
    }

}