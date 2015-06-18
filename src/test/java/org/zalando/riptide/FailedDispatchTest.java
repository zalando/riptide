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
import org.hamcrest.core.Is;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;
import org.zalando.riptide.handler.PassThroughResponseErrorHandler;
import org.zalando.riptide.model.Error;
import org.zalando.riptide.model.MediaTypes;
import org.zalando.riptide.model.Problem;
import org.zalando.riptide.model.Success;

import java.io.IOException;
import java.net.URI;

import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hobsoft.hamcrest.compose.ComposeMatchers.hasFeature;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpStatus.ACCEPTED;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.HttpStatus.Series.CLIENT_ERROR;
import static org.springframework.http.HttpStatus.Series.SUCCESSFUL;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.APPLICATION_XML;
import static org.springframework.http.MediaType.TEXT_PLAIN;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.zalando.riptide.Conditions.anyContentType;
import static org.zalando.riptide.Conditions.anySeries;
import static org.zalando.riptide.Conditions.anyStatus;
import static org.zalando.riptide.Conditions.on;
import static org.zalando.riptide.Selectors.contentType;
import static org.zalando.riptide.Selectors.series;
import static org.zalando.riptide.Selectors.status;
import static org.zalando.riptide.model.MediaTypes.ERROR;
import static org.zalando.riptide.model.MediaTypes.PROBLEM;
import static org.zalando.riptide.model.MediaTypes.SUCCESS;

public final class FailedDispatchTest {

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    private final URI url = URI.create("https://api.example.com");

    private final Rest unit;
    private final MockRestServiceServer server;

    public FailedDispatchTest() {
        final RestTemplate template = new RestTemplate();
        final MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
        converter.setObjectMapper(new ObjectMapper().findAndRegisterModules());
        template.setMessageConverters(singletonList(converter));
        template.setErrorHandler(new PassThroughResponseErrorHandler());
        this.server = MockRestServiceServer.createServer(template);
        this.unit = Rest.create(template);
    }

    @Test
    public void shouldFailIfNoMatch() {
        server.expect(requestTo(url))
                .andRespond(withSuccess()
                        .body(new ClassPathResource("success.json"))
                        .contentType(APPLICATION_JSON));

        exception.expect(UnsupportedResponseException.class);
        exception.expectMessage("Unable to dispatch application/json");
        exception.expectMessage("application/success+json");
        exception.expectMessage("application/problem+json");
        exception.expectMessage("application/vnd.error+json");
        exception.expect(hasFeature("response", UnsupportedResponseException::getResponse, is(notNullValue())));

        unit.execute(GET, url)
                .dispatch(contentType(),
                        // note that we don't match on application/json explicitly
                        on(SUCCESS, Success.class).capture(),
                        on(PROBLEM, Problem.class).capture(),
                        on(ERROR, Error.class).capture());
    }

    @Test
    public void shouldFallbackToAnyMatcherOnFailedConversionBecauseOfUnknownContentType() throws IOException {
        server.expect(requestTo(url))
              .andRespond(withSuccess()
                      .body("{}")
                      .contentType(MediaType.APPLICATION_ATOM_XML));

        final ClientHttpResponseConsumer consumer = mock(ClientHttpResponseConsumer.class);

        unit.execute(GET, url)
            .dispatch(status(),
                    on(HttpStatus.OK)
                            .dispatch(series(),
                                    on(SUCCESSFUL, Success.class).capture(),
                                    anySeries().call(consumer)),
                    on(HttpStatus.CREATED, Success.class).capture(),
                    anyStatus().call(this::fail));

        verify(consumer).accept(any());
    }

    @Test
    public void shouldFallbackToAnyMatcherOnFailedConversionBecauseOfFaultyBody() throws IOException {
        server.expect(requestTo(url))
              .andRespond(withSuccess()
                      .body("{")
                      .contentType(MediaTypes.SUCCESS));

        final ClientHttpResponseConsumer consumer = mock(ClientHttpResponseConsumer.class);

        unit.execute(GET, url)
            .dispatch(status(),
                    on(HttpStatus.OK)
                            .dispatch(series(),
                                    on(SUCCESSFUL, Success.class).capture(),
                                    anySeries().call(consumer)),
                    on(HttpStatus.CREATED, Success.class).capture(),
                    anyStatus().call(this::fail));

        verify(consumer).accept(any());
    }

    @Test
    public void shouldHandleNoBodyAtAll() {
        server.expect(requestTo(url))
              .andRespond(withStatus(HttpStatus.OK)
                      .body("")
                      .contentType(MediaTypes.SUCCESS));

        final Retriever retriever = unit.execute(GET, url)
                                        .dispatch(status(),
                                                on(HttpStatus.OK)
                                                        .dispatch(contentType(),
                                                                on(MediaTypes.SUCCESS, Success.class).capture(),
                                                                anyContentType().call(this::fail)),
                                                on(HttpStatus.CREATED, Success.class).capture(),
                                                anyStatus().call(this::fail));

        assertThat(retriever.retrieve(Success.class).isPresent(), Is.is(false));
    }

    private void fail(final ClientHttpResponse response) {
        throw new AssertionError("Should not have been executed");
    }

    @Test
    public void shouldPropagateIfNoMatch() throws IOException {
        server.expect(requestTo(url))
                .andRespond(withSuccess()
                        .body(new ClassPathResource("success.json"))
                        .contentType(APPLICATION_JSON));

        final ClientHttpResponseConsumer consumer = mock(ClientHttpResponseConsumer.class);

        unit.execute(GET, url)
                .dispatch(series(),
                        on(SUCCESSFUL).dispatch(status(),
                                on(OK).dispatch(contentType(),
                                        on(APPLICATION_XML).capture(),
                                        on(TEXT_PLAIN).capture()),
                                on(ACCEPTED).capture(),
                                anyStatus().call(consumer)),
                        on(CLIENT_ERROR).capture());

        verify(consumer).accept(any());
    }

    @Test
    public void shouldPropagateMultipleLevelsIfNoMatch() throws IOException {
        server.expect(requestTo(url))
                .andRespond(withSuccess()
                        .body(new ClassPathResource("success.json"))
                        .contentType(APPLICATION_JSON));

        final ClientHttpResponseConsumer consumer = mock(ClientHttpResponseConsumer.class);

        unit.execute(GET, url)
                .dispatch(series(),
                        on(SUCCESSFUL).dispatch(status(),
                                on(OK).dispatch(contentType(),
                                        on(APPLICATION_XML).capture(),
                                        on(TEXT_PLAIN).capture()),
                                on(ACCEPTED).capture()),
                        on(CLIENT_ERROR).capture(),
                        anySeries().call(consumer));

        verify(consumer).accept(any());
    }
    
    @Test
    public void shouldPreserveExceptionIfPropagateFailed() {
        server.expect(requestTo(url))
                .andRespond(withSuccess()
                        .body(new ClassPathResource("success.json"))
                        .contentType(APPLICATION_JSON));

        exception.expect(UnsupportedResponseException.class);
        exception.expectMessage("Unable to dispatch application/json");
        exception.expectMessage("application/xml");
        exception.expectMessage("text/plain");
        exception.expect(hasFeature("response", UnsupportedResponseException::getResponse, is(notNullValue())));
    
        unit.execute(GET, url)
                .dispatch(series(),
                        on(SUCCESSFUL).dispatch(status(),
                                on(OK).dispatch(contentType(),
                                        on(APPLICATION_XML).capture(),
                                        on(TEXT_PLAIN).capture()),
                                on(ACCEPTED).capture()),
                        on(CLIENT_ERROR).capture());    
    }

}
