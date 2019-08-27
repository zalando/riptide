package org.zalando.riptide;

import com.google.common.reflect.*;
import lombok.*;
import org.springframework.http.*;
import org.springframework.http.client.*;
import org.springframework.http.converter.*;
import org.springframework.web.client.*;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;

import static com.google.common.base.Throwables.*;

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
            return extractor.extractData(response);
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
