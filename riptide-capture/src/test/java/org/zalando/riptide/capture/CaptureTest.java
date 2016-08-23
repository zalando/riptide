package org.zalando.riptide.capture;

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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.AsyncRestTemplate;
import org.zalando.riptide.Rest;

import java.io.IOException;
import java.util.NoSuchElementException;
import java.util.concurrent.CompletableFuture;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.zalando.riptide.Bindings.anyStatus;
import static org.zalando.riptide.Bindings.on;
import static org.zalando.riptide.Navigators.status;

public final class CaptureTest {

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    private final Rest unit;
    private final MockRestServiceServer server;

    public CaptureTest() {
        final AsyncRestTemplate template = new AsyncRestTemplate();
        this.server = MockRestServiceServer.createServer(template);
        this.unit = Rest.builder()
                .requestFactory(template.getAsyncRequestFactory())
                .converter(new MappingJackson2HttpMessageConverter(new ObjectMapper().findAndRegisterModules()))
                .converter(new StringHttpMessageConverter())
                .baseUrl("https://api.example.com")
                .build();
    }

    @Before
    public void setUp() {
        server.expect(requestTo("https://api.example.com/accounts/123")).andRespond(
                withSuccess()
                        .body(new ClassPathResource("message.json"))
                        .contentType(APPLICATION_JSON));
    }

    @Test
    public void shouldCapture() {
        final Capture<ObjectNode> capture = Capture.empty();

        unit.get("/accounts/123")
                .dispatch(status(),
                        on(OK).call(ObjectNode.class, capture),
                        anyStatus().call(this::fail)).join();

        final ObjectNode node = capture.retrieve();

        assertThat(node.get("message").asText(), is("Hello World!"));
    }

    @Test
    public void shouldAdapt() {
        final Capture<ObjectNode> capture = Capture.empty();

        final CompletableFuture<Void> future = unit.get("/accounts/123")
                .dispatch(status(),
                        on(OK).call(ObjectNode.class, capture),
                        anyStatus().call(this::fail));

        final ObjectNode node = capture.adapt(future).join();

        assertThat(node.get("message").asText(), is("Hello World!"));
    }

    @Test(expected = NoSuchElementException.class)
    public void shouldFail() {
        Capture.empty().retrieve();
    }

    private void fail(final ClientHttpResponse response) throws IOException {
        throw new AssertionError(response.getRawStatusCode());
    }

}
