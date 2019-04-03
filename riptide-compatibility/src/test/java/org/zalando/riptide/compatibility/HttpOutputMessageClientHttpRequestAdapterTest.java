package org.zalando.riptide.compatibility;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.client.ClientHttpRequest;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

class HttpOutputMessageClientHttpRequestAdapterTest {

    private final HttpOutputMessage message = mock(HttpOutputMessage.class);
    private final ClientHttpRequest unit = new HttpOutputMessageClientHttpRequestAdapter(message);

    @Test
    void execute() {
        assertThrows(UnsupportedOperationException.class, unit::execute);
    }

    @Test
    void getMethodValue() {
        assertThrows(UnsupportedOperationException.class, unit::getMethodValue);
    }

    @Test
    void getURI() {
        assertThrows(UnsupportedOperationException.class, unit::getURI);
    }

}
