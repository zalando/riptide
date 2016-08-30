package org.zalando.riptide;

import org.junit.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.ClientHttpResponse;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import static com.google.common.io.ByteStreams.toByteArray;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.OK;

public final class ForwardingClientHttpResponseTest {

    @Test
    public void shouldDelegateAccessors() throws IOException {
        final ClientHttpResponse response = mock(ClientHttpResponse.class);
        when(response.getRawStatusCode()).thenReturn(200);
        when(response.getStatusCode()).thenReturn(OK);
        when(response.getStatusText()).thenReturn("OK");
        when(response.getBody()).thenReturn(new ByteArrayInputStream("Hello World!".getBytes(UTF_8)));
        final HttpHeaders headers = new HttpHeaders();
        when(response.getHeaders()).thenReturn(headers);

        final ForwardingClientHttpResponse unit = new ForwardingClientHttpResponse() {
            @Override
            protected ClientHttpResponse delegate() {
                return response;
            }
        };

        assertThat(unit.getRawStatusCode(), is(200));
        assertThat(unit.getStatusCode(), is(OK));
        assertThat(unit.getStatusText(), is("OK"));
        assertThat(new String(toByteArray(unit.getBody()), UTF_8), is("Hello World!"));
        assertThat(unit.getHeaders(), is(headers));
    }

    @Test
    public void shouldDelegateClose() {
        final ClientHttpResponse response = mock(ClientHttpResponse.class);
        final ForwardingClientHttpResponse unit = new ForwardingClientHttpResponse() {
            @Override
            protected ClientHttpResponse delegate() {
                return response;
            }
        };

        unit.close();

        verify(response).close();
    }

}