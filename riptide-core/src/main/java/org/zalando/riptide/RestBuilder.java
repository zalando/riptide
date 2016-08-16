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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;

import org.springframework.http.client.AsyncClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.scheduling.concurrent.ConcurrentTaskExecutor;
import org.springframework.web.client.RestTemplate;

import com.google.common.collect.ImmutableList;

public final class RestBuilder {

    // package private so we can trick code coverage
    static class Converters {
        private static final ImmutableList<HttpMessageConverter<?>> DEFAULT =
                ImmutableList.copyOf(new RestTemplate().getMessageConverters());
    }

    private AsyncClientHttpRequestFactory requestFactory;
    private final List<HttpMessageConverter<?>> converters = new ArrayList<>();
    private String baseUrl;

    RestBuilder() {

    }

    public RestBuilder requestFactory(final AsyncClientHttpRequestFactory requestFactory) {
        this.requestFactory = requestFactory;
        return this;
    }

    public RestBuilder defaultConverters() {
        return converters(Converters.DEFAULT);
    }

    public RestBuilder clearConverters() {
        converters.clear();
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

    public RestBuilder baseUrl(final String baseUrl) {
        this.baseUrl = baseUrl;
        return this;
    }

    public RestBuilder configure(final RestConfigurer configurer) {
        configurer.configure(this);
        return this;
    }

    public static RestConfigurer simpleRequestFactory(final ExecutorService executor) {
        return builder -> {
            final SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
            factory.setTaskExecutor(new ConcurrentTaskExecutor(executor));
            builder.requestFactory(factory);
        };
    }

    public Rest build() {
        return new Rest(requestFactory, converters(), baseUrl);
    }

    private List<HttpMessageConverter<?>> converters() {
        return converters.isEmpty() ? Converters.DEFAULT : converters;
    }

}
