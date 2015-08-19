package org.zalando.riptide;

/*
 * ⁣​
 * riptide
 * ⁣⁣
 * Copyright (C) 2015 Zalando SE
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
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.client.RequestCallback;
import org.springframework.web.client.RestClientException;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

import static java.lang.String.format;

final class Callback<T> implements RequestCallback {

    private final List<HttpMessageConverter<?>> converters;
    private final HttpEntity<T> entity;

    Callback(final List<HttpMessageConverter<?>> converters, final HttpEntity<T> entity) {
        this.converters = converters;
        this.entity = entity;
    }

    @Override
    public void doWithRequest(final ClientHttpRequest request) throws IOException {
        final HttpHeaders headers = entity.getHeaders();
        request.getHeaders().putAll(headers);
        
        @Nullable final T body = entity.getBody();
        
        if (body == null) {
            return;
        }
        
        final Class<?> type = body.getClass();
        @Nullable final MediaType contentType = headers.getContentType();

        final Optional<HttpMessageConverter<T>> match = converters.stream()
                .filter(c -> c.canWrite(type, contentType))
                .map(this::cast)
                .findFirst();

        if (match.isPresent()) {
            final HttpMessageConverter<T> converter = match.get();

            converter.write(body, contentType, request);
        } else {
            final String message = format(
                    "Could not write request: no suitable HttpMessageConverter found for request type [%s]",
                    type.getName());

            if (contentType == null) {
                throw new RestClientException(message);
            } else {
                throw new RestClientException(format("%s and content type [%s]", message, contentType));
            }
        }
    }

    @SuppressWarnings("unchecked")
    private HttpMessageConverter<T> cast(final HttpMessageConverter<?> converter) {
        return (HttpMessageConverter<T>) converter;
    }

}
