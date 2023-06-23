package org.zalando.riptide.httpclient;

import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.io.entity.InputStreamEntity;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

final class ApacheClientHttpResponseBodyTest {

    @Test
    void shouldCallCloseOnNormalStreams() throws IOException {
        final InputStream stream = mock(InputStream.class);
        final ClassicHttpResponse response = mock(ClassicHttpResponse.class);
        when(response.getEntity()).thenReturn(new InputStreamEntity(stream, null));
        when(response.getHeaders()).thenReturn(new Header[0]);

        new ApacheClientHttpResponse(response).close();

        verify(stream).close();
    }

    @Test
    void shouldReturnEmptyWhenResponseIsNotHttpEntityContainer() throws IOException {
        final HttpResponse httpResponse = mock(HttpResponse.class);
        when(httpResponse.getHeaders()).thenReturn(new Header[0]);

        try (final ApacheClientHttpResponse x = new ApacheClientHttpResponse(httpResponse)) {
            assertThat(x.getBody()).isEqualTo(EmptyInputStream.EMPTY);
        }
    }

}
