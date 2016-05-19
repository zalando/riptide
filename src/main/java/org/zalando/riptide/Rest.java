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
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.List;

public final class Rest extends RestBase<RestTemplate, Dispatcher>{

    private Rest(final RestTemplate template) {
        super(template, template::getUriTemplateHandler);
    }

    @Override
    protected <T> Dispatcher execute(final HttpMethod method, final URI url, final HttpEntity<T> entity) {
        final List<HttpMessageConverter<?>> converters = template.getMessageConverters();
        final Callback<T> callback = new Callback<>(converters, entity);
        final ClientHttpResponse response = execute(method, url, callback);
        return new Dispatcher(converters, response, router);
    }

    /**
     * Returns the {@link ClientHttpResponse} as reported by the underlying {@link RestTemplate}.
     * <p>
     * Note: When used with a <i>OAuth2RestTemplate</i> this method catches the exception containing the buffered
     * response thrown by the {@link OAuth2CompatibilityResponseErrorHandler} and continues with normal dispatching.
     * </p>
     */
    private <T> ClientHttpResponse execute(final HttpMethod method, final URI url, final Callback<T> callback) {
        try {
            return template.execute(url, method, callback, BufferingClientHttpResponse::buffer);
        } catch (final AlreadyConsumedResponseException e) {
            return e.getResponse();
        }
    }

    public static Rest create(final RestTemplate template) {
        return new Rest(template);
    }
}
