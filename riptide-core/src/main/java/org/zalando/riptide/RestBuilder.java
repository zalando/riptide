package org.zalando.riptide;

/*
 * ⁣​
 * Riptide: Core
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

import com.google.common.collect.ImmutableList;
import org.springframework.http.client.AsyncClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.scheduling.concurrent.ConcurrentTaskExecutor;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriTemplateHandler;
import org.springframework.web.util.UriTemplateHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class RestBuilder {

    // package private so we can trick code coverage
    static class Converters {
        private static final ImmutableList<HttpMessageConverter<?>> DEFAULT =
                ImmutableList.copyOf(new RestTemplate().getMessageConverters());
    }

    private AsyncClientHttpRequestFactory requestFactory;
    private final List<HttpMessageConverter<?>> converters = new ArrayList<>();
    private UriTemplateHandler uriTemplateHandler;

    RestBuilder() {

    }

    public RestBuilder requestFactory(final AsyncClientHttpRequestFactory requestFactory) {
        this.requestFactory = requestFactory;
        return this;
    }

    public RestBuilder converters(final Iterable<HttpMessageConverter<?>> converters) {
        converters.forEach(this::converter);
        return this;
    }

    public RestBuilder converter(final HttpMessageConverter<?> converter) {
        this.converters.add(converter);
        return this;
    }

    public RestBuilder uriTemplateHandler(final UriTemplateHandler uriTemplateHandler) {
        this.uriTemplateHandler = uriTemplateHandler;
        return this;
    }

    public RestBuilder baseUrl(final String baseUrl) {
        final DefaultUriTemplateHandler handler = new DefaultUriTemplateHandler();
        handler.setBaseUrl(baseUrl);
        return uriTemplateHandler(handler);
    }

    public Rest build() {
        if (requestFactory == null) {
            final SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
            final ExecutorService executor = Executors.newSingleThreadExecutor();
            factory.setTaskExecutor(new ConcurrentTaskExecutor(executor));

            return new Rest(factory, converters(), uriTemplateHandler(), executor::shutdown);
        } else {
            return Rest.create(requestFactory, converters(), uriTemplateHandler());
        }
    }

    private List<HttpMessageConverter<?>> converters() {
        return converters.isEmpty() ? Converters.DEFAULT : converters;
    }

    private UriTemplateHandler uriTemplateHandler() {
        return uriTemplateHandler == null ? new DefaultUriTemplateHandler() : uriTemplateHandler;
    }

}
