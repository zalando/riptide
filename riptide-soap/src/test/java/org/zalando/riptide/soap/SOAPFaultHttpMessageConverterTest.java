package org.zalando.riptide.soap;

import org.junit.jupiter.api.Test;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.mock.http.MockHttpOutputMessage;

import static org.junit.jupiter.api.Assertions.assertThrows;

final class SOAPFaultHttpMessageConverterTest {

    private final HttpMessageConverter<Object> unit = new SOAPFaultHttpMessageConverter();

    @Test
    void write() {
        assertThrows(UnsupportedOperationException.class, () ->
                unit.write(new Object(), null, new MockHttpOutputMessage()));
    }

}
