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

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.mock.http.client.MockClientHttpResponse;
import org.springframework.web.client.RestClientException;

import java.io.IOException;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.instanceOf;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.OK;
import static org.zalando.riptide.Binding.create;
import static org.zalando.riptide.Conditions.anyStatus;
import static org.zalando.riptide.Conditions.on;
import static org.zalando.riptide.Selectors.status;

public class RouterTest {

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @Test
    public void shouldRejectDuplicateWildcards() {
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("Multiple wildcard entries");

        Router.create(status(), asList(
                anyStatus().capture(),
                anyStatus().capture()))
                .route(new MockClientHttpResponse((byte[]) null, OK), emptyList());
    }

    @Test
    public void shouldRejectDuplicateAttributes() {
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("Multiple entries with same key: 200");

        Router.create(status(), asList(
                on(OK).capture(),
                on(OK).capture()))
                .route(new MockClientHttpResponse((byte[]) null, OK), emptyList());
    }

    @Test
    public void shouldCatchIOExceptionFromResponse() throws IOException {
        exception.expect(RestClientException.class);
        exception.expectCause(instanceOf(IOException.class));

        final ClientHttpResponse response = mock(ClientHttpResponse.class);
        when(response.getStatusCode()).thenThrow(new IOException());

        Router.create(status(), singletonList(anyStatus().capture()))
                .route(response, emptyList());
    }

    @Test
    public void shouldCatchIOExceptionFromBinding() throws IOException {
        exception.expect(RestClientException.class);
        exception.expectCause(instanceOf(IOException.class));

        final HttpStatus anyStatus = null;
        final Binding<HttpStatus> binding = create(anyStatus, (response, converters) -> {
            throw new IOException();
        });

        final ClientHttpResponse response = mock(ClientHttpResponse.class);
        when(response.getStatusCode()).thenReturn(OK);

        Router.create(status(), singletonList(binding))
                .route(response, emptyList());
    }
}