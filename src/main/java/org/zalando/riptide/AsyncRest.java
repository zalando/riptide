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

import com.google.gag.annotation.remark.OhNoYouDidnt;
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
import org.springframework.web.client.AsyncRestTemplate;
import org.springframework.web.client.ResourceAccessException;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.concurrent.ExecutionException;

public final class AsyncRest {

    private final AsyncClientHttpRequestFactory asyncClientHttpRequestFactory;
    private final List<HttpMessageConverter<?>> converters;
    private final Router router = new Router();

    private AsyncRest(AsyncClientHttpRequestFactory asyncClientHttpRequestFactory, List<HttpMessageConverter<?>> converters) {
        this.asyncClientHttpRequestFactory = asyncClientHttpRequestFactory;
        this.converters = converters;
    }

    public AsyncDispatcher execute(final HttpMethod method, final URI url) {
        return execute(method, url, HttpEntity.EMPTY);
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

    private <T> AsyncDispatcher execute(final HttpMethod method, final URI url, final HttpEntity<T> entity) {
        try {
            final AsyncClientHttpRequest request = asyncClientHttpRequestFactory.createAsyncRequest(url, method);
            RequestUtil.writeRequestEntity(entity, new AsyncClientHttpRequestAdapter(request), converters);
            final ListenableFuture<ClientHttpResponse> responseFuture = request.executeAsync();
            final ExceptionWrappingFuture future = new ExceptionWrappingFuture(responseFuture);

            return new AsyncDispatcher(converters, future, router);
        } catch (IOException ex) {
            final String message = String.format("I/O error on %s request for \"%s\":%s", method.name(), url, ex.getMessage());
            throw new ResourceAccessException(message, ex);
        }
    }

    static class ExceptionWrappingFuture extends ListenableFutureAdapter<ClientHttpResponse, ClientHttpResponse> {

        public ExceptionWrappingFuture(final ListenableFuture<ClientHttpResponse> clientHttpResponseFuture) {
            super(clientHttpResponseFuture);
        }

        @Override
        protected final ClientHttpResponse adapt(ClientHttpResponse response) throws ExecutionException {
            return response;
        }
    }


    public static AsyncRest create(final AsyncRestTemplate template) {
        return create(template.getAsyncRequestFactory(), template.getMessageConverters());
    }

    public static AsyncRest create(final AsyncClientHttpRequestFactory asyncClientHttpRequestFactory, final List<HttpMessageConverter<?>> converters) {
        return new AsyncRest(asyncClientHttpRequestFactory, converters);
    }

    // syntactic sugar
    @OhNoYouDidnt
    public static FailureCallback handle(final FailureCallback callback) {
        return callback;
    }

}
