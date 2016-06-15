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

import org.junit.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.net.URI;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.zalando.riptide.Bindings.on;
import static org.zalando.riptide.Selectors.isCurrentRepresentation;

public final class CurrentRepresentationNavigatorTest {

    private final URI url = URI.create("https://api.example.com");

    private final Rest unit;
    private final MockRestServiceServer server;

    public CurrentRepresentationNavigatorTest() {
        final RestTemplate template = new RestTemplate();
        this.server = MockRestServiceServer.createServer(template);
        this.unit = Rest.create(template);
    }

    @Test
    public void shouldMatchOnSameHeader() {
        final HttpHeaders headers = new HttpHeaders();
        headers.set("Location", "/foo");
        headers.set("Content-Location", "/foo");

        server.expect(requestTo(url))
                .andRespond(withSuccess()
                        .headers(headers));

        unit.get(url).dispatch(isCurrentRepresentation(),
                on(true).capture(),
                on(false).call(() -> {throw new AssertionError();}));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void shouldNotMatchOnDifferentHeaders() {
        final HttpHeaders headers = new HttpHeaders();
        headers.set("Location", "/foo");
        headers.set("Content-Location", "/bar");

        server.expect(requestTo(url))
                .andRespond(withSuccess()
                        .headers(headers));

        unit.get(url).dispatch(isCurrentRepresentation(),
                on(true).capture(),
                on(false).call(() -> {throw new UnsupportedOperationException();}));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void shouldNotMatchOnMissingHeaders() {
        server.expect(requestTo(url))
                .andRespond(withSuccess());

        unit.get(url).dispatch(isCurrentRepresentation(),
                on(true).capture(),
                on(false).call(() -> {throw new UnsupportedOperationException();}));
    }

}
