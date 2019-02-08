package org.zalando.riptide.httpclient;

import org.apache.http.conn.EofSensorInputStream;
import org.junit.jupiter.api.Test;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;
import java.io.InputStream;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

final class ApacheClientHttpResponseBodyTest {

    @Test
    void shouldCallCloseOnNormalStreams() throws IOException {
        InputStream stream = mock(InputStream.class);
        ClientHttpResponse response = mock(ClientHttpResponse.class);
        when(response.getBody()).thenReturn(stream);

        new ApacheClientHttpResponse(response).close();

        verify(stream).close();
    }

    @Test
    void shouldCallAbortAndCloseOnConnectionReleaseTrigger() throws IOException {
        EofSensorInputStream stream = mock(EofSensorInputStream.class);
        ClientHttpResponse response = mock(ClientHttpResponse.class);
        when(response.getBody()).thenReturn(stream);

        new ApacheClientHttpResponse(response).close();

        verify(stream).abortConnection();
        verify(stream).close();
    }
}
