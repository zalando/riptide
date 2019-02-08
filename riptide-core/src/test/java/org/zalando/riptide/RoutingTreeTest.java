package org.zalando.riptide;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.mock.http.client.MockClientHttpResponse;

import java.io.IOException;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.OK;
import static org.zalando.riptide.Binding.create;
import static org.zalando.riptide.Bindings.anyStatus;
import static org.zalando.riptide.Bindings.on;
import static org.zalando.riptide.Navigators.status;
import static org.zalando.riptide.PassRoute.pass;

@ExtendWith(MockitoExtension.class)
final class RoutingTreeTest {

    @Mock
    private Route other;

    @Mock
    private Route expected;

    private final MessageReader reader = mock(MessageReader.class);

    @Test
    void shouldExposeNavigator() {
        final RoutingTree<HttpStatus> unit = RoutingTree.dispatch(status());

        assertThat(unit.getNavigator(), is(status()));
    }

    @Test
    void shouldUsedAttributeRoute() throws Exception {
        RoutingTree.dispatch(status(),
                create(OK, expected),
                create(null, other))
                .execute(response(OK), reader);

        verify(expected).execute(any(), any());
    }

    @Test
    void shouldUsedWildcardRoute() throws Exception {
        RoutingTree.dispatch(status(),
                create(OK, other),
                create(null, expected))
                .execute(response(CREATED), reader);

        verify(expected).execute(any(), any());
    }

    @Test
    void shouldUsedAddedAttributeRoute() throws Exception {
        RoutingTree.dispatch(status(),
                create(null, other))
                .merge(create(OK, expected))
                .execute(response(OK), reader);

        verify(expected).execute(any(), any());
    }

    @Test
    void shouldUsedAddedWildcardRoute() throws Exception {
        RoutingTree.dispatch(status(),
                create(OK, other))
                .merge(create(null, expected))
                .execute(response(CREATED), reader);

        verify(expected).execute(any(), any());
    }

    @Test
    void shouldUseLastWildcardRoute() throws Exception {
        RoutingTree.dispatch(status(),
                create((HttpStatus) null, other))
                .merge(create((HttpStatus) null, expected))
                .execute(response(OK), reader);

        verify(expected).execute(any(), any());
    }

    @Test
    void shouldUseLastAttributeRoute() throws Exception {
        RoutingTree.dispatch(status(),
                create(OK, other))
                .merge(create(OK, expected))
                .execute(response(OK), reader);

        verify(expected).execute(any(), any());
    }

    @Test
    void shouldUseLastAddedAttributeRoute() throws Exception {
        RoutingTree.dispatch(status(),
                create(OK, other),
                create(null, other))
                .merge(create(OK, other))
                .merge(create(OK, expected))
                .execute(response(OK), reader);

        verify(expected).execute(any(), any());
    }

    @Test
    void shouldUseLastAddedWildcardeRoute() throws Exception {
        RoutingTree.dispatch(status(),
                create(OK, other),
                create(null, other))
                .merge(asList(create(null, other),
                        create(null, expected)))
                .execute(response(CREATED), reader);

        verify(expected).execute(any(), any());
    }

    @Test
    void shouldCreateNewRoutingTreeIfChanged() {
        final RoutingTree<HttpStatus> tree = RoutingTree.dispatch(status(), on(OK).call(pass()));
        final RoutingTree<HttpStatus> result = tree.merge(anyStatus().call(pass()));
        assertNotEquals(tree, result);
    }

    @Test
    void shouldCreateNewRoutingTreeIfNotChanged() {
        final RoutingTree<HttpStatus> tree = RoutingTree.dispatch(status(), on(OK).call(pass()));
        final RoutingTree<HttpStatus> result = tree.merge(on(OK).call(pass()));
        assertNotEquals(tree, result);
    }

    @Test
    void shouldCatchIOExceptionFromResponse() throws Exception {
        final ClientHttpResponse response = mock(ClientHttpResponse.class);
        when(response.getStatusCode()).thenThrow(new IOException());

        final RoutingTree<HttpStatus> tree = RoutingTree.dispatch(status(),
                singletonList(anyStatus().call(pass())));

        assertThrows(IOException.class, () ->
                tree.execute(response, reader));
    }

    @Test
    void shouldCatchIOExceptionFromBinding() throws Exception {
        final HttpStatus anyStatus = null;
        final Binding<HttpStatus> binding = create(anyStatus, (response, converters) -> {
            throw new IOException();
        });

        final ClientHttpResponse response = mock(ClientHttpResponse.class);
        when(response.getStatusCode()).thenReturn(OK);

        final RoutingTree<HttpStatus> tree = RoutingTree.dispatch(status(), singletonList(binding));

        assertThrows(IOException.class, () ->
                tree.execute(response, reader));
    }

    @Test
    void shouldFailForDuplicateBindings() {
        assertThrows(IllegalArgumentException.class, () ->
                RoutingTree.dispatch(status(),
                        on(OK).call(pass()),
                        on(OK).call(pass())));
    }

    private MockClientHttpResponse response(final HttpStatus status) {
        return new MockClientHttpResponse(new byte[0], status);
    }

}
