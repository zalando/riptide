package org.zalando.riptide;

/*
 * ⁣​
 * Riptide
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
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriTemplateHandler;

import javax.annotation.Nullable;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.lang.String.format;

abstract class RestBase<D> {

    protected abstract UriTemplateHandler getUriTemplateHandler();

    public RestWithURL<D> withUrl(final String uriTemplate, final Object... uriVariables) {
        return new RestWithURL<>(this, getUriTemplateHandler().expand(uriTemplate, uriVariables));
    }

    public RestWithURL<D> withUrl(final String uriTemplate, final Map<String, ?> uriVariables) {
        return new RestWithURL<>(this, getUriTemplateHandler().expand(uriTemplate, uriVariables));
    }

    public D execute(final HttpMethod method, final String url) {
        return expandAndExecute(method, url, HttpEntity.EMPTY);
    }

    public D execute(final HttpMethod method, final String url, final HttpHeaders headers) {
        return expandAndExecute(method, url, new HttpEntity<>(headers));
    }

    public D execute(final HttpMethod method, final String url, final Object body) {
        return expandAndExecute(method, url, new HttpEntity<>(body));
    }

    public D execute(final HttpMethod method, final String url, final HttpHeaders headers, final Object body) {
        return expandAndExecute(method, url, new HttpEntity<>(body, headers));
    }

    private <T> D expandAndExecute(final HttpMethod method, final String url, final HttpEntity<T> entity) {
        return execute(method, getUriTemplateHandler().expand(url), entity);
    }

    public D execute(final HttpMethod method, final URI url) {
        return execute(method, url, HttpEntity.EMPTY);
    }

    public D execute(final HttpMethod method, final URI url, final HttpHeaders headers) {
        return execute(method, url, new HttpEntity<>(headers));
    }

    public D execute(final HttpMethod method, final URI url, final Object body) {
        return execute(method, url, new HttpEntity<>(body));
    }

    public D execute(final HttpMethod method, final URI url, final HttpHeaders headers, final Object body) {
        return execute(method, url, new HttpEntity<>(body, headers));
    }

    protected abstract <T> D execute(final HttpMethod method, final URI url, final HttpEntity<T> entity);

    static <T> void writeRequestEntity(final HttpEntity<T> entity, final ClientHttpRequest request, List<HttpMessageConverter<?>> converters) throws IOException {
        final HttpHeaders headers = entity.getHeaders();
        request.getHeaders().putAll(headers);

        @Nullable final T body = entity.getBody();

        if (body != null) {

            final Class<?> type = body.getClass();
            @Nullable final MediaType contentType = headers.getContentType();

            final Optional<HttpMessageConverter<T>> match = converters.stream()
                    .filter(c -> c.canWrite(type, contentType))
                    .map(RestBase::<T>cast)
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
    }

    @SuppressWarnings("unchecked")
    private static <T> HttpMessageConverter<T> cast(final HttpMessageConverter<?> converter) {
        return (HttpMessageConverter<T>) converter;
    }

}
