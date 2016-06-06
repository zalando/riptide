package org.zalando.riptide;

/*
 * ⁣​
 * Riptide
 * ⁣⁣
 * Copyright (C) 2015 - 2016 Zalando SE
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
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.springframework.http.HttpMethod;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.Arrays;

import static java.util.Collections.singletonList;
import static org.springframework.http.HttpStatus.Series.SUCCESSFUL;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.zalando.riptide.Bindings.on;
import static org.zalando.riptide.Selectors.series;

@RunWith(Parameterized.class)
public class UriTest {

    private final Rest unit;
    private final MockRestServiceServer server;

    private final HttpMethod method;
    private final Executor<Capture> executor;

    public UriTest(final HttpMethod method, final Executor<Capture> executor) {
        final RestTemplate template = new RestTemplate();
        template.setMessageConverters(singletonList(new MappingJackson2HttpMessageConverter(
                new ObjectMapper().findAndRegisterModules())));

        this.server = MockRestServiceServer.createServer(template);
        this.unit = Rest.create(template);

        this.method = method;
        this.executor = executor;
    }

    interface Executor<R> {
        Requester<R> execute(RestClient<R> client, URI uri);
    }

    @Parameterized.Parameters(name= "{0}")
    public static Iterable<Object[]> data() {
        return Arrays.asList(new Object[][] {
                {HttpMethod.GET, (Executor) RestClient::get},
                {HttpMethod.HEAD, (Executor) RestClient::head},
                {HttpMethod.POST, (Executor) RestClient::post},
                {HttpMethod.PUT, (Executor) RestClient::put},
                {HttpMethod.PATCH, (Executor) RestClient::patch},
                {HttpMethod.DELETE, (Executor) RestClient::delete},
                {HttpMethod.OPTIONS, (Executor) RestClient::options},
                {HttpMethod.TRACE, (Executor) RestClient::trace},
        });
    }

    @Before
    public void setUp() {
        server.expect(requestTo("https://api.example.org/pages/123"))
                .andExpect(method(method))
                .andRespond(withSuccess());
    }

    @After
    public void tearDown() {
        server.verify();
    }

    @Test
    public void shouldExpand() {
        executor.execute(unit, URI.create("https://api.example.org/pages/123"))
                .dispatch(series(),
                        on(SUCCESSFUL).capture());
    }

}
