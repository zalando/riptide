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
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.URI;

import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hobsoft.hamcrest.compose.ComposeMatchers.hasFeature;
import static org.junit.Assert.assertThat;
import static org.springframework.http.HttpMethod.GET;
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
import static org.zalando.riptide.Conditions.anyContentType;
import static org.zalando.riptide.Conditions.anySeries;
import static org.zalando.riptide.Conditions.anyStatus;
import static org.zalando.riptide.Conditions.anyStatusCode;
import static org.zalando.riptide.Conditions.on;
import static org.zalando.riptide.MediaTypes.ERROR;
import static org.zalando.riptide.MediaTypes.PROBLEM;
import static org.zalando.riptide.Selectors.contentType;
import static org.zalando.riptide.Selectors.series;
import static org.zalando.riptide.Selectors.status;
import static org.zalando.riptide.Selectors.statusCode;

public final class NestedDispatchTest {

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    private final URI url = URI.create("http://localhost");

    private final Rest unit;
    private final MockRestServiceServer server;

    public NestedDispatchTest() {
        final RestTemplate template = new RestTemplate();
        final MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
        converter.setObjectMapper(new ObjectMapper().findAndRegisterModules());
        template.setMessageConverters(singletonList(converter));
        template.setErrorHandler(new PassThroughResponseErrorHandler());
        this.server = MockRestServiceServer.createServer(template);
        this.unit = Rest.create(template);
    }

    private <T> T perform(Class<T> type) throws IOException {
        return unit.execute(GET, url)
                .dispatch(series(),
                        on(SUCCESSFUL)
                                .dispatch(status(),
                                        on(CREATED, Success.class).capture(),
                                        on(ACCEPTED, Success.class).capture(),
                                        anyStatus().call(this::fail)),
                        on(CLIENT_ERROR)
                            .dispatch(status(),
                                    on(UNAUTHORIZED).capture(),
                                    on(UNPROCESSABLE_ENTITY)
                                            .dispatch(contentType(),
                                                    on(PROBLEM, Problem.class).capture(),
                                                    on(ERROR, Problem.class).capture(),
                                                    anyContentType().call(this::fail)),
                                    anyStatus().call(this::fail)),
                        on(SERVER_ERROR)
                            .dispatch(statusCode(),
                                    on(500).capture(),
                                    on(503).capture(),
                                    anyStatusCode().call(this::fail)),
                        anySeries().call(this::fail))
                .retrieve(type).orElse(null);
    }
    
    private static final class Failure extends RuntimeException {
        private final HttpStatus status;

        private Failure(HttpStatus status) {
            this.status = status;
        }

        public HttpStatus getStatus() {
            return status;
        }
    }
    
    private void fail(ClientHttpResponse response) {
        try {
            throw new Failure(response.getStatusCode());
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }
    
    @Test
    public void shouldDispatchLevelOne() throws IOException {
        server.expect(requestTo(url)).andRespond(withStatus(MOVED_PERMANENTLY));

        exception.expect(Failure.class);
        exception.expect(hasFeature("status", Failure::getStatus, equalTo(MOVED_PERMANENTLY)));

        perform(Void.class);
    }
    
    @Test
    public void shouldDispatchLevelTwo() throws IOException {
        server.expect(requestTo(url)).andRespond(
                withStatus(CREATED)
                        .body(new ClassPathResource("success.json"))
                        .contentType(APPLICATION_JSON));
        
        final Success success = perform(Success.class);

        assertThat(success.isHappy(), is(true));
    }
    
    @Test
    public void shouldDispatchLevelThree() throws IOException {
        server.expect(requestTo(url)).andRespond(
                withStatus(UNPROCESSABLE_ENTITY)
                        .body(new ClassPathResource("problem.json"))
                        .contentType(PROBLEM));
        
        final Problem problem = perform(Problem.class);
        
        assertThat(problem.getType(), is(URI.create("http://httpstatus.es/422")));
        assertThat(problem.getTitle(), is("Unprocessable Entity"));
        assertThat(problem.getStatus(), is(422));
        assertThat(problem.getDetail(), is("A problem occurred."));
    }

}
