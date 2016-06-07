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
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriTemplateHandler;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.util.List;

public final class Rest extends RestBase<Dispatcher>{

    private final ClientHttpRequestFactory clientHttpRequestFactory;
    private final List<HttpMessageConverter<?>> converters;
    private final UriTemplateHandler uriTemplateHandler;

    private Rest(ClientHttpRequestFactory clientHttpRequestFactory, List<HttpMessageConverter<?>> converters, UriTemplateHandler uriTemplateHandler) {
        this.clientHttpRequestFactory = clientHttpRequestFactory;
        this.converters = converters;
        this.uriTemplateHandler = uriTemplateHandler;
    }

    @Override
    protected UriTemplateHandler getUriTemplateHandler() {
        return uriTemplateHandler;
    }

    public Dispatcher execute(final HttpMethod method, final URI url, final HttpHeaders headers) {
        return execute(method, url, new HttpEntity<>(headers));
    }

    public Dispatcher execute(final HttpMethod method, final URI url, final Object body) {
        return execute(method, url, new HttpEntity<>(body));
    }

    public Dispatcher execute(final HttpMethod method, final URI url, final HttpHeaders headers, final Object body) {
        return execute(method, url, new HttpEntity<>(body, headers));
    }

    protected  <T> Dispatcher execute(final HttpMethod method, final URI url, final HttpEntity<T> entity) {
        final ClientHttpResponse response = executeRequest(method, url, entity);
        return new Dispatcher(converters, response);
    }

    /**
     * Returns the {@link ClientHttpResponse} as reported by the underlying {@link RestTemplate}.
     * <p>
     * Note: When used with a <i>OAuth2RestTemplate</i> this method catches the exception containing the buffered
     * response thrown by the {@link OAuth2CompatibilityResponseErrorHandler} and continues with normal dispatching.
     * </p>
     */
    private <T> ClientHttpResponse executeRequest(final HttpMethod method, final URI url, HttpEntity<T> entity) {
        try {
            final ClientHttpRequest request = clientHttpRequestFactory.createRequest(url, method);
            RequestUtil.writeRequestEntity(entity, request, converters);
            return request.execute();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (final AlreadyConsumedResponseException e) {
            return e.getResponse();
        }
    }

    public static Rest create(final ClientHttpRequestFactory clientHttpRequestFactory, final List<HttpMessageConverter<?>> converters, final UriTemplateHandler uriTemplateHandler) {
        return new Rest(clientHttpRequestFactory, converters, uriTemplateHandler);
    }

    public static Rest create(final RestTemplate template) {
        return new Rest(template.getRequestFactory(), template.getMessageConverters(), template.getUriTemplateHandler());
    }
}
