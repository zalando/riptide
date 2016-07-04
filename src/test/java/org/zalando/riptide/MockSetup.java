package org.zalando.riptide;

/*
 * ⁣​
 * Riptide
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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.client.AsyncClientHttpRequestFactory;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.AsyncRestTemplate;
import org.springframework.web.util.DefaultUriTemplateHandler;

import java.util.ArrayList;
import java.util.List;

public final class MockSetup {

    private final MockRestServiceServer server;
    private final Rest rest;
    private final List<HttpMessageConverter<?>> converters = new ArrayList<>();

    public MockSetup() {
        this("https://api.example.com");
    }

    public MockSetup(final String baseUrl) {
        final AsyncRestTemplate template = new AsyncRestTemplate();

        this.server = MockRestServiceServer.createServer(template);
        final AsyncClientHttpRequestFactory factory = template.getAsyncRequestFactory();

        this.converters.add(new MappingJackson2HttpMessageConverter(
                new ObjectMapper().findAndRegisterModules()));
        final StringHttpMessageConverter stringHttpMessageConverter = new StringHttpMessageConverter();
        stringHttpMessageConverter.setWriteAcceptCharset(false);
        this.converters.add(stringHttpMessageConverter);

        final DefaultUriTemplateHandler templateHandler = new DefaultUriTemplateHandler();
        templateHandler.setBaseUrl(baseUrl);

        this.rest = Rest.create(factory, converters, templateHandler);
    }

    public MockRestServiceServer getServer() {
        return server;
    }

    public Rest getRest() {
        return rest;
    }

    public List<HttpMessageConverter<?>> getConverters() {
        return converters;
    }

}
