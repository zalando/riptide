package org.zalando.riptide;

import lombok.*;
import org.springframework.http.*;
import org.springframework.http.converter.*;
import org.springframework.web.client.*;

import javax.annotation.*;
import java.io.*;
import java.util.*;

import static java.lang.String.*;

@AllArgsConstructor
final class DefaultMessageWriter implements MessageWriter {

    private final List<HttpMessageConverter<?>> converters;

    @Override
    public void write(final HttpOutputMessage request, final RequestArguments arguments)
            throws IOException {

        @Nullable final Object body = arguments.getBody();

        if (body == null) {
            return;
        }

        final Class<?> type = body.getClass();

        @Nullable final MediaType contentType = request.getHeaders().getContentType();

        converters.stream()
                .filter(converter -> converter.canWrite(type, contentType))
                .map(this::cast)
                .findFirst()
                .orElseThrow(() -> fail(type, contentType))
                .write(body, contentType, request);
    }

    @SuppressWarnings("unchecked") // guarded by HttpMessageConverter#canWrite
    private <T> HttpMessageConverter<T> cast(final HttpMessageConverter<?> converter) {
        return (HttpMessageConverter<T>) converter;
    }

    private RestClientException fail(final Class<?> type, @Nullable final MediaType contentType) {
        final String message = format(
                "Could not write request: no suitable HttpMessageConverter found for request type [%s]",
                type.getName());
    
        if (contentType == null) {
            return new RestClientException(message);
        } else {
            return new RestClientException(format("%s and content type [%s]", message, contentType));
        }
    }

}
