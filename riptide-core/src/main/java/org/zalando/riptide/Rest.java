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

import com.google.common.collect.Multimap;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.AsyncClientHttpRequest;
import org.springframework.http.client.AsyncClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.SettableListenableFuture;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import javax.annotation.Nullable;
import java.io.IOException;
import java.net.URI;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.springframework.web.util.UriComponentsBuilder.fromUri;
import static org.springframework.web.util.UriComponentsBuilder.fromUriString;

public final class Rest {

    private final AsyncClientHttpRequestFactory requestFactory;
    private final MessageReader reader;
    private final MessageWriter writer;
    private final String baseUrl;

    Rest(final AsyncClientHttpRequestFactory requestFactory, final List<HttpMessageConverter<?>> converters,
            @Nullable final String baseUrl) {
        this.requestFactory = checkNotNull(requestFactory, "request factory");
        this.baseUrl = baseUrl;
        checkNotNull(converters, "converters");
        this.reader = new DefaultMessageReader(converters);
        this.writer = new DefaultMessageWriter(converters);
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
        return new ListenableFutureRequester(method, fromUriString(uriTemplate), uriVariables);
    }

    private Requester execute(final HttpMethod method, final URI url) {
        return new ListenableFutureRequester(method, fromUri(url));
    }

    private class ListenableFutureRequester extends Requester {

        private final HttpMethod method;
        private final UriComponentsBuilder urlBuilder;
        private final Object[] urlVariables;

        public ListenableFutureRequester(final HttpMethod method, final UriComponentsBuilder urlBuilder,
                final Object... urlVariables) {
            this.method = method;
            this.urlBuilder = urlBuilder;
            this.urlVariables = urlVariables;
        }

        @Override
        protected <T> Dispatcher execute(final Multimap<String, String> query, final HttpHeaders headers,
                @Nullable final T body) throws IOException {

            final HttpEntity<T> entity = new HttpEntity<>(body, headers);
            final AsyncClientHttpRequest request = createRequest(query, entity);
            final ListenableFuture<ClientHttpResponse> future = request.executeAsync();

            return new Dispatcher() {

                @Override
                public <A> ListenableFuture<Void> dispatch(final RoutingTree<A> tree) {
                    final SettableListenableFuture<Void> capture = new SettableListenableFuture<Void>() {
                        @Override
                        protected void interruptTask() {
                            future.cancel(true);
                        }
                    };

                    future.addCallback(response -> {
                        try {
                            tree.execute(response, reader);
                            capture.set(null);
                        } catch (final Exception e) {
                            capture.setException(e);
                        }
                    }, capture::setException);

                    return capture;
                }

            };
        }

        private <T> AsyncClientHttpRequest createRequest(final Multimap<String, String> query,
                final HttpEntity<T> entity) throws IOException {

            final URI url = createUrl(query);
            final AsyncClientHttpRequest request = requestFactory.createAsyncRequest(url, method);
            writer.write(request, entity);
            return request;
        }

        private URI createUrl(final Multimap<String, String> query) {
            query.entries().forEach(entry ->
                    urlBuilder.queryParam(entry.getKey(), entry.getValue()));

            final UriComponents components = urlBuilder.build().expand(urlVariables).encode();

            if (baseUrl == null || components.getHost() != null) {
                return components.toUri();
            }

            return URI.create(baseUrl + components.toUriString());
        }

    }

    public static RestBuilder builder() {
        return new RestBuilder();
    }

}
