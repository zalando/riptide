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
import org.springframework.http.client.AsyncClientHttpRequest;
import org.springframework.http.client.AsyncClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.util.concurrent.FailureCallback;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;
import org.springframework.util.concurrent.SettableListenableFuture;
import org.springframework.util.concurrent.SuccessCallback;
import org.springframework.web.client.AsyncRestTemplate;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.DefaultUriTemplateHandler;
import org.springframework.web.util.UriTemplateHandler;

import javax.annotation.Nullable;
import java.io.IOException;
import java.net.URI;
import java.util.List;

import static java.lang.String.format;

public final class Rest {

    private final AsyncClientHttpRequestFactory requestFactory;
    private final List<HttpMessageConverter<?>> converters;
    private final UriTemplateHandler uriTemplateHandler;
    private final MessageReader reader;

    private Rest(final AsyncClientHttpRequestFactory requestFactory, final List<HttpMessageConverter<?>> converters, final UriTemplateHandler uriTemplateHandler) {
        this.requestFactory = requestFactory;
        this.converters = converters;
        this.uriTemplateHandler = uriTemplateHandler;
        this.reader = new DefaultMessageReader(converters);
    }

    public final Requester get(final String urlTemplate, final Object... urlVariables) {
        return execute(HttpMethod.GET, urlTemplate, urlVariables);
    }

    public final Requester get(final URI url) {
        return execute(HttpMethod.GET, url);
    }

    public final Requester head(final String urlTemplate, final Object... urlVariables) {
        return execute(HttpMethod.HEAD, urlTemplate, urlVariables);
    }

    public final Requester head(final URI url) {
        return execute(HttpMethod.HEAD, url);
    }

    public final Requester post(final String urlTemplate, final Object... urlVariables) {
        return execute(HttpMethod.POST, urlTemplate, urlVariables);
    }

    public final Requester post(final URI url) {
        return execute(HttpMethod.POST, url);
    }

    public final Requester put(final String urlTemplate, final Object... urlVariables) {
        return execute(HttpMethod.PUT, urlTemplate, urlVariables);
    }

    public final Requester put(final URI url) {
        return execute(HttpMethod.PUT, url);
    }

    public final Requester patch(final String urlTemplate, final Object... urlVariables) {
        return execute(HttpMethod.PATCH, urlTemplate, urlVariables);
    }

    public final Requester patch(final URI url) {
        return execute(HttpMethod.PATCH, url);
    }

    public final Requester delete(final String urlTemplate, final Object... urlVariables) {
        return execute(HttpMethod.DELETE, urlTemplate, urlVariables);
    }

    public final Requester delete(final URI url) {
        return execute(HttpMethod.DELETE, url);
    }

    public final Requester options(final String urlTemplate, final Object... urlVariables) {
        return execute(HttpMethod.OPTIONS, urlTemplate, urlVariables);
    }

    public final Requester options(final URI url) {
        return execute(HttpMethod.OPTIONS, url);
    }

    public final Requester trace(final String urlTemplate, final Object... urlVariables) {
        return execute(HttpMethod.TRACE, urlTemplate, urlVariables);
    }

    public final Requester trace(final URI url) {
        return execute(HttpMethod.TRACE, url);
    }

    private Requester execute(final HttpMethod method, final String uriTemplate,
            final Object... uriVariables) {
        return execute(method, uriTemplateHandler.expand(uriTemplate, uriVariables));
    }

    private Requester execute(final HttpMethod method, final URI uri) {
        return new ListenableFutureRequester(uri, method);
    }

    private class ListenableFutureRequester extends Requester {

        private final URI url;
        private final HttpMethod method;

        public ListenableFutureRequester(final URI url, final HttpMethod method) {
            this.url = url;
            this.method = method;
        }

        @Override
        protected <T> Dispatcher execute(final HttpHeaders headers, @Nullable final T body) throws IOException {
            final HttpEntity<T> entity = new HttpEntity<>(body, headers);
            final AsyncClientHttpRequest request = createRequest(entity);
            final ListenableFuture<ClientHttpResponse> future = request.executeAsync();

            return new Dispatcher() {
                @Override
                public <A> ListenableFuture<Capture> dispatch(final RoutingTree<A> tree) {
                    final SettableListenableFuture<Capture> capture = new SettableListenableFuture<>();
                    final FailureCallback failure = capture::setException;
                    final SuccessCallback<ClientHttpResponse> success = response -> {
                        try {
                            capture.set(tree.execute(response, reader));
                        } catch (final Throwable e) {
                            failure.onFailure(e);
                        }
                    };

                    future.addCallback(success, failure);
                    return capture;
                }
            };
        }

        private <T> AsyncClientHttpRequest createRequest(final HttpEntity<T> entity)
                throws IOException {

            final AsyncClientHttpRequest request = requestFactory.createAsyncRequest(url, method);

            final HttpHeaders headers = entity.getHeaders();
            request.getHeaders().putAll(headers);

            @Nullable final T body = entity.getBody();

            if (body == null) {
                return request;
            }

            final Class<?> type = body.getClass();
            @Nullable final MediaType contentType = headers.getContentType();

            converters.stream()
                    .filter(c -> c.canWrite(type, contentType))
                    .map(this::<T>cast)
                    .findFirst()
                    .orElseThrow(() -> {
                        final String message = format(
                                "Could not write request: no suitable HttpMessageConverter found for request type [%s]",
                                type.getName());

                        if (contentType == null) {
                            return new RestClientException(message);
                        } else {
                            return new RestClientException(format("%s and content type [%s]", message, contentType));
                        }
                    })
                    .write(body, contentType, request);

            return request;
        }

        @SuppressWarnings("unchecked")
        private <T> HttpMessageConverter<T> cast(final HttpMessageConverter<?> converter) {
            return (HttpMessageConverter<T>) converter;
        }

    }

    @Deprecated
    public static Rest create(final AsyncRestTemplate template) {
        final AsyncClientHttpRequestFactory factory = template.getAsyncRequestFactory();
        final List<HttpMessageConverter<?>> converters = template.getMessageConverters();
        final UriTemplateHandler uriTemplateHandler = template.getUriTemplateHandler();
        return create(factory, converters, uriTemplateHandler);
    }

    public static Rest create(final AsyncClientHttpRequestFactory factory,
            final List<HttpMessageConverter<?>> converters) {
        return create(factory, converters, new DefaultUriTemplateHandler());
    }


    public static Rest create(final AsyncClientHttpRequestFactory factory,
            final List<HttpMessageConverter<?>> converters, final UriTemplateHandler uriTemplateHandler) {
        return new Rest(factory, converters, uriTemplateHandler);
    }

    public static <T> ListenableFutureCallback<T> handle(final FailureCallback callback) {
        return new ListenableFutureCallback<T>() {
            @Override
            public void onSuccess(final T result) {
                // ignored
            }

            @Override
            public void onFailure(final Throwable ex) {
                callback.onFailure(ex);
            }
        };
    }

}
