package org.zalando.riptide.httpclient;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.conn.EofSensorInputStream;
import org.apache.http.entity.InputStreamEntity;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

final class ApacheClientHttpResponseBodyTest {

    @Test
    void shouldCallCloseOnNormalStreams() throws IOException {
        final InputStream stream = mock(InputStream.class);
        final HttpResponse response = mock(HttpResponse.class);
        when(response.getEntity()).thenReturn(new InputStreamEntity(stream));
        when(response.getAllHeaders()).thenReturn(new Header[0]);

        new ApacheClientHttpResponse(response).close();

        verify(stream).close();
    }

    @Test
    void shouldCallAbortAndCloseOnConnectionReleaseTrigger() throws IOException {
        final EofSensorInputStream stream = mock(EofSensorInputStream.class);
        final HttpResponse response = mock(HttpResponse.class);
        when(response.getEntity()).thenReturn(new InputStreamEntity(stream));
        when(response.getAllHeaders()).thenReturn(new Header[0]);

        new ApacheClientHttpResponse(response).close();

        verify(stream).abortConnection();
        verify(stream).close();
    }
}
