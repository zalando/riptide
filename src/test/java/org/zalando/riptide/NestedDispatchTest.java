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
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.AsyncRestTemplate;
import org.zalando.riptide.model.Problem;
import org.zalando.riptide.model.Success;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.ExecutionException;

import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hobsoft.hamcrest.compose.ComposeMatchers.hasFeature;
import static org.junit.Assert.assertThat;
import static org.springframework.http.HttpStatus.ACCEPTED;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.MOVED_PERMANENTLY;
import static org.springframework.http.HttpStatus.Series.CLIENT_ERROR;
import static org.springframework.http.HttpStatus.Series.SERVER_ERROR;
import static org.springframework.http.HttpStatus.Series.SUCCESSFUL;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;
import static org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.zalando.riptide.Bindings.anyContentType;
import static org.zalando.riptide.Bindings.anySeries;
import static org.zalando.riptide.Bindings.anyStatus;
import static org.zalando.riptide.Bindings.anyStatusCode;
import static org.zalando.riptide.Bindings.on;
import static org.zalando.riptide.Navigators.contentType;
import static org.zalando.riptide.Navigators.series;
import static org.zalando.riptide.Navigators.status;
import static org.zalando.riptide.Navigators.statusCode;
import static org.zalando.riptide.model.MediaTypes.ERROR;
import static org.zalando.riptide.model.MediaTypes.PROBLEM;

public final class NestedDispatchTest {

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    private final URI url = URI.create("http://localhost");

    private final Rest unit;
    private final MockRestServiceServer server;

    public NestedDispatchTest() {
        final AsyncRestTemplate template = new AsyncRestTemplate();
        final MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
        converter.setObjectMapper(new ObjectMapper().findAndRegisterModules());
        template.setMessageConverters(singletonList(converter));
        this.server = MockRestServiceServer.createServer(template);
        this.unit = Rest.create(template);
    }

    private <T> T perform(final Class<T> type) throws ExecutionException, InterruptedException, IOException {
        return unit.get(url)
                .dispatch(series(),
                        on(SUCCESSFUL)
                                .dispatch(status(),
                                        on(CREATED).capture(Success.class),
                                        on(ACCEPTED).capture(Success.class),
                                        anyStatus().call(this::fail)),
                        on(CLIENT_ERROR)
                                .dispatch(status(),
                                        on(UNAUTHORIZED).capture(),
                                        anyStatus().bind(problemHandling())),
                        on(SERVER_ERROR)
                                .dispatch(statusCode(),
                                        on(500).capture(),
                                        on(503).capture(),
                                        anyStatusCode().call(this::fail)),
                        anySeries().call(this::fail))
                .get().as(type).orElse(null);
    }

    private Route problemHandling() {
        return Route.dispatch(contentType(),
                on(PROBLEM).capture(Problem.class),
                on(ERROR).capture(Problem.class),
                anyContentType().call(this::fail));
    }

    private static final class Failure extends RuntimeException {
        private final HttpStatus status;

        private Failure(final HttpStatus status) {
            this.status = status;
        }

        public HttpStatus getStatus() {
            return status;
        }
    }

    private void fail(final ClientHttpResponse response) throws IOException {
        throw new Failure(response.getStatusCode());
    }

    @Test
    public void shouldDispatchLevelOne() throws ExecutionException, InterruptedException, IOException {
        server.expect(requestTo(url)).andRespond(withStatus(MOVED_PERMANENTLY));

        exception.expect(ExecutionException.class);
        exception.expectCause(instanceOf(Failure.class));
        exception.expectCause(hasFeature("status", Failure::getStatus, equalTo(MOVED_PERMANENTLY)));

        perform(Void.class);
    }

    @Test
    public void shouldDispatchLevelTwo() throws ExecutionException, InterruptedException, IOException {
        server.expect(requestTo(url)).andRespond(
                withStatus(CREATED)
                        .body(new ClassPathResource("success.json"))
                        .contentType(APPLICATION_JSON));

        final Success success = perform(Success.class);

        assertThat(success.isHappy(), is(true));
    }

    @Test
    public void shouldDispatchLevelThree() throws ExecutionException, InterruptedException, IOException {
        server.expect(requestTo(url)).andRespond(
                withStatus(UNPROCESSABLE_ENTITY)
                        .body(new ClassPathResource("problem.json"))
                        .contentType(ERROR));

        final Problem problem = perform(Problem.class);

        assertThat(problem.getType(), is(URI.create("http://httpstatus.es/422")));
        assertThat(problem.getTitle(), is("Unprocessable Entity"));
        assertThat(problem.getStatus(), is(422));
        assertThat(problem.getDetail(), is("A problem occurred."));
    }

}
