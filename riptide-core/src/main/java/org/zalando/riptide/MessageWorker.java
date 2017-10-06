package org.zalando.riptide;

import com.google.common.reflect.TypeToken;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.AsyncClientHttpRequest;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.client.HttpMessageConverterExtractor;
import org.springframework.web.client.ResponseExtractor;
import org.springframework.web.client.RestClientException;

import javax.annotation.Nullable;
import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;

final class MessageWorker implements MessageReader, MessageWriter {

    private final List<HttpMessageConverter<?>> converters;

    MessageWorker(final List<HttpMessageConverter<?>> converters) {
        this.converters = checkNotNull(converters, "converters");
    }

    @Override
    public <I> I read(final TypeToken<I> type, final ClientHttpResponse response) throws IOException {
        // TODO get rid of those special cases
        if (type.isSubtypeOf(ClientHttpResponse.class)) {
            return cast(response);
        } else if (type.isSubtypeOf(ResponseEntity.class)) {
            final Type bodyType = ParameterizedType.class.cast(type.getType()).getActualTypeArguments()[0];

            final I body = readBody(bodyType, response);
            closeIfNecessary(body, response);
            final HttpHeaders headers = response.getHeaders();
            final HttpStatus statusCode = response.getStatusCode();
            return cast(new ResponseEntity<>(body, headers, statusCode));
        } else {
            final I body = readBody(type.getType(), response);
            closeIfNecessary(body, response);
            return body;
        }
    }

    private <I> I readBody(final Type type, final ClientHttpResponse response) throws IOException {
        final ResponseExtractor<I> extractor = new HttpMessageConverterExtractor<>(type, converters);
        return extractor.extractData(response);
    }

    private <I> void closeIfNecessary(final I body, final ClientHttpResponse response) {
        if (body instanceof AutoCloseable) {
            return;
        }

        response.close();
    }

    @SuppressWarnings("unchecked")
    private <I> I cast(final Object result) {
        return (I) result;
    }

    @Override
    public <T> void write(final AsyncClientHttpRequest request, final HttpEntity<T> entity) throws IOException {
        final HttpHeaders headers = entity.getHeaders();
        request.getHeaders().putAll(headers);
    
        @Nullable final T body = entity.getBody();
    
        if (body == null) {
            return;
        }
    
        final Class<?> type = body.getClass();
        @Nullable final MediaType contentType = headers.getContentType();
    
        converters.stream()
                .filter(converter -> converter.canWrite(type, contentType))
                .map(this::<T>cast)
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
