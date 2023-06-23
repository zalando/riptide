package org.zalando.riptide;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

final class HttpResponseExceptionTest {

    @Test
    @SuppressWarnings("ThrowableNotThrown")
    void shouldNotFailOnNullBody() throws IOException {
        final ClientHttpResponse response = mock(ClientHttpResponse.class);
        when(response.getStatusCode()).thenReturn(HttpStatus.BAD_REQUEST);
        when(response.getHeaders()).thenReturn(new HttpHeaders());

        new HttpResponseException("foo", response) {};
    }

}
