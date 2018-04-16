package org.zalando.riptide;

import org.junit.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public final class HttpResponseExceptionTest {

    @Test
    @SuppressWarnings("ThrowableNotThrown")
    public void shouldNotFailOnNullBody() throws IOException {
        final ClientHttpResponse response = mock(ClientHttpResponse.class);
        when(response.getHeaders()).thenReturn(new HttpHeaders());

        new HttpResponseException("foo", response) {};
    }

}
