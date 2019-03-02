package org.zalando.riptide;

import lombok.AllArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.client.RestClientException;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.List;

import static java.lang.String.format;

@AllArgsConstructor
final class DefaultMessageWriter implements MessageWriter {

    private final List<HttpMessageConverter<?>> converters;

    @Override
    public void write(final ClientHttpRequest request, final RequestArguments arguments)
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
