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
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.util.concurrent.FailureCallback;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;
import org.springframework.web.client.AsyncRestTemplate;

import java.net.URI;
import java.util.List;

public final class AsyncRest {

    private final AsyncRestTemplate template;
    private final Router router = new Router();

    private AsyncRest(final AsyncRestTemplate template) {
        this.template = template;
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
        final List<HttpMessageConverter<?>> converters = template.getMessageConverters();
        final Callback<T> callback = new Callback<>(converters, entity);

        final ListenableFuture<ClientHttpResponse> future = template.execute(url, method,
                new AsyncRequestCallbackAdapter<>(callback), BufferingClientHttpResponse::buffer);

        return new AsyncDispatcher(converters, future, router);
    }

    public static AsyncRest create(final AsyncRestTemplate template) {
        return new AsyncRest(template);
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
