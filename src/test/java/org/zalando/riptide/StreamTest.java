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

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;
import org.zalando.riptide.model.Account;

import java.io.SequenceInputStream;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.zalando.riptide.Bindings.on;
import static org.zalando.riptide.Selectors.contentType;

public final class StreamTest {

    static class StreamingMappingJackson2HttpMessageConverter extends MappingJackson2HttpMessageConverter {

        public static final MediaType APPLICATION_X_JSON_STREAM = new MediaType("application", "x-json-stream");

        public StreamingMappingJackson2HttpMessageConverter() {
            super(configureMapper(Jackson2ObjectMapperBuilder.json().build()));
        }

        public StreamingMappingJackson2HttpMessageConverter(ObjectMapper objectMapper) {
            super(configureMapper(objectMapper.copy()));
        }

        private static ObjectMapper configureMapper(ObjectMapper objectMapper) {
            objectMapper.getFactory().configure(JsonParser.Feature.AUTO_CLOSE_SOURCE, false);
            return objectMapper;
        }

        @Override
        public List<MediaType> getSupportedMediaTypes() {
            return singletonList(APPLICATION_X_JSON_STREAM);
        }
    }

    private final URI url = URI.create("https://api.example.com/blobs/123");

    private final Rest unit;
    private final MockRestServiceServer server;

    public StreamTest() {
        final RestTemplate template = new RestTemplate();
        final ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new Jdk8Module());
        objectMapper.registerModule(new ParameterNamesModule());
        final StreamingMappingJackson2HttpMessageConverter converter = new StreamingMappingJackson2HttpMessageConverter(objectMapper);
        template.setMessageConverters(Collections.singletonList(converter));
        this.server = MockRestServiceServer.createServer(template);
        this.unit = Rest.create(template);
    }

    @Test(timeout = 500L)
    public void shouldExtractOriginalBody() throws Exception {
        final CloseOnceInputStream content = new CloseOnceInputStream(new SequenceInputStream(new ClassPathResource("account.json").getInputStream(), new ClassPathResource("account.json").getInputStream()));

        server.expect(requestTo(url)).andRespond(
                withSuccess()
                        .body(new InputStreamResource(content))
                        .contentType(StreamingMappingJackson2HttpMessageConverter.APPLICATION_X_JSON_STREAM));

        try (final Stream<Account> stream = unit.execute(GET, url)
                .dispatch(contentType(),
                        on(StreamingMappingJackson2HttpMessageConverter.APPLICATION_X_JSON_STREAM).stream(Account.class))
                .stream(Account.class)) {

            final Account[] accounts = stream.limit(2).toArray(Account[]::new);

            //final Optional<Account> first = stream.findFirst();
            //assertTrue("There should be a first element", first.isPresent());
            assertEquals("There should be two objects in the stream", 2, accounts.length);

            assertEquals("1234567890", accounts[0].getId());
            assertEquals("Acme Corporation", accounts[0].getName());

            //final Optional<Account> second = stream.findFirst();
            //assertTrue("There should be a second element", first.isPresent());
            assertEquals("1234567890", accounts[1].getId());
            assertEquals("Acme Corporation", accounts[1].getName());
        }

        assertTrue("InputStream should be closed", content.isClosed());

    }

}
