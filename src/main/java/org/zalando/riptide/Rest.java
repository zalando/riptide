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
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.client.RequestCallback;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Nullable;
import java.io.IOException;
import java.net.URI;
import java.util.Optional;

import static java.lang.String.format;

public final class Rest {

    private final RestTemplate template;

    private Rest(RestTemplate template) {
        this.template = template;
    }

    Dispatcher execute(HttpMethod method, URI url) {
        return new Dispatcher(template, method, url, request -> {
        });
    }

    // TODO test
    Dispatcher execute(HttpMethod method, URI url, HttpHeaders headers) {
        return new Dispatcher(template, method, url, request -> request.getHeaders().putAll(headers));
    }

    // TODO test
    Dispatcher execute(HttpMethod method, URI url, Object entity) {
        return new Dispatcher(template, method, url, new Callback<>(new HttpEntity<>(entity)));
    }

    // TODO test
    Dispatcher execute(HttpMethod method, URI url, HttpHeaders headers, Object entity) {
        return new Dispatcher(template, method, url, new Callback<>(new HttpEntity<>(entity, headers)));
    }

    // TODO test
    private final class Callback<T> implements RequestCallback {

        private final HttpEntity<T> entity;

        private Callback(HttpEntity<T> entity) {
            this.entity = entity;
        }

        @Override
        public void doWithRequest(ClientHttpRequest request) throws IOException {
            final T body = entity.getBody();
            final Class<?> type = body.getClass();
            final HttpHeaders headers = entity.getHeaders();
            @Nullable final MediaType contentType = headers.getContentType();

            final Optional<HttpMessageConverter<T>> match = template.getMessageConverters().stream()
                    .filter(c -> c.canWrite(type, contentType))
                    .map(this::cast)
                    .findFirst();

            if (match.isPresent()) {
                final HttpMessageConverter<T> converter = match.get();
                request.getHeaders().putAll(headers);

                try {
                    converter.write(body, contentType, request);
                } catch (IOException e) {
                    throw new IllegalStateException(e);
                }
            } else {
                fail(type, contentType);
            }
        }

        @SuppressWarnings("unchecked")
        private HttpMessageConverter<T> cast(HttpMessageConverter<?> converter) {
            return (HttpMessageConverter<T>) converter;
        }

        private RestClientException fail(Class<?> type, @Nullable MediaType contentType) {
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

    public static Rest create(RestTemplate template) {
        return new Rest(template);
    }

}
