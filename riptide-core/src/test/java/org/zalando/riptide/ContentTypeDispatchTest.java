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

import org.junit.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.test.web.client.MockRestServiceServer;
import org.zalando.riptide.model.Error;
import org.zalando.riptide.model.Problem;
import org.zalando.riptide.model.Success;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.springframework.http.HttpStatus.Series.SUCCESSFUL;
import static org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY;
import static org.springframework.http.MediaType.parseMediaType;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.zalando.riptide.Bindings.anyContentType;
import static org.zalando.riptide.Bindings.anySeries;
import static org.zalando.riptide.Bindings.on;
import static org.zalando.riptide.Navigators.contentType;
import static org.zalando.riptide.Navigators.series;
import static org.zalando.riptide.Routes.pass;
import static org.zalando.riptide.model.MediaTypes.ERROR;
import static org.zalando.riptide.model.MediaTypes.PROBLEM;
import static org.zalando.riptide.model.MediaTypes.SUCCESS;
import static org.zalando.riptide.model.MediaTypes.SUCCESS_V1;
import static org.zalando.riptide.model.MediaTypes.SUCCESS_V2;

public final class ContentTypeDispatchTest {

    private final URI url = URI.create("https://api.example.com");

    private final Rest unit;
    private final MockRestServiceServer server;

    public ContentTypeDispatchTest() {
        final MockSetup setup = new MockSetup();
        this.unit = setup.getRest();
        this.server = setup.getServer();
    }

    private <T> T perform(final Class<T> type) throws ExecutionException, InterruptedException, IOException {
        final AtomicReference<Object> capture = new AtomicReference<>();

        unit.get(url)
                .dispatch(contentType(),
                        on(SUCCESS).call(Success.class, capture::set),
                        on(PROBLEM).call(Problem.class, capture::set),
                        on(ERROR).call(Error.class, capture::set),
                        anyContentType().call(this::fail))
                .get();

        return type.cast(capture.get());
    }

    @Test
    public void shouldDispatchSuccess() throws ExecutionException, InterruptedException, IOException {
        server.expect(requestTo(url))
                .andRespond(withSuccess()
                        .body(new ClassPathResource("success.json"))
                        .contentType(SUCCESS));

        final Success success = perform(Success.class);

        assertThat(success.isHappy(), is(true));
    }

    @Test
    public void shouldDispatchProblem() throws ExecutionException, InterruptedException, IOException {
        server.expect(requestTo(url))
                .andRespond(withStatus(UNPROCESSABLE_ENTITY)
                        .body(new ClassPathResource("problem.json"))
                        .contentType(PROBLEM));

        final Problem problem = perform(Problem.class);

        assertThat(problem.getType(), is(URI.create("http://httpstatus.es/422")));
        assertThat(problem.getTitle(), is("Unprocessable Entity"));
        assertThat(problem.getStatus(), is(422));
        assertThat(problem.getDetail(), is("A problem occurred."));
    }

    @Test
    public void shouldDispatchError() throws ExecutionException, InterruptedException, IOException {
        server.expect(requestTo(url))
                .andRespond(withStatus(UNPROCESSABLE_ENTITY)
                        .body(new ClassPathResource("error.json"))
                        .contentType(ERROR));

        final Error error = perform(org.zalando.riptide.model.Error.class);

        assertThat(error.getMessage(), is("A problem occurred."));
        assertThat(error.getPath(), is(url));
    }

    @Test
    public void shouldDispatchToMostSpecificContentType() throws ExecutionException, InterruptedException, IOException {
        server.expect(requestTo(url))
                .andRespond(withSuccess()
                        .body(new ClassPathResource("success.json"))
                        .contentType(SUCCESS));

        final AtomicReference<Success> capture = new AtomicReference<>();

        unit.get(url)
                .dispatch(contentType(),
                        on(parseMediaType("application/*+json")).call(this::fail),
                        on(parseMediaType("application/success+json;version=2")).call(Success.class, capture::set),
                        anyContentType().call(this::fail))
                .get();

        final Success success = capture.get();
        assertThat(success.isHappy(), is(true));
    }

    @Test
    public void shouldNotFailIfNoContentTypeSpecified() throws ExecutionException, InterruptedException, IOException {
        server.expect(requestTo(url))
                .andRespond(withSuccess()
                        .body(new ClassPathResource("success.json"))
                        .contentType(null));

        unit.get(url)
                .dispatch(series(),
                        on(SUCCESSFUL).dispatch(contentType(),
                                on(SUCCESS).call(pass())),
                        anySeries().call(pass()))
                .get();
    }

    @Test
    public void shouldDispatchToFullMatch() throws ExecutionException, InterruptedException, IOException {
        server.expect(requestTo(url))
                .andRespond(withSuccess()
                        .body(new ClassPathResource("success.json"))
                        .contentType(SUCCESS_V2));

        final AtomicReference<Success> capture = new AtomicReference<>();

        unit.get(url)
                .dispatch(contentType(),
                        on(SUCCESS_V1).call(this::fail),
                        on(SUCCESS_V2).call(Success.class, capture::set),
                        anyContentType().call(this::fail))
                .get();

        final Success success = capture.get();
        assertThat(success.isHappy(), is(true));
    }

    private void fail(final ClientHttpResponse response) throws IOException {
        throw new AssertionError(response.getStatusText());
    }

}
