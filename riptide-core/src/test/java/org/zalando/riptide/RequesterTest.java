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

import org.junit.After;
import org.junit.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.client.MockRestServiceServer;

import java.io.IOException;

import static org.springframework.http.HttpStatus.Series.SUCCESSFUL;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.zalando.riptide.Bindings.on;
import static org.zalando.riptide.Navigators.series;
import static org.zalando.riptide.Route.pass;

public class RequesterTest {

    private final Rest unit;
    private final MockRestServiceServer server;

    public RequesterTest() {
        final MockSetup setup = new MockSetup();
        this.unit = setup.getRest();
        this.server = setup.getServer();
    }

    @After
    public void after() {
        server.verify();
    }

    @Test
    public void shouldExpandWithoutVariables() throws IOException {
        expectRequestTo("https://api.example.com/123");

        unit.get("/123")
                .dispatch(series(),
                        on(SUCCESSFUL).call(pass()));
    }

    @Test
    public void shouldExpandOne() throws IOException {
        expectRequestTo("https://api.example.com/123");

        unit.get("/{id}", 123)
                .dispatch(series(),
                        on(SUCCESSFUL).call(pass()));
    }

    @Test
    public void shouldExpandTwo() throws IOException {
        expectRequestTo("https://api.example.com/123/456");

        unit.get("/{parent}/{child}", 123, "456")
                .dispatch(series(),
                        on(SUCCESSFUL).call(pass()));
    }

    @Test
    public void shouldExpandQueryparams() throws IOException {
        expectRequestTo("https://example.com/posts/123?filter=new");

        final int postId = 123;
        final String filter = "new";

        unit.get("https://example.com/posts/{id}?filter={filter}", postId, filter)
                .dispatch(series(),
                        on(SUCCESSFUL).call(pass()));
    }

    @Test
    public void shouldEncodePath() throws IOException {
        expectRequestTo("https://ru.wikipedia.org/wiki/%D0%9E%D1%82%D0%B1%D0%BE%D0%B9%D0%BD%D0%BE%D0%B5_%D1%82%D0%B5%D1%87%D0%B5%D0%BD%D0%B8%D0%B5");

        unit.get("https://ru.wikipedia.org/wiki/{article-name}", "Отбойное_течение")
                .dispatch(series(),
                        on(SUCCESSFUL).call(pass()));
    }

    @Test
    public void shouldEncodeQueraparams() throws IOException {
        expectRequestTo("https://ru.wiktionary.org/w/index.php?title=%D0%A1%D0%BB%D1%83%D0%B6%D0%B5%D0%B1%D0%BD%D0%B0%D1%8F:%D0%9A%D0%BE%D0%BB%D0%BB%D0%B5%D0%BA%D1%86%D0%B8%D1%8F_%D0%BA%D0%BD%D0%B8%D0%B3&bookcmd=book_creator&referer=%D0%97%D0%B0%D0%B3%D0%BB%D0%B0%D0%B2%D0%BD%D0%B0%D1%8F%20%D1%81%D1%82%D1%80%D0%B0%D0%BD%D0%B8%D1%86%D0%B0");

        unit.get("https://ru.wiktionary.org/w/index.php?title={title}&bookcmd=book_creator&referer={referer}", "Служебная:Коллекция_книг", "Заглавная страница")
                .dispatch(series(),
                        on(SUCCESSFUL).call(pass()));
    }

    @Test
    public void shouldExpandOnGetWithHeaders() throws IOException {
        expectRequestTo("https://api.example.com/123");

        unit.get("/123")
                .headers(new HttpHeaders())
                .dispatch(series(),
                        on(SUCCESSFUL).call(pass()));
    }

    @Test
    public void shouldExpandOnGetWithBody() throws IOException {
        expectRequestTo("https://api.example.com/123");

        unit.get("/123")
                .body("deadbody");
    }

    @Test
    public void shouldExpandOnGetWithHeadersAndBody() throws IOException {
        expectRequestTo("https://api.example.com/123");

        unit.get("/123")
                .headers(new HttpHeaders())
                .body("deadbody");
    }

    private void expectRequestTo(final String url) {
        server.expect(requestTo(url))
                .andRespond(withSuccess());
    }

}
