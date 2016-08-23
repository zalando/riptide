package org.zalando.riptide;

/*
 * ⁣​
 * Riptide Core
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

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.client.AsyncClientHttpRequest;
import org.springframework.http.client.AsyncClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.web.util.UriComponentsBuilder;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.util.concurrent.CompletableFuture;

public final class Requester extends Dispatcher {

    private final AsyncClientHttpRequestFactory requestFactory;
    private final MessageWorker worker;
    private final HttpMethod method;
    private final UriComponentsBuilder urlBuilder;
    private final Object[] urlVariables;

    private final Multimap<String, String> query = LinkedHashMultimap.create();
    private final HttpHeaders headers = new HttpHeaders();

    public Requester(final AsyncClientHttpRequestFactory requestFactory, final MessageWorker worker,
            final HttpMethod method, final UriComponentsBuilder urlBuilder, final Object... urlVariables) {
        this.requestFactory = requestFactory;
        this.worker = worker;
        this.method = method;
        this.urlBuilder = urlBuilder;
        this.urlVariables = urlVariables;
    }

    public final Requester queryParam(final String name, final String value) {
        query.put(name, value);
        return this;
    }

    public final Requester queryParams(final Multimap<String, String> params) {
        query.putAll(params);
        return this;
    }

    public final Requester header(final String name, final String value) {
        headers.add(name, value);
        return this;
    }

    public final Requester headers(final HttpHeaders headers) {
        this.headers.putAll(headers);
        return this;
    }

    public final Requester accept(final MediaType acceptableMediaType, final MediaType... acceptableMediaTypes) {
        headers.setAccept(Lists.asList(acceptableMediaType, acceptableMediaTypes));
        return this;
    }

    public final Requester contentType(final MediaType contentType) {
        headers.setContentType(contentType);
        return this;
    }

    public final <T> Dispatcher body(final T body) {
        return execute(query, headers, body);
    }

    @Override
    public final <A> CompletableFuture<Void> dispatch(final RoutingTree<A> tree) {
        return execute(query, headers, null).dispatch(tree);
    }

    protected <T> Dispatcher execute(final Multimap<String, String> query, final HttpHeaders headers,
            final @Nullable T body) {

        final HttpEntity<T> entity = new HttpEntity<>(body, headers);
        final ListenableFuture<ClientHttpResponse> future = createAndExecute(query, entity);

        return new Dispatcher() {

            @Override
            public <A> CompletableFuture<Void> dispatch(final RoutingTree<A> tree) {
                final CompletableFuture<Void> capture = new CompletableFuture<Void>() {
                    @Override
                    public boolean cancel(final boolean mayInterruptIfRunning) {
                        final boolean cancelled = future.cancel(mayInterruptIfRunning);
                        super.cancel(mayInterruptIfRunning);
                        return cancelled;
                    }
                };

                future.addCallback(response -> {
                    try {
                        tree.execute(response, worker);
                        capture.complete(null);
                    } catch (final Exception e) {
                        capture.completeExceptionally(e);
                    }
                }, capture::completeExceptionally);

                return capture;
            }

        };
    }

    private <T> ListenableFuture<ClientHttpResponse> createAndExecute(final Multimap<String, String> query,
            final HttpEntity<T> entity) {
        try {
            final AsyncClientHttpRequest request = create(query, entity);
            return request.executeAsync();
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private <T> AsyncClientHttpRequest create(final Multimap<String, String> query,
            final HttpEntity<T> entity) throws IOException {

        final URI url = createUrl(query);
        final AsyncClientHttpRequest request = requestFactory.createAsyncRequest(url, method);
        worker.write(request, entity);
        return request;
    }

    private URI createUrl(final Multimap<String, String> query) {
        query.entries().forEach(entry ->
                urlBuilder.queryParam(entry.getKey(), entry.getValue()));
        return urlBuilder.buildAndExpand(urlVariables).encode().toUri().normalize();
    }

}
