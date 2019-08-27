package org.zalando.riptide;

import org.junit.jupiter.api.*;
import org.springframework.http.*;
import org.springframework.http.client.*;

import java.io.*;

import static org.mockito.Mockito.*;

final class HttpResponseExceptionTest {

    @Test
    @SuppressWarnings("ThrowableNotThrown")
    void shouldNotFailOnNullBody() throws IOException {
        final ClientHttpResponse response = mock(ClientHttpResponse.class);
        when(response.getHeaders()).thenReturn(new HttpHeaders());

        new HttpResponseException("foo", response) {};
    }

}
