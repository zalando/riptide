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

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.AsyncClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.web.client.ResourceAccessException;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import static java.util.Collections.emptyList;
import static org.springframework.http.HttpStatus.Series.SUCCESSFUL;
import static org.zalando.riptide.Bindings.on;
import static org.zalando.riptide.Selectors.series;

public class HandlesIOExceptionTest {
    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @Test
    public void shouldHandleExceptionDuringRequestCreation() throws URISyntaxException {

        exception.expect(ResourceAccessException.class);
        exception.expectMessage("I/O error on GET request");

        final ClientHttpRequestFactory factory = (uri, httpMethod) -> {
            throw new IOException("Could not create request");
        };

        Rest.create(factory, emptyList())
                .execute(HttpMethod.GET, new URI("http://localhost/"))
                .dispatch(series(),
                        on(SUCCESSFUL).capture());
    }

    @Test
    public void shouldHandleExceptionDuringAsyncRequestCreation() throws URISyntaxException {

        exception.expect(ResourceAccessException.class);
        exception.expectMessage("I/O error on GET request");

        final AsyncClientHttpRequestFactory factory = (uri, httpMethod) -> {
            throw new IOException("Could not create request");
        };

        AsyncRest.create(factory, emptyList())
                .execute(HttpMethod.GET, new URI("http://localhost/"))
                .dispatch(series(),
                        on(SUCCESSFUL).capture());
    }

}
