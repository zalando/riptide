package org.zalando.riptide.soap;

import org.junit.jupiter.api.*;
import org.springframework.http.converter.*;
import org.springframework.mock.http.*;

import static org.junit.jupiter.api.Assertions.*;

final class SOAPFaultHttpMessageConverterTest {

    private final HttpMessageConverter<Object> unit = new SOAPFaultHttpMessageConverter();

    @Test
    void write() {
        assertThrows(UnsupportedOperationException.class, () ->
                unit.write(new Object(), null, new MockHttpOutputMessage()));
    }

}
