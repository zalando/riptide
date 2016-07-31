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

import org.apache.http.client.HttpClient;
import org.springframework.core.task.AsyncListenableTaskExecutor;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.AsyncClientHttpRequest;
import org.springframework.http.client.AsyncClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;

import java.io.IOException;
import java.net.URI;

public class RestAsyncClientHttpRequestFactory implements AsyncClientHttpRequestFactory {

    private final ClientHttpRequestFactory factory;
    private final AsyncListenableTaskExecutor executor;

    public RestAsyncClientHttpRequestFactory(final HttpClient client, final AsyncListenableTaskExecutor executor) {
        this.factory = new HttpComponentsClientHttpRequestFactory(client);
        this.executor = executor;
    }

    @Override
    public AsyncClientHttpRequest createAsyncRequest(final URI uri, final HttpMethod method) throws IOException {
        return new RestAsyncClientHttpRequest(factory.createRequest(uri, method), executor);
    }

}
