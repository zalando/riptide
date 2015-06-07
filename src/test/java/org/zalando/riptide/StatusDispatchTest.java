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

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.URI;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpStatus.MOVED_TEMPORARILY;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.zalando.riptide.Conditions.on;
import static org.zalando.riptide.Selectors.status;


@RunWith(Parameterized.class)
public final class StatusDispatchTest {

    private final URI url = URI.create("https://api.example.com");

    private final Rest unit;
    private final MockRestServiceServer server;

    private final HttpStatus status;

    public StatusDispatchTest(HttpStatus status) {
        this.status = status;
        final RestTemplate template = new RestTemplate();
        template.setErrorHandler(new PassThroughResponseErrorHandler());
        this.server = MockRestServiceServer.createServer(template);
        this.unit = Rest.create(template);
    }

    @Parameterized.Parameters(name = "{0}")
    public static Iterable<Object[]> data() {
        return Stream.of(HttpStatus.values())
                .filter(s -> s != MOVED_TEMPORARILY) // duplicate with FOUND
                .map(s -> new Object[]{s})
                .collect(toList());
    }

    @Test
    @SuppressWarnings("deprecation")
    public void shouldDispatch() {
        server.expect(requestTo(url)).andRespond(withStatus(status));

        final Consumer<ClientHttpResponse> verifier = response -> {
            try {
                assertThat(response.getStatusCode(), is(status));
            } catch (IOException e) {
                throw new AssertionError(e);
            }
        };
        
        @SuppressWarnings("unchecked")
        final Binding<HttpStatus>[] bindings = Stream.of(HttpStatus.values())
                .map(status -> on(status).call(verifier))
                .toArray(Binding[]::new);

        unit.execute(GET, url).dispatch(status(), bindings);
    }

}
