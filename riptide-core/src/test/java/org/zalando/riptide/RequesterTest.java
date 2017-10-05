package org.zalando.riptide;

import com.google.common.collect.ImmutableMultimap;
import org.junit.After;
import org.junit.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.client.MockRestServiceServer;

import static org.springframework.http.HttpStatus.Series.SUCCESSFUL;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.zalando.riptide.Bindings.on;
import static org.zalando.riptide.Navigators.contentType;
import static org.zalando.riptide.Navigators.series;
import static org.zalando.riptide.PassRoute.pass;

public class RequesterTest {

    private final Http unit;
    private final MockRestServiceServer server;

    public RequesterTest() {
        final MockSetup setup = new MockSetup();
        this.unit = setup.getHttp();
        this.server = setup.getServer();
    }

    @After
    public void after() {
        server.verify();
    }

    @Test
    public void shouldExpandWithoutVariables() {
        expectRequestTo("https://api.example.com/123");

        unit.get("/123")
                .dispatch(series(),
                        on(SUCCESSFUL).call(pass()));
    }

    @Test
    public void shouldExpandOne() {
        expectRequestTo("https://api.example.com/123");

        unit.get("/{id}", 123)
                .dispatch(series(),
                        on(SUCCESSFUL).call(pass()));
    }

    @Test
    public void shouldExpandTwo() {
        expectRequestTo("https://api.example.com/123/456");

        unit.get("/{parent}/{child}", 123, "456")
                .dispatch(series(),
                        on(SUCCESSFUL).call(pass()));
    }

    @Test
    public void shouldExpandInlinedQueryParams() {
        expectRequestTo("https://example.com/posts/123?filter=new");

        final int postId = 123;
        final String filter = "new";

        unit.get("https://example.com/posts/{id}?filter={filter}", postId, filter)
                .dispatch(series(),
                        on(SUCCESSFUL).call(pass()));
    }

    @Test
    public void shouldEncodePath() {
        expectRequestTo("https://ru.wikipedia.org/wiki/%D0%9E%D1%82%D0%B1%D0%BE%D0%B9%D0%BD%D0%BE%D0%B5_%D1%82%D0%B5%D1%87%D0%B5%D0%BD%D0%B8%D0%B5");

        unit.get("https://ru.wikipedia.org/wiki/{article-name}", "Отбойное_течение")
                .dispatch(series(),
                        on(SUCCESSFUL).call(pass()));
    }

    @Test
    public void shouldEncodeInlinedQueryParams() {
        expectRequestTo("https://ru.wiktionary.org/w/index.php?title=%D0%A1%D0%BB%D1%83%D0%B6%D0%B5%D0%B1%D0%BD%D0%B0%D1%8F:%D0%9A%D0%BE%D0%BB%D0%BB%D0%B5%D0%BA%D1%86%D0%B8%D1%8F_%D0%BA%D0%BD%D0%B8%D0%B3&bookcmd=book_creator&referer=%D0%97%D0%B0%D0%B3%D0%BB%D0%B0%D0%B2%D0%BD%D0%B0%D1%8F%20%D1%81%D1%82%D1%80%D0%B0%D0%BD%D0%B8%D1%86%D0%B0");

        unit.get("https://ru.wiktionary.org/w/index.php?title={title}&bookcmd=book_creator&referer={referer}", "Служебная:Коллекция_книг", "Заглавная страница")
                .dispatch(series(),
                        on(SUCCESSFUL).call(pass()));
    }

    @Test
    public void shouldAppendQueryParams() {
        server.expect(requestTo("https://api.example.com?foo=bar&foo=baz&bar=null"))
                .andRespond(withSuccess());

        unit.head("https://api.example.com")
                .queryParam("foo", "bar")
                .queryParams(ImmutableMultimap.of(
                        "foo", "baz",
                        "bar", "null"
                ))
                .dispatch(series(),
                        on(SUCCESSFUL).call(pass()));
    }

    @Test
    public void shouldEncodeAppendedQueryParams() {
        expectRequestTo("https://ru.wiktionary.org/w/index.php?title=%D0%A1%D0%BB%D1%83%D0%B6%D0%B5%D0%B1%D0%BD%D0%B0%D1%8F:%D0%9A%D0%BE%D0%BB%D0%BB%D0%B5%D0%BA%D1%86%D0%B8%D1%8F_%D0%BA%D0%BD%D0%B8%D0%B3&bookcmd=book_creator&referer=%D0%97%D0%B0%D0%B3%D0%BB%D0%B0%D0%B2%D0%BD%D0%B0%D1%8F%20%D1%81%D1%82%D1%80%D0%B0%D0%BD%D0%B8%D1%86%D0%B0");

        unit.head("https://ru.wiktionary.org/w/index.php")
                .queryParam("title", "Служебная:Коллекция_книг")
                .queryParam("bookcmd", "book_creator")
                .queryParam("referer", "Заглавная страница")
                .dispatch(series(),
                        on(SUCCESSFUL).call(pass()));
    }

    @Test
    public void shouldExpandOnGetWithHeaders() {
        expectRequestTo("https://api.example.com/123");

        unit.get("/123")
                .headers(new HttpHeaders())
                .dispatch(series(),
                        on(SUCCESSFUL).call(pass()));
    }

    @Test
    public void shouldExpandOnGetWithBody() {
        expectRequestTo("https://api.example.com/123");

        unit.get("/123")
                .body("deadbody")
                .dispatch(contentType());
    }

    @Test
    public void shouldExpandOnGetWithHeadersAndBody() {
        expectRequestTo("https://api.example.com/123");

        unit.get("/123")
                .headers(new HttpHeaders())
                .body("deadbody")
                .dispatch(contentType());
    }

    private void expectRequestTo(final String url) {
        server.expect(requestTo(url))
                .andRespond(withSuccess());
    }

}
