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
import java.util.HashMap;
import java.util.Map;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpHeaders;
import org.junit.Test;

import static java.util.Collections.emptyMap;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

public class WithUrlTest {

    private final RestTemplate template;
    private final Rest unit;
    private final MockRestServiceServer server;

    public WithUrlTest() {
        this.template = new RestTemplate();
        this.server = MockRestServiceServer.createServer(template);
        this.unit = Rest.create(template);
    }

    @Test
    public void shouldExpandWithoutVariables() {
        expectRequestTo("https://api.example.com/123");

        unit.withUrl("https://api.example.com/123")
            .execute(GET);
    }

    @Test
    public void shouldExpandOne() {
        expectRequestTo("https://api.example.com/123");

        unit.withUrl("https://api.example.com/{id}", 123)
            .execute(GET);
    }

    @Test
    public void shouldExpandTwo() {
        expectRequestTo("https://api.example.com/123/456");

        unit.withUrl("https://api.example.com/{parent}/{child}", 123, "456")
            .execute(GET);
    }

    @Test
    public void shouldExpandWithEmptyMap() {
        expectRequestTo("https://api.example.com/");

        unit.withUrl("https://api.example.com/", emptyMap())
            .execute(GET);
    }

    @Test
    public void shouldExpandMap() {
        expectRequestTo("https://api.example.com/123/456");

        final Map<String, Object> m = new HashMap<>();
        m.put("parent", 123);
        m.put("child", "456");

        unit.withUrl("https://api.example.com/{parent}/{child}", m)
            .execute(GET);
    }

    @Test
    public void shouldMakeCoverageHappy() {
        for (int i = 0; i < 4; i++) {
            expectRequestTo("https://api.example.com");
        }

        final RestWithURL<Dispatcher> withUrl = unit.withUrl("https://api.example.com");

        withUrl.execute(GET);
        withUrl.execute(GET, new HttpHeaders());
        withUrl.execute(GET, "deadbody");
        withUrl.execute(GET, new HttpHeaders(), "deadbody");
    }

    private void expectRequestTo(final String url) {
        server.expect(requestTo(url))
            .andRespond(withSuccess());
    }
}
