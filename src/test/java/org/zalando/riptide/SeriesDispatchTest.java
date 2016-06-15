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

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.springframework.http.HttpStatus.Series.CLIENT_ERROR;
import static org.springframework.http.HttpStatus.Series.INFORMATIONAL;
import static org.springframework.http.HttpStatus.Series.REDIRECTION;
import static org.springframework.http.HttpStatus.Series.SERVER_ERROR;
import static org.springframework.http.HttpStatus.Series.SUCCESSFUL;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.zalando.riptide.Bindings.on;
import static org.zalando.riptide.Selectors.series;

@RunWith(Parameterized.class)
public final class SeriesDispatchTest {

    private final URI url = URI.create("https://api.example.com");

    private final Rest unit;
    private final MockRestServiceServer server;

    private final HttpStatus expected;

    public SeriesDispatchTest(final HttpStatus expected) {
        this.expected = expected;
        final RestTemplate template = new RestTemplate();
        this.server = MockRestServiceServer.createServer(template);
        this.unit = Rest.create(template);
    }

    @Parameterized.Parameters(name = "{0}")
    public static Iterable<Object[]> data() {
        return Stream.of(HttpStatus.Series.values())
                .map(series -> Stream.of(HttpStatus.values())
                        .filter(status -> status.series() == series)
                        .findFirst()
                        .get())
                .map(status -> new Object[]{status})
                .collect(toList());
    }

    @Test
    public void shouldDispatch() {
        server.expect(requestTo(url)).andRespond(withStatus(expected));

        final ClientHttpResponseConsumer verifier = response ->
                assertThat(response.getStatusCode().series(), is(expected.series()));

        unit.get(url)
                .dispatch(series(),
                        on(INFORMATIONAL).call(verifier),
                        on(SUCCESSFUL).call(verifier),
                        on(REDIRECTION).call(verifier),
                        on(CLIENT_ERROR).call(verifier),
                        on(SERVER_ERROR).call(verifier));
    }

}
