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
    public void shouldExpandQueryparams() {
        expectRequestTo("https://example.com/posts/123?filter=new");

        final int postId = 123;
        final String filter = "new";

        unit.withUrl("https://example.com/posts/{id}?filter={filter}", postId, filter)
            .execute(GET);
    }

    @Test
    public void shouldEncodePath() {
        expectRequestTo("https://ru.wikipedia.org/wiki/%D0%9E%D1%82%D0%B1%D0%BE%D0%B9%D0%BD%D0%BE%D0%B5_%D1%82%D0%B5%D1%87%D0%B5%D0%BD%D0%B8%D0%B5");

        unit.withUrl("https://ru.wikipedia.org/wiki/{article-name}", "Отбойное_течение")
            .execute(GET);
    }

    @Test
    public void shouldEncodeQueraparams() {
        expectRequestTo("https://ru.wiktionary.org/w/index.php?title=%D0%A1%D0%BB%D1%83%D0%B6%D0%B5%D0%B1%D0%BD%D0%B0%D1%8F:%D0%9A%D0%BE%D0%BB%D0%BB%D0%B5%D0%BA%D1%86%D0%B8%D1%8F_%D0%BA%D0%BD%D0%B8%D0%B3&bookcmd=book_creator&referer=%D0%97%D0%B0%D0%B3%D0%BB%D0%B0%D0%B2%D0%BD%D0%B0%D1%8F%20%D1%81%D1%82%D1%80%D0%B0%D0%BD%D0%B8%D1%86%D0%B0");

        unit.withUrl("https://ru.wiktionary.org/w/index.php?title={title}&bookcmd=book_creator&referer={referer}", "Служебная:Коллекция_книг", "Заглавная страница")
            .execute(GET);
    }

    @Test
    public void shouldExpandOnGetWithHeaders() {
        expectRequestTo("https://api.example.com/123");

        unit.withUrl("https://api.example.com/123")
            .execute(GET, new HttpHeaders());
    }

    @Test
    public void shouldExpandOnGetWithBody() {
        expectRequestTo("https://api.example.com/123");

        unit.withUrl("https://api.example.com/123")
            .execute(GET, "deadbody");
    }

    @Test
    public void shouldExpandOnGetWithHeadersAndBody() {
        expectRequestTo("https://api.example.com/123");

        unit.withUrl("https://api.example.com/123")
            .execute(GET, new HttpHeaders(), "deadbody");
    }

    private void expectRequestTo(final String url) {
        server.expect(requestTo(url))
            .andRespond(withSuccess());
    }
}
