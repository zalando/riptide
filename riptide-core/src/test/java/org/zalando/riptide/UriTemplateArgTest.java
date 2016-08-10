package org.zalando.riptide;

/*
 * ⁣​
 * Riptide Core
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

import static org.springframework.http.HttpStatus.Series.SUCCESSFUL;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.zalando.riptide.Bindings.on;
import static org.zalando.riptide.Navigators.series;
import static org.zalando.riptide.Route.pass;

import java.io.IOException;
import java.util.Arrays;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.client.MockRestServiceServer;

@RunWith(Parameterized.class)
public class UriTemplateArgTest {

    private final Rest unit;
    private final MockRestServiceServer server;

    private final String requestUrl;
    private final String uriTemplate;
    private final Object[] uriVariables;

    public UriTemplateArgTest(final String baseUrl, final String requestUrl,
            final String uriTemplate, final Object... uriVariables) {
        final MockSetup setup = new MockSetup(baseUrl, null);
        this.unit = setup.getRest();
        this.server = setup.getServer();

        this.requestUrl = requestUrl;
        this.uriTemplate = uriTemplate;
        this.uriVariables = uriVariables;
    }

    @Parameterized.Parameters(name = "{1}")
    public static Iterable<Object[]> data() {
        return Arrays.asList(new Object[][] {
                { null, "/pages/0", "/pages/{page}", new Object[] { 0 } },
                { null, "../pages/1", "../pages/{page}", new Object[] { 1 } },
                { null, "https://api.example.com/pages/2", "https://api.example.com/pages/{page}",
                        new Object[] { 2 } },
                { "https://api.example.org/", "https://api.example.org/pages/3", "/pages/{page}",
                        new Object[] { 3 } },
                { "https://api.example.org/", "https://api.example.com/pages/4", "https://api.example.com/pages/{page}",
                        new Object[] { 4 } },
                { "https://api.example.org/books/", "https://api.example.org/books/pages/5", "./pages/{page}",
                        new Object[] { 5 } },
                { "https://api.example.org/books/", "https://api.example.org/pages/6", "../pages/{page}",
                        new Object[] { 6 } }
        });
    }

    @Before
    public void setUp() {
        server.expect(requestTo(requestUrl))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess());
    }

    @After
    public void tearDown() {
        server.verify();
    }

    @Test
    public void shouldExpand() throws IOException {
        this.unit.get(uriTemplate, uriVariables)
                .dispatch(series(),
                        on(SUCCESSFUL).call(pass()));
    }

}
