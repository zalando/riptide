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

import lombok.SneakyThrows;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriTemplateHandler;
import org.springframework.web.util.UriTemplateHandler;

import javax.annotation.Nullable;
import java.io.IOException;
import java.net.URI;
import java.util.List;

public final class Rest extends RestClient<Capture> {

    private final ClientHttpRequestFactory clientHttpRequestFactory;
    private final List<HttpMessageConverter<?>> converters;
    private final UriTemplateHandler uriTemplateHandler;

    private Rest(final ClientHttpRequestFactory clientHttpRequestFactory,
            final List<HttpMessageConverter<?>> converters, final UriTemplateHandler uriTemplateHandler) {
        this.clientHttpRequestFactory = clientHttpRequestFactory;
        this.converters = converters;
        this.uriTemplateHandler = uriTemplateHandler;
    }

    @Override
    protected Requester<Capture> execute(final HttpMethod method, final String uriTemplate, final Object... uriVariables) {
        return execute(method, uriTemplateHandler.expand(uriTemplate, uriVariables));
    }

    @Override
    protected Requester<Capture> execute(final HttpMethod method, final URI url) {
        return new Requester<Capture>() {

            @Override
            protected <T> Dispatcher<Capture> execute(final HttpHeaders headers, @Nullable final T body) {
                // TODO only once
                final MessageReader reader = new DefaultMessageReader(converters);

                try {
                    final HttpEntity<T> entity = new HttpEntity<>(body, headers);
                    final ClientHttpRequest request = clientHttpRequestFactory.createRequest(url, method);
                    writeRequestEntity(entity, request, converters);
                    final ClientHttpResponse response = request.execute();


                    return new Dispatcher<Capture>() {
                        @Override
                        @SneakyThrows
                        public <A> Capture dispatch(final RoutingTree<A> tree) {
                            return tree.execute(response, reader);
                        }
                    };
                } catch (final IOException ex) {
                    final String message = String.format("I/O error on %s request for \"%s\": %s", method.name(), url, ex.getMessage());
                    throw new ResourceAccessException(message, ex);
                }
            }

        };
    }

    public static Rest create(final ClientHttpRequestFactory clientHttpRequestFactory, final List<HttpMessageConverter<?>> converters, final UriTemplateHandler uriTemplateHandler) {
        return new Rest(clientHttpRequestFactory, converters, uriTemplateHandler);
    }

    public static Rest create(final ClientHttpRequestFactory clientHttpRequestFactory, final List<HttpMessageConverter<?>> converters) {
        return create(clientHttpRequestFactory, converters, new DefaultUriTemplateHandler());
    }

    @Deprecated
    public static Rest create(final RestTemplate template) {
        return create(template.getRequestFactory(), template.getMessageConverters(), template.getUriTemplateHandler());
    }

}
