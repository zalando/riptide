package org.zalando.riptide.httpclient;

import org.apache.http.*;
import org.apache.http.conn.*;
import org.apache.http.entity.*;
import org.junit.jupiter.api.*;

import java.io.*;

import static org.mockito.Mockito.*;

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
