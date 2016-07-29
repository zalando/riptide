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
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.AsyncRestTemplate;

import java.util.Arrays;

public final class MockSetup {

    private final MockRestServiceServer server;
    private final Rest rest;

    public MockSetup() {
        this("https://api.example.com", Arrays.asList(new StringHttpMessageConverter(),
                new MappingJackson2HttpMessageConverter(new ObjectMapper().findAndRegisterModules())));
    }

    public MockSetup(final String baseUrl, final Iterable<HttpMessageConverter<?>> converters) {
        final AsyncRestTemplate template = new AsyncRestTemplate();
        this.server = MockRestServiceServer.createServer(template);
        this.rest = Rest.builder()
                .requestFactory(template.getAsyncRequestFactory())
                .converters(converters)
                .baseUrl(baseUrl)
                .build();
    }

    public MockRestServiceServer getServer() {
        return server;
    }

    public Rest getRest() {
        return rest;
    }

}
