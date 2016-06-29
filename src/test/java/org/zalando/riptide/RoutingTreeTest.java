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

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.mock.http.client.MockClientHttpResponse;

import java.io.IOException;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.OK;
import static org.zalando.riptide.Routes.pass;
import static org.zalando.riptide.Binding.create;
import static org.zalando.riptide.Bindings.anyStatus;
import static org.zalando.riptide.Bindings.on;
import static org.zalando.riptide.Navigators.status;

public class RoutingTreeTest {

    @Rule
    public final ExpectedException exception = ExpectedException.none();
    
    private final MessageReader reader = mock(MessageReader.class);

    @Test
    public void shouldUsedAttributeRoute() throws Exception {
        final Capture other = Capture.none();
        final Capture expected = Capture.none();
        final Capture actual = RoutingTree.create(status(),
                Binding.create(OK, (u, v) -> expected),
                Binding.create(null, (u, v) -> other))
                .execute(response(OK), reader);

        Assert.assertEquals(expected, actual);
    }

    @Test
    public void shouldUsedWildcardRoute() throws Exception {
        final Capture other = Capture.none();
        final Capture expected = Capture.none();
        final Capture actual = RoutingTree.create(status(),
                Binding.create(OK, (u, v) -> other),
                Binding.create(null, (u, v) -> expected))
                .execute(response(CREATED), reader);

        Assert.assertEquals(expected, actual);
    }

    @Test
    public void shouldUsedAddedAttributeRoute() throws Exception {
        final Capture other = Capture.none();
        final Capture expected = Capture.none();
        final Capture actual = RoutingTree.create(status(),
                Binding.create(null, (u, v) -> other))
                .merge(Binding.create(OK, (u, v) -> expected))
                .execute(response(OK), reader);

        Assert.assertEquals(expected, actual);
    }

    @Test
    public void shouldUsedAddedWildcardRoute() throws Exception {
        final Capture other = Capture.none();
        final Capture expected = Capture.none();
        final Capture actual = RoutingTree.create(status(),
                Binding.create(OK, (u, v) -> other))
                .merge(Binding.create(null, (u, v) -> expected))
                .execute(response(CREATED), reader);

        Assert.assertEquals(expected, actual);
    }

    @Test
    public void shouldUseLastWildcardRoute() throws Exception {
        final Capture other = Capture.none();
        final Capture expected = Capture.none();
        final Capture actual = RoutingTree.create(status(),
                Binding.create((HttpStatus) null, (u, v) -> other))
                .merge(Binding.create((HttpStatus) null, (u, v) -> expected))
                .execute(response(OK), reader);

        Assert.assertEquals(expected, actual);
    }

    @Test
    public void shouldUseLastAttributeRoute() throws Exception {
        final Capture other = Capture.none();
        final Capture expected = Capture.none();
        final Capture actual = RoutingTree.create(status(),
                Binding.create(OK, (u, v) -> other))
                .merge(Binding.create(OK, (u, v) -> expected))
                .execute(response(OK), reader);

        Assert.assertEquals(expected, actual);
    }

    @Test
    public void shouldUseLastAddedAttributeRoute() throws Exception {
        final Capture other = Capture.none();
        final Capture expected = Capture.none();
        final Capture actual = RoutingTree.create(status(),
                Binding.create(OK, (u, v) -> other),
                Binding.create(null, (u, v) -> other))
                .merge(Binding.create(OK, (u, v) -> other))
                .merge(Binding.create(OK, (u, v) -> expected))
                .execute(response(OK), reader);

        Assert.assertEquals(expected, actual);
    }

    @Test
    public void shouldUseLastAddedWildcardeRoute() throws Exception {
        final Capture other = Capture.none();
        final Capture expected = Capture.none();
        final Capture actual = RoutingTree.create(status(),
                Binding.create(OK, (u, v) -> other),
                Binding.create(null, (u, v) -> other))
                .merge(asList(Binding.create(null, (u, v) -> other),
                        Binding.create(null, (u, v) -> expected)))
                .execute(response(CREATED), reader);

        Assert.assertEquals(expected, actual);
    }

    @Test
    public void shouldCreateNewRoutingTreeIfChanged() {
        final RoutingTree<HttpStatus> tree = RoutingTree.create(status(), on(OK).capture());
        final RoutingTree<HttpStatus> result = tree.merge(anyStatus().capture());
        Assert.assertNotEquals(tree, result);
    }

    @Test
    public void shouldCreateNewRoutingTreeIfNotChanged() {
        final RoutingTree<HttpStatus> tree = RoutingTree.create(status(), on(OK).capture());
        final RoutingTree<HttpStatus> result = tree.merge(on(OK).capture());
        Assert.assertNotEquals(tree, result);
    }

    @Test
    public void shouldCatchIOExceptionFromResponse() throws Exception {
        exception.expect(IOException.class);

        final ClientHttpResponse response = mock(ClientHttpResponse.class);
        when(response.getStatusCode()).thenThrow(new IOException());

        RoutingTree.create(status(), singletonList(anyStatus().capture()))
                .execute(response, reader);
    }

    @Test
    public void shouldCatchIOExceptionFromBinding() throws Exception {
        exception.expect(IOException.class);

        final HttpStatus anyStatus = null;
        final Binding<HttpStatus> binding = create(anyStatus, (response, converters) -> {
            throw new IOException();
        });

        final ClientHttpResponse response = mock(ClientHttpResponse.class);
        when(response.getStatusCode()).thenReturn(OK);

        RoutingTree.create(status(), singletonList(binding))
                .execute(response, reader);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldFailForDuplicateBindings() {
        RoutingTree.create(status(),
                on(OK).capture(),
                on(OK).call(pass()));
    }

    private MockClientHttpResponse response(final HttpStatus status) {
        return new MockClientHttpResponse((byte[]) null, status);
    }

}
