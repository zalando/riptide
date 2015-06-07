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
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.URI;

import static java.util.Collections.singletonList;
import static java.util.function.Function.identity;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpStatus.ACCEPTED;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.Series.CLIENT_ERROR;
import static org.springframework.http.HttpStatus.Series.SUCCESSFUL;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;
import static org.zalando.riptide.Conditions.anyContentType;
import static org.zalando.riptide.Conditions.anySeries;
import static org.zalando.riptide.Conditions.anyStatus;
import static org.zalando.riptide.Conditions.on;
import static org.zalando.riptide.MediaTypes.ERROR;
import static org.zalando.riptide.MediaTypes.PROBLEM;
import static org.zalando.riptide.Selectors.contentType;
import static org.zalando.riptide.Selectors.series;
import static org.zalando.riptide.Selectors.status;

public final class NestedDispatchTest {

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

    private <T> T perform(Class<T> type) {
        return unit.execute(GET, URI.create("https://api.example.com"))
                .dispatch(series(),
                        on(SUCCESSFUL)
                                .dispatch(status(),
                                        on(CREATED, Success.class).capture(),
                                        on(ACCEPTED, Success.class).capture(),
                                        anyStatus().call(this::fail)),
                        on(CLIENT_ERROR)
                            .dispatch(status(),
                                    on(UNAUTHORIZED).map(identity()).capture(),
                                    anyStatus()
                                            .dispatch(contentType(),
                                                    on(PROBLEM, Problem.class).capture(),
                                                    on(ERROR, Problem.class).capture(),
                                                    anyContentType().call(this::fail))),
                        anySeries().call(this::fail))
                .retrieve(type).orElse(null);
    }
    
    private void fail(ClientHttpResponse response) {
        try {
            throw new AssertionError(response.getRawStatusCode());
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }
    
    @Test
    public void shouldDispatch() {
        
    }

}
