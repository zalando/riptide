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

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.AsyncClientHttpRequest;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.client.RestClientException;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.List;

import static java.lang.String.format;

final class DefaultMessageWriter implements MessageWriter {

    private final List<HttpMessageConverter<?>> converters;

    DefaultMessageWriter(final List<HttpMessageConverter<?>> converters) {
        this.converters = converters;
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
