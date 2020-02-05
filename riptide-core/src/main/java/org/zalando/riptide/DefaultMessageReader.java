package org.zalando.riptide;

import com.google.common.reflect.TypeToken;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.client.HttpMessageConverterExtractor;
import org.springframework.web.client.ResponseExtractor;
import org.springframework.web.client.RestClientException;

import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;

import static com.google.common.base.Throwables.propagateIfPossible;

@AllArgsConstructor
final class DefaultMessageReader implements MessageReader {

    private final List<HttpMessageConverter<?>> converters;

    @Override
    public <I> I read(final TypeToken<I> type, final ClientHttpResponse response) throws IOException {
        if (type.isSubtypeOf(ResponseEntity.class)) {
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
        try {
            try {
                return extractor.extractData(response);
            } catch (final IOException | RuntimeException e) {
                response.close();
                throw e;
            }
        } catch (final RestClientException e) {
            propagateIfPossible(e.getCause(), IOException.class, HttpMessageNotReadableException.class);
            throw e;
        }
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

}
