package org.example.application;

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
import org.junit.Test;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;
import org.zalando.riptide.PassThroughResponseErrorHandler;
import org.zalando.riptide.Rest;
import org.zalando.riptide.model.Problem;
import org.zalando.riptide.model.Success;

import java.net.URI;

import static java.util.Collections.singletonList;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpStatus.ACCEPTED;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.Series.CLIENT_ERROR;
import static org.springframework.http.HttpStatus.Series.SERVER_ERROR;
import static org.springframework.http.HttpStatus.Series.SUCCESSFUL;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;
import static org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.zalando.riptide.Conditions.anyContentType;
import static org.zalando.riptide.Conditions.anySeries;
import static org.zalando.riptide.Conditions.anyStatus;
import static org.zalando.riptide.Conditions.anyStatusCode;
import static org.zalando.riptide.Conditions.on;
import static org.zalando.riptide.Selectors.contentType;
import static org.zalando.riptide.Selectors.series;
import static org.zalando.riptide.Selectors.status;
import static org.zalando.riptide.Selectors.statusCode;
import static org.zalando.riptide.model.MediaTypes.ERROR;
import static org.zalando.riptide.model.MediaTypes.PROBLEM;

/**
 * Simple sanity check to see if the API of riptide is actually public and accessible.
 */
public class ApiIntegrationTest {

    private final URI url = URI.create("http://localhost");

    private final MockRestServiceServer server;
    private final Rest unit;

    public ApiIntegrationTest() {
        final RestTemplate template = new RestTemplate();
        final MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
        converter.setObjectMapper(new ObjectMapper().findAndRegisterModules());
        template.setMessageConverters(singletonList(converter));
        template.setErrorHandler(new PassThroughResponseErrorHandler());
        this.server = MockRestServiceServer.createServer(template);
        this.unit = Rest.create(template);
    }

    @Test
    public void shouldAllowSimpleExampleApiUsage() {
        server.expect(requestTo(url)).andRespond(withSuccess());

        unit.execute(GET, url).dispatch(status(),
                on(CREATED).call(this::callback),
                on(ACCEPTED).call(this::callback),
                on(BAD_REQUEST).call(this::callback),
                anyStatus().call(this::callback));
    }

    @Test
    public void shouldAllowNestedExampleApiUsage() {
        server.expect(requestTo(url)).andRespond(withSuccess());

        unit.execute(GET, url)
                .dispatch(series(),
                        on(SUCCESSFUL)
                                .dispatch(status(),
                                        on(CREATED, Success.class).capture(),
                                        on(ACCEPTED, Success.class).capture(),
                                        anyStatus().call(this::callback)),
                        on(CLIENT_ERROR)
                                .dispatch(status(),
                                        on(UNAUTHORIZED).call(this::callback),
                                        on(UNPROCESSABLE_ENTITY)
                                                .dispatch(contentType(),
                                                        on(PROBLEM, Problem.class).capture(),
                                                        on(ERROR, Problem.class).capture(),
                                                        anyContentType().call(this::callback)),
                                        anyStatus().call(this::callback)),
                        on(SERVER_ERROR)
                                .dispatch(statusCode(),
                                        on(503).call(this::callback),
                                        anyStatusCode().call(this::callback)),
                        anySeries().call(this::callback))
                .as(Success.class).orElse(null);
    }

    @SuppressWarnings("EmptyMethod")
    private void callback(final ClientHttpResponse response) {
        // no operation
    }

}
