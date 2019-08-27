package org.zalando.riptide;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.*;
import org.mockito.*;
import org.mockito.junit.jupiter.*;
import org.springframework.http.*;
import org.springframework.http.HttpStatus.*;
import org.springframework.http.client.*;
import org.springframework.mock.http.client.*;

import java.io.*;

import static java.util.Collections.*;
import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Answers.CALLS_REAL_METHODS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.http.HttpStatus.*;
import static org.springframework.http.HttpStatus.Series.*;
import static org.zalando.riptide.Binding.*;
import static org.zalando.riptide.Bindings.*;
import static org.zalando.riptide.Navigators.*;
import static org.zalando.riptide.PassRoute.*;
import static org.zalando.riptide.RoutingTree.*;

@ExtendWith(MockitoExtension.class)
final class RoutingTreeTest {

    @Mock(answer = CALLS_REAL_METHODS)
    private Route other;

    @Mock(answer = CALLS_REAL_METHODS)
    private Route expected;

    private final MessageReader reader = mock(MessageReader.class);

    @Test
    void shouldExposeNavigator() {
        final RoutingTree<HttpStatus> unit = dispatch(status());

        assertThat(unit.getNavigator(), is(status()));
    }

    @Test
    void shouldUsedAttributeRoute() throws Exception {
        dispatch(status(),
                on(OK).call(expected),
                anyStatus().call(other))
                .execute(response(OK), reader);

        verify(expected).execute(any(), any());
    }

    @Test
    void shouldUsedWildcardRoute() throws Exception {
        dispatch(status(),
                on(OK).call(other),
                anyStatus().call(expected))
                .execute(response(CREATED), reader);

        verify(expected).execute(any(), any());
    }

    @Test
    void shouldUsedAddedAttributeRoute() throws Exception {
        dispatch(status(),
                anyStatus().call(other))
                .merge(on(OK).call(expected))
                .execute(response(OK), reader);

        verify(expected).execute(any(), any());
    }

    @Test
    void shouldUsedAddedWildcardRoute() throws Exception {
        dispatch(status(),
                on(OK).call(other))
                .merge(anyStatus().call(expected))
                .execute(response(CREATED), reader);

        verify(expected).execute(any(), any());
    }

    @Test
    void shouldUseLastWildcardRoute() throws Exception {
        final RoutingTree<HttpStatus> merge = dispatch(status(),
                anyStatus().call(other))
                .merge(anyStatus().call(expected));

        merge.execute(response(OK), reader);

        verify(expected).execute(any(), any());
    }

    @Test
    void shouldUseLastAttributeRoute() throws Exception {
        dispatch(status(),
                on(OK).call(other))
                .merge(on(OK).call(expected))
                .execute(response(OK), reader);

        verify(expected).execute(any(), any());
    }

    @Test
    void shouldUseLastAddedAttributeRoute() throws Exception {
        dispatch(status(),
                on(OK).call(other),
                anyStatus().call(other))
                .merge(on(OK).call(other))
                .merge(on(OK).call(expected))
                .execute(response(OK), reader);

        verify(expected).execute(any(), any());
    }

    @Test
    void shouldUseLastAddedWildcardRoute() throws Exception {
        dispatch(status(),
                on(OK).call(other),
                anyStatus().call(other))
                .merge(singletonList(anyStatus().call(expected)))
                .execute(response(CREATED), reader);

        verify(expected).execute(any(), any());
    }

    @Test
    void shouldMergeRecursively() throws Exception {
        final RoutingTree<Series> left = dispatch(series(),
                on(SUCCESSFUL).dispatch(status(),
                        anyStatus().call(other)),
                anySeries().call(other));

        final RoutingTree<Series> right = dispatch(series(),
                on(SUCCESSFUL).dispatch(status(),
                        on(CREATED).call(expected)));

        left.merge(right).execute(response(CREATED), reader);

        verify(expected).execute(any(), any());
    }

    @Test
    void shouldUseOtherWhenMergingOnDifferentAttributes() throws Exception {
        final RoutingTree<Series> left = dispatch(series(),
                on(SUCCESSFUL).dispatch(status(),
                        anyStatus().call(other)),
                anySeries().call(pass()));

        final RoutingTree<HttpStatus> right = dispatch(status(),
                on(CREATED).call(expected),
                anyStatus().call(pass()));

        final Route merge = left.merge(right);

        merge.execute(response(CREATED), reader);
        merge.execute(response(OK), reader);

        verify(expected).execute(any(), any());
        verify(other, never()).execute(any(), any());
    }

    @Test
    void shouldUseNonRoutingTreeOnMerge() throws Exception {
        final RoutingTree<Series> left = dispatch(series(),
                on(SUCCESSFUL).dispatch(status(),
                        anyStatus().call(other)),
                anySeries().call(pass()));

        final Route merge = left.merge(expected);

        merge.execute(response(CREATED), reader);
        merge.execute(response(OK), reader);

        verify(expected, times(2)).execute(any(), any());
        verify(other, never()).execute(any(), any());
    }

    @Test
    void shouldCreateNewRoutingTreeIfChanged() {
        final RoutingTree<HttpStatus> tree = dispatch(status(), on(OK).call(pass()));
        final RoutingTree<HttpStatus> result = tree.merge(anyStatus().call(pass()));
        assertNotEquals(tree, result);
    }

    @Test
    void shouldCreateNewRoutingTreeIfNotChanged() {
        final RoutingTree<HttpStatus> tree = dispatch(status(), on(OK).call(pass()));
        final RoutingTree<HttpStatus> result = tree.merge(on(OK).call(pass()));
        assertNotEquals(tree, result);
    }

    @Test
    void shouldCatchIOExceptionFromResponse() throws Exception {
        final ClientHttpResponse response = mock(ClientHttpResponse.class);
        when(response.getStatusCode()).thenThrow(new IOException());

        final RoutingTree<HttpStatus> tree = dispatch(status(),
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

        final RoutingTree<HttpStatus> tree = dispatch(status(), singletonList(binding));

        assertThrows(IOException.class, () ->
                tree.execute(response, reader));
    }

    @Test
    void shouldFailForDuplicateBindings() {
        assertThrows(IllegalArgumentException.class, () ->
                dispatch(status(),
                        on(OK).call(pass()),
                        on(OK).call(pass())));
    }

    private MockClientHttpResponse response(final HttpStatus status) {
        return new MockClientHttpResponse(new byte[0], status);
    }

}
