package org.zalando.riptide.httpclient;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpRequest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

final class ApacheClientHttpRequestTest {

    @Test
    void getMethodValue() {
        final ClientHttpRequest request = mock(ClientHttpRequest.class);
        final ApacheClientHttpRequest unit = new ApacheClientHttpRequest(request);
        when(request.getMethod()).thenReturn(HttpMethod.POST);
        when(request.getMethodValue()).thenReturn("POST");

        assertThat(unit.getMethod(), is(HttpMethod.POST));
        assertThat(unit.getMethodValue(), is("POST"));
    }

    @Test
    void shouldNotSupportGetBody() {
        final ClientHttpRequest request = mock(ClientHttpRequest.class);
        final ApacheClientHttpRequest unit = new ApacheClientHttpRequest(request);

        assertThrows(UnsupportedOperationException.class, unit::getBody);
    }

}
