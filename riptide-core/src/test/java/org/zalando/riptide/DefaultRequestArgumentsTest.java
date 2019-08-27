package org.zalando.riptide;

import lombok.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;
import org.springframework.http.*;
import org.zalando.riptide.RequestArguments.*;

import java.net.*;
import java.util.*;
import java.util.function.*;

import static java.util.Collections.*;
import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

final class DefaultRequestArgumentsTest {

    private final RequestArguments unit = RequestArguments.create();

    @Value
    private static final class Assertion<T> {
        BiFunction<RequestArguments, T, RequestArguments> wither;
        T argument;
        Function<RequestArguments, T> getter;
    }

    static List<Assertion<?>> data() {
        return Arrays.asList(
                new Assertion<>(RequestArguments::withBaseUrl, URI.create("https://api.example.com"), RequestArguments::getBaseUrl),
                new Assertion<>(RequestArguments::withUrlResolution, UrlResolution.APPEND, RequestArguments::getUrlResolution),
                new Assertion<>(RequestArguments::withMethod, HttpMethod.GET, RequestArguments::getMethod),
                new Assertion<>(RequestArguments::withUriTemplate, "/{id}", RequestArguments::getUriTemplate),
                new Assertion<>(RequestArguments::withUri, URI.create("/123"), RequestArguments::getUri),
                new Assertion<>(RequestArguments::withBody, new Object(), RequestArguments::getBody),
                new Assertion<>(RequestArguments::withEntity, mock(Entity.class), RequestArguments::getEntity),
                new Assertion<>(RequestArguments::withRoute, mock(Route.class), RequestArguments::getRoute)
        );
    }

    @ParameterizedTest
    @MethodSource("data")
    <T> void shouldOptimizeForReapplyingSameValue(final Assertion<T> assertion) {
        final RequestArguments applied = assertion.wither.apply(unit, assertion.argument);
        final RequestArguments appliedAgain = assertion.wither.apply(applied, assertion.argument);

        assertThat(appliedAgain, is(sameInstance(applied)));
    }

    @ParameterizedTest
    @MethodSource("data")
    <T> void shouldModifyValue(final Assertion<T> assertion) {
        final RequestArguments applied = assertion.wither.apply(unit, assertion.argument);

        assertThat(applied, is(not(sameInstance(unit))));
        assertThat(assertion.getter.apply(applied), is(sameInstance(assertion.argument)));
    }

    @Test
    void shouldRemoveQueryParam() {
        final RequestArguments with = unit.withQueryParam("foo", "bar");
        assertThat(with.getQueryParams(), hasKey("foo"));

        final RequestArguments without = with.withoutQueryParam("foo");
        assertThat(without.getQueryParams(), not(hasKey("foo")));
    }

    @Test
    void shouldReplaceQueryParam() {
        final RequestArguments with = unit.withQueryParam("foo", "bar");
        assertThat(with.getQueryParams(), hasKey("foo"));

        final RequestArguments without = with.replaceQueryParams(singletonMap("q", singletonList("example")));
        assertThat(without.getQueryParams(), hasEntry("q", singletonList("example")));
    }

    @Test
    void shouldRemoveHeader() {
        final RequestArguments with = unit.withHeader("Foo", "bar");
        assertThat(with.getHeaders(), hasKey("Foo"));

        final RequestArguments without = with.withoutHeader("Foo");
        assertThat(without.getHeaders(), not(hasKey("Foo")));
    }

    @Test
    void shouldReplaceHeaders() {
        final RequestArguments with = unit.withHeader("Foo", "bar");
        assertThat(with.getHeaders(), hasKey("Foo"));

        final RequestArguments without = with.replaceHeaders(singletonMap("Test", singletonList("true")));
        assertThat(without.getHeaders(), hasEntry("Test", singletonList("true")));
    }

    @Test
    void headersShouldBeCaseInsensitive() {
        final RequestArguments arguments = unit.withHeader("Foo", "bar");
        assertTrue(arguments.getHeaders().containsKey("foo"));
    }

    @Test
    void shouldCacheRequestUri() {
        final RequestArguments arguments = unit
                .withBaseUrl(URI.create("https://www.example.org"))
                .withUrlResolution(UrlResolution.RFC)
                .withUriTemplate("/users/{user}")
                .replaceUriVariables(singletonList("me"));

        assertSame(arguments.getRequestUri(), arguments.getRequestUri());
    }

}
