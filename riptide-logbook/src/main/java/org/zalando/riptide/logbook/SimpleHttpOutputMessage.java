package org.zalando.riptide.logbook;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpOutputMessage;

import java.io.OutputStream;

@AllArgsConstructor
@Getter
final class SimpleHttpOutputMessage implements HttpOutputMessage {
    private final HttpHeaders headers;
    private final OutputStream body;
}
