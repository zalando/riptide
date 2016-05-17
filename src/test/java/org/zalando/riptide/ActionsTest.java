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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;
import org.zalando.problem.Problem;
import org.zalando.problem.ThrowableProblem;

import java.io.IOException;
import java.net.URI;

import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasToString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hobsoft.hamcrest.compose.ComposeMatchers.hasFeature;
import static org.junit.Assert.assertThat;
import static org.springframework.http.HttpHeaders.CONTENT_LOCATION;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.HEAD;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.HttpStatus.Series.SUCCESSFUL;
import static org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.zalando.riptide.Actions.contentLocation;
import static org.zalando.riptide.Actions.headers;
import static org.zalando.riptide.Actions.location;
import static org.zalando.riptide.Actions.normalize;
import static org.zalando.riptide.Actions.pass;
import static org.zalando.riptide.Actions.propagate;
import static org.zalando.riptide.Conditions.anyStatus;
import static org.zalando.riptide.Conditions.on;
import static org.zalando.riptide.Selectors.series;
import static org.zalando.riptide.Selectors.status;

public final class ActionsTest {

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    private final URI url = URI.create("https://api.example.com/accounts/123");

    private final Rest unit;
    private final MockRestServiceServer server;

    public ActionsTest() {
        final RestTemplate template = new RestTemplate();
        template.setMessageConverters(singletonList(new MappingJackson2HttpMessageConverter(
                new ObjectMapper().findAndRegisterModules())));
        template.setErrorHandler(new PassThroughResponseErrorHandler());
        this.server = MockRestServiceServer.createServer(template);
        this.unit = Rest.create(template);
    }

    @Test
    public void shouldPass() {
        server.expect(requestTo(url)).andRespond(
                withSuccess());

        unit.execute(GET, url)
                .dispatch(status(),
                        on(OK).call(pass()),
                        anyStatus().call(this::fail));
    }

    @Test
    public void shouldMapHeaders() {
        server.expect(requestTo(url)).andRespond(
                withSuccess()
                        .body(new ClassPathResource("account.json"))
                        .contentType(APPLICATION_JSON));

        final HttpHeaders headers = unit.execute(HEAD, url)
                .dispatch(status(),
                        on(OK).capture(headers()),
                        anyStatus().call(this::fail))
                .to(HttpHeaders.class);

        assertThat(headers.toSingleValueMap(), hasEntry("Content-Type", APPLICATION_JSON_VALUE));
    }

    @Test
    public void shouldMapLocation() {
        final HttpHeaders headers = new HttpHeaders();
        headers.setLocation(URI.create("http://example.org"));

        server.expect(requestTo(url)).andRespond(
                withSuccess()
                .headers(headers));

        final URI uri = unit.execute(HEAD, url)
                .dispatch(status(),
                        on(OK).capture(location()),
                        anyStatus().call(this::fail))
                .to(URI.class);

        assertThat(uri, hasToString("http://example.org"));
    }

    @Test
    public void shouldPropagateException() {
        server.expect(requestTo(url)).andRespond(
                withStatus(UNPROCESSABLE_ENTITY)
                        .body(new ClassPathResource("problem.json"))
                        .contentType(APPLICATION_JSON));

        exception.expect(ThrowableProblem.class);
        exception.expect(hasFeature("title", Problem::getTitle, is("Unprocessable Entity")));

        unit.execute(GET, url)
                .dispatch(status(),
                        on(UNPROCESSABLE_ENTITY).call(ThrowableProblem.class, propagate()),
                        anyStatus().call(this::fail));
    }

    @Test
    public void shouldNotFailIfNothingToNormalize() {
        server.expect(requestTo(url)).andRespond(
                withSuccess());

        final ClientHttpResponse response = unit.execute(GET, url)
                .dispatch(series(),
                        on(SUCCESSFUL).capture(normalize(url)))
                .to(ClientHttpResponse.class);

        assertThat(response, hasToString(notNullValue()));
    }

    @Test
    public void shouldNormalizeLocation() {
        final HttpHeaders headers = new HttpHeaders();
        headers.setLocation(URI.create("/accounts/456"));
        server.expect(requestTo(url)).andRespond(
                withSuccess().headers(headers));

        final URI location = unit.execute(GET, url)
                .dispatch(series(),
                        on(SUCCESSFUL).capture(normalize(url).andThen(location())))
                .to(URI.class);

        assertThat(location, hasToString("https://api.example.com/accounts/456"));
    }

    @Test
    public void shouldNormalizeContentLocation() {
        final HttpHeaders headers = new HttpHeaders();
        headers.set(CONTENT_LOCATION, "/accounts/456");
        server.expect(requestTo(url)).andRespond(
                withSuccess().headers(headers));

        final URI location = unit.execute(GET, url)
                .dispatch(series(),
                        on(SUCCESSFUL).capture(normalize(url).andThen(contentLocation())))
                .to(URI.class);

        assertThat(location, hasToString("https://api.example.com/accounts/456"));

    }

    @Test
    public void shouldNormalizeLocationAndContentLocation() {
        final HttpHeaders headers = new HttpHeaders();
        headers.setLocation(URI.create("/accounts/456"));
        headers.set(CONTENT_LOCATION, "/accounts/456");
        server.expect(requestTo(url)).andRespond(
                withSuccess().headers(headers));


        final ClientHttpResponse response = unit.execute(GET, url)
                .dispatch(series(),
                        on(SUCCESSFUL).capture(normalize(url)))
                .to(ClientHttpResponse.class);

        assertThat(response.getHeaders().getLocation(), hasToString("https://api.example.com/accounts/456"));
        assertThat(response.getHeaders().getFirst(CONTENT_LOCATION), is("https://api.example.com/accounts/456"));
    }

    private void fail(final ClientHttpResponse response) throws IOException {
        throw new AssertionError(response.getRawStatusCode());
    }

}
