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
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.net.URI;

import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hobsoft.hamcrest.compose.ComposeMatchers.hasFeature;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.zalando.riptide.Conditions.on;
import static org.zalando.riptide.MediaTypes.ERROR;
import static org.zalando.riptide.MediaTypes.PROBLEM;
import static org.zalando.riptide.MediaTypes.SUCCESS;
import static org.zalando.riptide.Selectors.contentType;

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

}
