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

import org.springframework.context.annotation.DependsOn;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.client.RestClientException;

import javax.annotation.Nullable;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Optional;

import static java.lang.String.format;

abstract class RestClient<R> {

    public final Requester<R> get(final String urlTemplate, final Object... urlVariables) {
        return execute(HttpMethod.GET, urlTemplate, urlVariables);
    }

    public final Requester<R> get(final URI url) {
        return execute(HttpMethod.GET, url);
    }

    public final Requester<R> head(final String urlTemplate, final Object... urlVariables) {
        return execute(HttpMethod.HEAD, urlTemplate, urlVariables);
    }

    public final Requester<R> head(final URI url) {
        return execute(HttpMethod.HEAD, url);
    }

    public final Requester<R> post(final String urlTemplate, final Object... urlVariables) {
        return execute(HttpMethod.POST, urlTemplate, urlVariables);
    }

    public final Requester<R> post(final URI url) {
        return execute(HttpMethod.POST, url);
    }

    public final Requester<R> put(final String urlTemplate, final Object... urlVariables) {
        return execute(HttpMethod.PUT, urlTemplate, urlVariables);
    }

    public final Requester<R> put(final URI url) {
        return execute(HttpMethod.PUT, url);
    }

    public final Requester<R> patch(final String urlTemplate, final Object... urlVariables) {
        return execute(HttpMethod.PATCH, urlTemplate, urlVariables);
    }

    public final Requester<R> patch(final URI url) {
        return execute(HttpMethod.PATCH, url);
    }

    public final Requester<R> delete(final String urlTemplate, final Object... urlVariables) {
        return execute(HttpMethod.DELETE, urlTemplate, urlVariables);
    }

    public final Requester<R> delete(final URI url) {
        return execute(HttpMethod.DELETE, url);
    }

    public final Requester<R> options(final String urlTemplate, final Object... urlVariables) {
        return execute(HttpMethod.OPTIONS, urlTemplate, urlVariables);
    }

    public final Requester<R> options(final URI url) {
        return execute(HttpMethod.OPTIONS, url);
    }

    public final Requester<R> trace(final String urlTemplate, final Object... urlVariables) {
        return execute(HttpMethod.TRACE, urlTemplate, urlVariables);
    }

    public final Requester<R> trace(final URI url) {
        return execute(HttpMethod.TRACE, url);
    }

    protected abstract Requester<R> execute(final HttpMethod method, final String urlTemplate,
            final Object... urlVariables);

    protected abstract Requester<R> execute(final HttpMethod method, final URI url);

    // TODO @Deprecated
    static <T> void writeRequestEntity(final HttpEntity<T> entity, final ClientHttpRequest request,
            final List<HttpMessageConverter<?>> converters) throws IOException {
        final HttpHeaders headers = entity.getHeaders();
        request.getHeaders().putAll(headers);

        @Nullable final T body = entity.getBody();

        if (body != null) {

            final Class<?> type = body.getClass();
            @Nullable final MediaType contentType = headers.getContentType();

            final Optional<HttpMessageConverter<T>> match = converters.stream()
                    .filter(c -> c.canWrite(type, contentType))
                    .map(RestClient::<T>cast)
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
