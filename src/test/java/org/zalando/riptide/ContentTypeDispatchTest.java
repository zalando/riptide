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
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;
import org.zalando.riptide.model.Error;
import org.zalando.riptide.model.Problem;
import org.zalando.riptide.model.Success;

import java.io.IOException;
import java.net.URI;

import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY;
import static org.springframework.http.MediaType.parseMediaType;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.zalando.riptide.Conditions.anyContentType;
import static org.zalando.riptide.Conditions.on;
import static org.zalando.riptide.Selectors.contentType;
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
        final RestTemplate template = new RestTemplate();
        final MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
        converter.setObjectMapper(new ObjectMapper().findAndRegisterModules());
        template.setMessageConverters(singletonList(converter));
        template.setErrorHandler(new PassThroughResponseErrorHandler());
        this.server = MockRestServiceServer.createServer(template);
        this.unit = Rest.create(template);
    }

    private <T> T perform(Class<T> type) {
        return unit.execute(GET, url)
                .dispatch(contentType(),
                        on(SUCCESS, Success.class).capture(),
                        on(PROBLEM, Problem.class).capture(),
                        on(ERROR, Error.class).capture(),
                        anyContentType().call(this::fail))
                .retrieve(type).get();
    }

    @Test
    public void shouldDispatchSuccess() {
        server.expect(requestTo(url))
                .andRespond(withSuccess()
                        .body(new ClassPathResource("success.json"))
                        .contentType(SUCCESS));

        final Success success = perform(Success.class);

        assertThat(success.isHappy(), is(true));
    }

    @Test
    public void shouldDispatchProblem() {
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
    public void shouldDispatchError() {
        server.expect(requestTo(url))
                .andRespond(withStatus(UNPROCESSABLE_ENTITY)
                        .body(new ClassPathResource("error.json"))
                        .contentType(ERROR));

        final Error error = perform(org.zalando.riptide.model.Error.class);

        assertThat(error.getMessage(), is("A problem occurred."));
        assertThat(error.getPath(), is(url));
    }

    @Test
    public void shouldDispatchToMostSpecificContentType() {
        server.expect(requestTo(url))
                .andRespond(withSuccess()
                        .body(new ClassPathResource("success.json"))
                        .contentType(SUCCESS));

        final Success success = unit.execute(GET, url)
                .dispatch(contentType(),
                        on(parseMediaType("application/*+json")).call(this::fail),
                        on(SUCCESS, Success.class).capture(),
                        anyContentType().call(this::fail))
                .retrieve(Success.class).get();

        assertThat(success.isHappy(), is(true));
    }

    @Test
    public void shouldNotFailIfNoContentTypeSpecified() {
        server.expect(requestTo(url))
                .andRespond(withSuccess()
                        .body(new ClassPathResource("success.json"))
                        .contentType(null));

        unit.execute(GET, url)
                .dispatch(contentType(),
                        on(SUCCESS, Success.class).capture(),
                        anyContentType().capture());
    }

    @Test
    public void shouldDispatchToFullMatch() {
        server.expect(requestTo(url))
                .andRespond(withSuccess()
                        .body(new ClassPathResource("success.json"))
                        .contentType(SUCCESS_V2));

        final Success success = unit.execute(GET, url)
                .dispatch(contentType(),
                        on(SUCCESS_V1).call(this::fail),
                        on(SUCCESS_V2, Success.class).capture(),
                        anyContentType().call(this::fail))
                .retrieve(Success.class).get();

        assertThat(success.isHappy(), is(true));
    }

    private void fail(ClientHttpResponse response) throws IOException {
        throw new AssertionError(response.getStatusText());
    }

}
