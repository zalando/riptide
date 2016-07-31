package org.zalando.riptide.httpclient;

/*
 * ⁣​
 * Riptide: HTTP Client
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

import org.springframework.core.task.AsyncListenableTaskExecutor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.AsyncClientHttpRequest;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.concurrent.ListenableFuture;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;

final class RestAsyncClientHttpRequest implements AsyncClientHttpRequest {

    private final ClientHttpRequest request;
    private final AsyncListenableTaskExecutor executor;

    RestAsyncClientHttpRequest(final ClientHttpRequest request, final AsyncListenableTaskExecutor executor) {
        this.request = request;
        this.executor = executor;
    }

    @Override
    public ListenableFuture<ClientHttpResponse> executeAsync() throws IOException {
        return executor.submitListenable(request::execute);
    }

    @Override
    public OutputStream getBody() throws IOException {
        return request.getBody();
    }

    @Override
    public HttpMethod getMethod() {
        return request.getMethod();
    }

    @Override
    public URI getURI() {
        return request.getURI();
    }

    @Override
    public HttpHeaders getHeaders() {
        return request.getHeaders();
    }

}
