package org.zalando.riptide.compatibility;

import org.junit.jupiter.api.*;
import org.springframework.http.*;
import org.springframework.http.client.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class HttpOutputMessageAsyncClientHttpRequestAdapterTest {

    private final HttpOutputMessage message = mock(HttpOutputMessage.class);
    private final AsyncClientHttpRequest unit = new HttpOutputMessageAsyncClientHttpRequestAdapter(message);

    @Test
    void executeAsync() {
        assertThrows(UnsupportedOperationException.class, unit::executeAsync);
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
