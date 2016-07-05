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
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.test.web.client.MockRestServiceServer;
import org.zalando.problem.ThrowableProblem;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;

import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasToString;
import static org.hamcrest.Matchers.instanceOf;
import static org.hobsoft.hamcrest.compose.ComposeMatchers.hasFeature;
import static org.junit.Assert.assertThat;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.zalando.riptide.Bindings.anyStatus;
import static org.zalando.riptide.Bindings.on;
import static org.zalando.riptide.Navigators.status;
import static org.zalando.riptide.Routes.to;
import static org.zalando.riptide.Routes.headers;
import static org.zalando.riptide.Routes.location;
import static org.zalando.riptide.Routes.pass;
import static org.zalando.riptide.Routes.propagate;

public final class RoutesTest {

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    private final URI url = URI.create("https://api.example.com/accounts/123");

    private final Rest unit;
    private final MockRestServiceServer server;

    public RoutesTest() {
        final MockSetup setup = new MockSetup();
        this.unit = setup.getRest();
        this.server = setup.getServer();
    }

    @Test
    public void shouldPass() throws IOException {
        server.expect(requestTo(url)).andRespond(
                withSuccess());

        unit.get(url)
                .dispatch(status(),
                        on(OK).call(pass()),
                        anyStatus().call(this::fail));
    }

    @Test
    public void shouldMapHeaders() throws ExecutionException, InterruptedException, IOException {
        server.expect(requestTo(url)).andRespond(
                withSuccess()
                        .body(new ClassPathResource("account.json"))
                        .contentType(APPLICATION_JSON));

        final AtomicReference<HttpHeaders> capture = new AtomicReference<>();

        unit.head(url)
                .dispatch(status(),
                        on(OK).call(to(headers()).andThen(capture::set)),
                        anyStatus().call(this::fail))
                .get();

        final HttpHeaders headers = capture.get();
        assertThat(headers.toSingleValueMap(), hasEntry("Content-Type", APPLICATION_JSON_VALUE));
    }

    @Test
    public void shouldMapLocation() throws ExecutionException, InterruptedException, IOException {
        final HttpHeaders headers = new HttpHeaders();
        headers.setLocation(URI.create("http://example.org"));

        server.expect(requestTo(url)).andRespond(
                withSuccess()
                        .headers(headers));

        final AtomicReference<URI> capture = new AtomicReference<>();

        unit.head(url)
                .dispatch(status(),
                        on(OK).call(to(location()).andThen(capture::set)),
                        anyStatus().call(this::fail))
                .get();

        final URI uri = capture.get();
        assertThat(uri, hasToString("http://example.org"));
    }

    @Test
    public void shouldPropagateException() throws ExecutionException, InterruptedException, IOException {
        server.expect(requestTo(url)).andRespond(
                withStatus(UNPROCESSABLE_ENTITY)
                        .body(new ClassPathResource("problem.json"))
                        .contentType(APPLICATION_JSON));

        exception.expect(ExecutionException.class);
        exception.expectCause(instanceOf(IOException.class));
        exception.expectCause(hasFeature("cause", Throwable::getCause, instanceOf(ThrowableProblem.class)));

        unit.get(url)
                .dispatch(status(),
                        on(UNPROCESSABLE_ENTITY).call(ThrowableProblem.class, propagate()),
                        anyStatus().call(this::fail))
                .get();
    }

    @Test
    public void shouldPropagateIOExceptionAsIs() throws ExecutionException, InterruptedException, IOException {
        server.expect(requestTo(url)).andRespond(
                withStatus(UNPROCESSABLE_ENTITY)
                        .body("{}")
                        .contentType(APPLICATION_JSON));

        exception.expect(ExecutionException.class);
        exception.expectCause(instanceOf(IOException.class));

        unit.get(url)
                .dispatch(status(),
                        on(UNPROCESSABLE_ENTITY).call(IOException.class, propagate()),
                        anyStatus().call(this::fail))
                .get();
    }

    private void fail(final ClientHttpResponse response) throws IOException {
        throw new AssertionError(response.getRawStatusCode());
    }

}
