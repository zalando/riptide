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
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.AsyncRestTemplate;

import java.net.URI;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpStatus.Series.CLIENT_ERROR;
import static org.springframework.http.HttpStatus.Series.SUCCESSFUL;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.zalando.riptide.Actions.pass;
import static org.zalando.riptide.Conditions.on;
import static org.zalando.riptide.Selectors.series;

public final class AsyncTest {

    private final URI url = URI.create("http://localhost");

    private final AsyncRest unit;
    private final MockRestServiceServer server;

    public AsyncTest() {
        final AsyncRestTemplate template = new AsyncRestTemplate();
        template.setErrorHandler(new PassThroughResponseErrorHandler());
        this.unit = AsyncRest.create(template);
        this.server = MockRestServiceServer.createServer(template);
    }

    @Test
    public void shouldCall() {
        server.expect(requestTo(url)).andRespond(withSuccess());

        @SuppressWarnings("unchecked")
        final ThrowingConsumer<ClientHttpResponse, RuntimeException> verifier = mock(ThrowingConsumer.class);

        unit.execute(GET, url).dispatch(series(),
                on(SUCCESSFUL).call(verifier));

        verify(verifier).accept(any());
    }

    @Test
    public void shouldCallWithHeaders() {
        server.expect(requestTo(url)).andRespond(withSuccess());

        @SuppressWarnings("unchecked")
        final ThrowingConsumer<ClientHttpResponse, RuntimeException> verifier = mock(ThrowingConsumer.class);

        unit.execute(GET, url, new HttpHeaders()).dispatch(series(),
                on(SUCCESSFUL).call(verifier));

        verify(verifier).accept(any());
    }

    @Test
    public void shouldCallWithBody() {
        server.expect(requestTo(url)).andRespond(withSuccess());

        @SuppressWarnings("unchecked")
        final ThrowingConsumer<ClientHttpResponse, RuntimeException> verifier = mock(ThrowingConsumer.class);

        unit.execute(GET, url, "test").dispatch(series(),
                on(SUCCESSFUL).call(verifier));

        verify(verifier).accept(any());
    }

    @Test
    public void shouldCallWithHeadersAndBody() {
        server.expect(requestTo(url)).andRespond(withSuccess());

        @SuppressWarnings("unchecked")
        final ThrowingConsumer<ClientHttpResponse, RuntimeException> verifier = mock(ThrowingConsumer.class);

        unit.execute(GET, url, new HttpHeaders(), "test").dispatch(series(),
                on(SUCCESSFUL).call(verifier));

        verify(verifier).accept(any());
    }
    
    @Test
    public void shouldIgnoreNoRouteException() {
        server.expect(requestTo(url)).andRespond(withSuccess());
        
        unit.execute(GET, url).dispatch(series(),
                on(CLIENT_ERROR).call(pass()));
    }

}
