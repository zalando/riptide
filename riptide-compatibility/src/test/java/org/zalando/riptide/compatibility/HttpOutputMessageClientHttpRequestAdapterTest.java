package org.zalando.riptide.compatibility;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpOutputMessage;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

class HttpOutputMessageClientHttpRequestAdapterTest {

    private final HttpOutputMessage message = mock(HttpOutputMessage.class);

    // need concrete type here to see both getMethod and getMethodValue
    private final HttpOutputMessageClientHttpRequestAdapter unit =
            new HttpOutputMessageClientHttpRequestAdapter(message);

    @Test
    void execute() {
        assertThrows(UnsupportedOperationException.class, unit::execute);
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
