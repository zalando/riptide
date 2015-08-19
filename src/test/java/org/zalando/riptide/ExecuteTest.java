package org.zalando.riptide;

/*
 * ⁣​
 * riptide
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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.google.gag.annotation.remark.Hack;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.converter.xml.Jaxb2RootElementHttpMessageConverter;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.zalando.riptide.model.Success;

import java.net.URI;
import java.util.List;

import static java.util.Collections.singletonList;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.MediaType.APPLICATION_XML;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

public final class ExecuteTest {

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    private final URI url = URI.create("https://api.example.com");

    private final RestTemplate template;
    private final Rest unit;
    private final MockRestServiceServer server;

    public ExecuteTest() {
        this.template = new RestTemplate();
        final MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
        converter.setObjectMapper(new ObjectMapper().findAndRegisterModules());
        template.setMessageConverters(singletonList(converter));
        template.setErrorHandler(new PassThroughResponseErrorHandler());
        this.server = MockRestServiceServer.createServer(template);
        this.unit = Rest.create(template);
    }

    @Test
    public void shouldSendNoBody() {
        server.expect(requestTo(url))
                .andExpect(content().string(""))
                .andRespond(withSuccess());

        unit.execute(GET, url);
    }

    @Test
    public void shouldSendHeaders() {
        server.expect(requestTo(url))
                .andExpect(header("X-Foo", "bar"))
                .andRespond(withSuccess());

        final HttpHeaders headers = new HttpHeaders();
        headers.set("X-Foo", "bar");

        unit.execute(GET, url, headers);
    }

    @Test
    public void shouldSendBody() {
        server.expect(requestTo(url))
                .andExpect(content().string("{\"foo\":\"bar\"}"))
                .andRespond(withSuccess());

        unit.execute(GET, url, ImmutableMap.of("foo", "bar"));
    }

    @Test
    public void shouldSendHeadersAndBody() {
        server.expect(requestTo(url))
                .andExpect(header("X-Foo", "bar"))
                .andExpect(content().string("{\"foo\":\"bar\"}"))
                .andRespond(withSuccess());

        final HttpHeaders headers = new HttpHeaders();
        headers.set("X-Foo", "bar");

        unit.execute(GET, url, headers, ImmutableMap.of("foo", "bar"));
    }

    @Test
    public void shouldFailIfNoConverterFoundForBody() {
        // we never actually make the request, but the mock server is doing some magic pre-actively
        server.expect(requestTo(url))
                .andRespond(withSuccess());

        final HttpHeaders headers = new HttpHeaders();
        headers.setContentType(APPLICATION_XML);

        exception.expect(RestClientException.class);
        exception.expectMessage("no suitable HttpMessageConverter found ");
        exception.expectMessage("org.zalando.riptide.model.Success");
        exception.expectMessage("application/xml");

        unit.execute(GET, url, headers, new Success(true));
    }

    @Test
    public void shouldFailIfNoConverterFoundForBodyOfUnknownContentType() {
        @Hack("Couldn't find a better way to prevent the Jackson converter from running")
        final List<HttpMessageConverter<?>> converters = singletonList(new Jaxb2RootElementHttpMessageConverter());
        template.setMessageConverters(converters);

        // we never actually make the request, but the mock server is doing some magic pre-actively
        server.expect(requestTo(url))
                .andRespond(withSuccess());

        exception.expect(RestClientException.class);
        exception.expectMessage("no suitable HttpMessageConverter found ");
        exception.expectMessage("org.zalando.riptide.model.Success");

        unit.execute(GET, url, new Success(true));
    }

}
