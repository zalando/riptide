package org.zalando.riptide;

/*
 * ⁣​
 * Riptide
 * ⁣⁣
 * Copyright (C) 2015 - 2016 Zalando SE
 * ⁣⁣
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ​⁣
 */

import com.google.common.reflect.TypeToken;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.client.HttpMessageConverterExtractor;
import org.springframework.web.client.ResponseExtractor;

import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;

final class DefaultMessageReader implements MessageReader {

    private final List<HttpMessageConverter<?>> converters;

    DefaultMessageReader(final List<HttpMessageConverter<?>> converters) {
        this.converters = converters;
    }

    @Override
    public <I> I read(final TypeToken<I> type, final ClientHttpResponse response) throws IOException {
        if (type.isSubtypeOf(ClientHttpResponse.class)) {
            return cast(response);
        } else if (type.isSubtypeOf(ResponseEntity.class)) {
            final Type bodyType = ParameterizedType.class.cast(type.getType()).getActualTypeArguments()[0];

            final I body = readBody(bodyType, response);
            closeIfNecessary(body, response);
            final HttpHeaders headers = response.getHeaders();
            final HttpStatus statusCode = response.getStatusCode();
            return cast(new ResponseEntity<I>(body, headers, statusCode));
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

}
