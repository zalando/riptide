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
import org.springframework.http.client.AsyncClientHttpRequest;
import org.springframework.http.client.AsyncClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.util.concurrent.FailureCallback;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureAdapter;
import org.springframework.util.concurrent.ListenableFutureCallback;
import org.springframework.web.client.AsyncRestTemplate;
import org.springframework.web.util.UriTemplateHandler;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.util.List;
import java.util.concurrent.ExecutionException;

public final class AsyncRest extends RestBase<AsyncDispatcher> {

    private final AsyncClientHttpRequestFactory asyncClientHttpRequestFactory;
    private final List<HttpMessageConverter<?>> converters;
    private final UriTemplateHandler uriTemplateHandler;

    private AsyncRest(AsyncClientHttpRequestFactory asyncClientHttpRequestFactory, List<HttpMessageConverter<?>> converters, UriTemplateHandler uriTemplateHandler) {
        this.asyncClientHttpRequestFactory = asyncClientHttpRequestFactory;
        this.converters = converters;
        this.uriTemplateHandler = uriTemplateHandler;
    }

    @Override
    protected UriTemplateHandler getUriTemplateHandler() {
        return uriTemplateHandler;
    }

    public AsyncDispatcher execute(final HttpMethod method, final URI url, final HttpHeaders headers) {
        return execute(method, url, new HttpEntity<>(headers));
    }

    public AsyncDispatcher execute(final HttpMethod method, final URI url, final Object body) {
        return execute(method, url, new HttpEntity<>(body));
    }

    public AsyncDispatcher execute(final HttpMethod method, final URI url, final HttpHeaders headers, final Object body) {
        return execute(method, url, new HttpEntity<>(body, headers));
    }

    protected <T> AsyncDispatcher execute(final HttpMethod method, final URI url, final HttpEntity<T> entity) {
        try {
            final AsyncClientHttpRequest request = asyncClientHttpRequestFactory.createAsyncRequest(url, method);
            RequestUtil.writeRequestEntity(entity, new AsyncClientHttpRequestAdapter(request), converters);
            final ListenableFuture<ClientHttpResponse> responseFuture = request.executeAsync();
            final ExceptionWrappingFuture future = new ExceptionWrappingFuture(responseFuture);

            return new AsyncDispatcher(converters, future);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    static class ExceptionWrappingFuture extends ListenableFutureAdapter<ClientHttpResponse, ClientHttpResponse> {

        public ExceptionWrappingFuture(final ListenableFuture<ClientHttpResponse> clientHttpResponseFuture) {
            super(clientHttpResponseFuture);
        }

        @Override
        protected final ClientHttpResponse adapt(ClientHttpResponse response) throws ExecutionException {
            try {
                return response;
            } catch (Throwable ex) {
                throw new ExecutionException(ex);
            }
        }
    }


    public static AsyncRest create(final AsyncRestTemplate template) {
        return new AsyncRest(template.getAsyncRequestFactory(), template.getMessageConverters(), template.getUriTemplateHandler());
    }

    public static AsyncRest create(final AsyncClientHttpRequestFactory asyncClientHttpRequestFactory, final List<HttpMessageConverter<?>> converters, UriTemplateHandler uriTemplateHandler) {
        return new AsyncRest(asyncClientHttpRequestFactory, converters, uriTemplateHandler);
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
