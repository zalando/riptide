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

import java.util.Arrays;
import java.util.List;

import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.AsyncRestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;

public final class MockSetup {

    private static final List<HttpMessageConverter<?>> DEFAULT_CONVERTERS =
            Arrays.asList(new StringHttpMessageConverter(),
                    new MappingJackson2HttpMessageConverter(new ObjectMapper().findAndRegisterModules()));

    private final MockRestServiceServer server;
    private final RestBuilder builder;

    public MockSetup() {
        this("https://api.example.com", null);
    }

    public MockSetup(final String baseUrl, final Iterable<HttpMessageConverter<?>> converters) {
        final AsyncRestTemplate template = new AsyncRestTemplate();
        this.server = MockRestServiceServer.createServer(template);
        this.builder = Rest.builder()
                .requestFactory(template.getAsyncRequestFactory())
                .converters(converters != null ? converters : DEFAULT_CONVERTERS)
                .baseUrl(baseUrl);
    }

    public MockRestServiceServer getServer() {
        return server;
    }

    public RestBuilder getRestBuilder() {
        return builder.clone();
    }
    public Rest getRest() {
        return builder.build();
    }

}
