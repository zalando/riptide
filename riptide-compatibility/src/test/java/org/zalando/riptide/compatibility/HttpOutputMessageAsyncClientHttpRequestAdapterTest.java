package org.zalando.riptide.compatibility;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpOutputMessage;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

class HttpOutputMessageAsyncClientHttpRequestAdapterTest {

    private final HttpOutputMessage message = mock(HttpOutputMessage.class);

    // need concrete type here to see both getMethod and getMethodValue
    private final HttpOutputMessageAsyncClientHttpRequestAdapter unit =
            new HttpOutputMessageAsyncClientHttpRequestAdapter(message);

    @Test
    void executeAsync() {
        assertThrows(UnsupportedOperationException.class, unit::executeAsync);
    }

    @Test
    void getMethodValue() {
        assertThrows(UnsupportedOperationException.class, unit::getMethodValue);
    }

    @Test
    void getMethod() {
        assertThrows(UnsupportedOperationException.class, unit::getMethod);
    }

    @Test
    void getURI() {
        assertThrows(UnsupportedOperationException.class, unit::getURI);
    }

}
