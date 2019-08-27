package org.zalando.riptide.logbook;

import org.junit.jupiter.api.*;
import org.springframework.http.*;
import org.zalando.riptide.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class LocalRequestTest {

    private final LocalRequest unit = new LocalRequest(RequestArguments.create()
        .withEntity(message -> {}));

    @Test
    void writeTo() {
        final HttpOutputMessage message = mock(HttpOutputMessage.class);

        unit.writeTo(message);

        assertThrows(UnsupportedOperationException.class, () -> unit.writeTo(message));
    }

}
