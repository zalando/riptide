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

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClientException;
import org.zalando.riptide.model.Error;
import org.zalando.riptide.model.MediaTypes;
import org.zalando.riptide.model.Problem;
import org.zalando.riptide.model.Success;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.ExecutionException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hobsoft.hamcrest.compose.ComposeMatchers.hasFeature;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.springframework.http.HttpStatus.ACCEPTED;
import static org.springframework.http.HttpStatus.MOVED_PERMANENTLY;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.HttpStatus.Series.CLIENT_ERROR;
import static org.springframework.http.HttpStatus.Series.SUCCESSFUL;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.APPLICATION_XML;
import static org.springframework.http.MediaType.TEXT_PLAIN;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withCreatedEntity;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.zalando.riptide.Bindings.anyContentType;
import static org.zalando.riptide.Bindings.anySeries;
import static org.zalando.riptide.Bindings.anyStatus;
import static org.zalando.riptide.Bindings.on;
import static org.zalando.riptide.Navigators.contentType;
import static org.zalando.riptide.Navigators.series;
import static org.zalando.riptide.Navigators.status;
import static org.zalando.riptide.model.MediaTypes.ERROR;
import static org.zalando.riptide.model.MediaTypes.PROBLEM;
import static org.zalando.riptide.model.MediaTypes.SUCCESS;

public final class FailedDispatchTest {

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    private final String url = "https://api.example.com";

    private final Rest unit;
    private final MockRestServiceServer server;

    public FailedDispatchTest() {
        final MockSetup setup = new MockSetup();
        this.unit = setup.getRest();
        this.server = setup.getServer();
    }

    @Test
    public void shouldThrowIfNoMatch() throws ExecutionException, InterruptedException, IOException {
        server.expect(requestTo(url))
                .andRespond(withSuccess()
                        .body(new ClassPathResource("success.json"))
                        .contentType(APPLICATION_JSON));

        exception.expect(ExecutionException.class);
        exception.expectCause(instanceOf(NoRouteException.class));
        exception.expectMessage("Unable to dispatch response (200 OK, Content-Type: application/json)");
        exception.expectCause(hasFeature("response", NoRouteException::getResponse, notNullValue()));

        unit.options(url)
                .dispatch(contentType(),
                        // note that we don't match on application/json explicitly
                        on(SUCCESS).capture(Success.class),
                        on(PROBLEM).capture(Problem.class),
                        on(ERROR).capture(Error.class))
                .get();
    }

    @Test
    public void shouldThrowOnFailedConversionBecauseOfUnknownContentType() throws ExecutionException, InterruptedException, IOException {
        server.expect(requestTo(url))
                .andRespond(withSuccess()
                        .body("{}")
                        .contentType(MediaType.APPLICATION_ATOM_XML));

        exception.expect(ExecutionException.class);
        exception.expectCause(instanceOf(RestClientException.class));
        exception.expectMessage("no suitable HttpMessageConverter found for response type");

        unit.get(url)
                .dispatch(status(),
                        on(HttpStatus.OK)
                                .dispatch(series(),
                                        on(SUCCESSFUL).capture(Success.class),
                                        anySeries().capture()),
                        on(HttpStatus.CREATED).capture(Success.class),
                        anyStatus().call(this::fail))
                .get();
    }

    @Test
    public void shouldThrowOnFailedConversionBecauseOfFaultyBody() throws ExecutionException, InterruptedException, IOException {
        server.expect(requestTo(url))
                .andRespond(withSuccess()
                        .body("{")
                        .contentType(MediaTypes.SUCCESS));

        exception.expect(ExecutionException.class);
        exception.expectCause(instanceOf(HttpMessageNotReadableException.class));
        exception.expectMessage("Could not read");

        unit.get(url)
                .dispatch(status(),
                        on(HttpStatus.OK)
                                .dispatch(series(),
                                        on(SUCCESSFUL).capture(Success.class),
                                        anySeries().capture()),
                        on(HttpStatus.CREATED).capture(Success.class),
                        anyStatus().call(this::fail))
                .get();
    }

    @Test
    public void shouldHandleNoBodyAtAll() throws ExecutionException, InterruptedException, IOException {
        server.expect(requestTo(url))
                .andRespond(withStatus(HttpStatus.OK)
                        .body("")
                        .contentType(MediaTypes.SUCCESS));

        final Capture capture = unit.get(url)
                .dispatch(status(),
                        on(HttpStatus.OK)
                                .dispatch(contentType(),
                                        on(MediaTypes.SUCCESS).capture(Success.class),
                                        anyContentType().call(this::fail)),
                        on(HttpStatus.CREATED).capture(Success.class),
                        anyStatus().call(this::fail))
                .get();

        assertThat(capture.as(Success.class).isPresent(), is(false));
    }

    private void fail(final ClientHttpResponse response) {
        throw new AssertionError("Should not have been executed");
    }

    @Test
    public void shouldPropagateIfNoMatch() throws Exception {
        server.expect(requestTo(url))
                .andRespond(withSuccess()
                        .body(new ClassPathResource("success.json"))
                        .contentType(APPLICATION_JSON));

        final ClientHttpResponseConsumer consumer = mock(ClientHttpResponseConsumer.class);

        unit.get(url)
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
    public void shouldPropagateMultipleLevelsIfNoMatch() throws Exception {
        server.expect(requestTo(url))
                .andRespond(withSuccess()
                        .body(new ClassPathResource("success.json"))
                        .contentType(APPLICATION_JSON));

        final ClientHttpResponseConsumer consumer = mock(ClientHttpResponseConsumer.class);

        unit.get(url)
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
    public void shouldPreserveExceptionIfPropagateFailed() throws ExecutionException, InterruptedException, IOException {
        server.expect(requestTo(url))
                .andRespond(withCreatedEntity(URI.create("about:blank"))
                        .body(new ClassPathResource("success.json"))
                        .contentType(APPLICATION_JSON));

        exception.expect(ExecutionException.class);
        exception.expectCause(instanceOf(NoRouteException.class));
        exception.expectMessage("Unable to dispatch response (201 Created, Content-Type: application/json)");
        exception.expectCause(hasFeature("response", NoRouteException::getResponse, notNullValue()));

        unit.post(url)
                .dispatch(series(),
                        on(SUCCESSFUL).dispatch(contentType(),
                                on(APPLICATION_JSON).dispatch(status(),
                                        on(OK).capture(),
                                        on(MOVED_PERMANENTLY).capture(),
                                        on(NOT_FOUND).capture()),
                                on(APPLICATION_XML).capture(),
                                on(TEXT_PLAIN).capture()),
                        on(CLIENT_ERROR).capture())
                .get();
    }

}
