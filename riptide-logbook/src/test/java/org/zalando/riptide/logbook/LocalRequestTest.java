package org.zalando.riptide.logbook;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpOutputMessage;
import org.zalando.riptide.RequestArguments;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

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
