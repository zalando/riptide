package org.zalando.riptide.httpclient;

import org.apache.http.conn.EofSensorInputStream;
import org.junit.Test;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;
import java.io.InputStream;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class RestAsyncClientHttpResponseBodyTest {

    @Test
    public void shouldCallCloseOnNormalStreams() throws IOException {
        InputStream stream = mock(InputStream.class);
        ClientHttpResponse response = mock(ClientHttpResponse.class);
        when(response.getBody()).thenReturn(stream);

        try (RestAsyncClientHttpResponse unit = new RestAsyncClientHttpResponse(response)) {
            unit.getBody().close();
        }

        verify(stream).close();
    }

    @Test
    public void shouldCallAbortAndCloseOnConnectionReleaseTrigger() throws IOException {
        EofSensorInputStream stream = mock(EofSensorInputStream.class);
        ClientHttpResponse response = mock(ClientHttpResponse.class);
        when(response.getBody()).thenReturn(stream);

        try (RestAsyncClientHttpResponse unit = new RestAsyncClientHttpResponse(response)) {
            unit.getBody().close();
        }

        verify(stream).abortConnection();
        verify(stream).close();
    }
}
