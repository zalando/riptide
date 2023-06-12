package org.zalando.riptide.idempotency;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpMethod;
import org.zalando.riptide.RequestArguments;

import java.util.function.Predicate;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.http.HttpMethod.*;
import static org.zalando.riptide.Attributes.IDEMPOTENT;

final class IdempotencyPredicateTest {

    private final Predicate<RequestArguments> unit = new IdempotencyPredicate();

    private static Stream<HttpMethod> shouldDetectIdempotentMethods() {
        return Stream.of(DELETE, GET, HEAD, OPTIONS, PUT, TRACE);
    }

    @ParameterizedTest
    @MethodSource
    void shouldDetectIdempotentMethods(final HttpMethod method) {
        assertTrue(unit.test(RequestArguments.create()
                .withMethod(method)));
    }

    private static Stream<HttpMethod> shouldNotDetectNonIdempotentMethods() {
        return Stream.of(POST, PATCH);
    }

    @ParameterizedTest
    @MethodSource
    void shouldNotDetectNonIdempotentMethods(final HttpMethod method) {
        assertFalse(unit.test(RequestArguments.create()
                .withMethod(method)));
    }

    @ParameterizedTest
    @CsvSource(delimiter = ';', value = {
            "If-Match;\"xyz\"",
            "if-match;\"xyz\"",
            "If-None-Match;*",
            "if-none-match;*",
            "If-Unmodified-Since;Sat, 29 Oct 1994 19:43:31 GMT",
            "if-unmodified-since;Sat, 29 Oct 1994 19:43:31 GMT",
    })
    void shouldDetectIdempotentConditionalHeaders(final String name, final String value) {
        assertTrue(unit.test(RequestArguments.create()
                .withMethod(POST)
                .withHeader(name, value)));
    }

    @ParameterizedTest
    @CsvSource(delimiter = ';', value = {
            "If-Match;*",
            "If-Match;\"abc\", \"xyz\"",
            "If-Modified-Since;Sat, 29 Oct 1994 19:43:31 GMT",
            "If-None-Match;\"xyz\"",
            "Date;Sat, 29 Oct 1994 19:43:31 GMT"
    })
    void shouldNotDetectNonIdempotentConditionalHeaders(final String name, final String value) {
        assertFalse(unit.test(RequestArguments.create()
                .withMethod(POST)
                .withHeader(name, value)));
    }

    @Test
    void shouldDetectIdempotencyKey() {
        assertTrue(unit.test(RequestArguments.create()
                .withMethod(POST)
                .withHeader("Idempotency-Key", "xPkDYMOzZNdBoJ2l")));
    }

    @Test
    void shouldDetectIdempotencyKeyCaseInsensitive() {
        assertTrue(unit.test(RequestArguments.create()
                .withMethod(POST)
                .withHeader("idempotency-key", "xPkDYMOzZNdBoJ2l")));
    }

    private static Stream<HttpMethod> shouldDetectOverriddenSafeMethod() {
        return Stream.of(GET, HEAD, OPTIONS, TRACE);
    }

    @ParameterizedTest
    @MethodSource
    void shouldDetectOverriddenSafeMethod(final HttpMethod override) {
        assertTrue(unit.test(RequestArguments.create()
                .withMethod(POST)
                .withHeader("X-HTTP-Method-Override", override.name())));
    }

    @Test
    void shouldNotDetectOverriddenMethod() {
        assertFalse(unit.test(RequestArguments.create()
                .withMethod(POST)));
    }

    @Test
    void shouldNotDetectUnrecognizedOverriddenMethod() {
        assertFalse(unit.test(RequestArguments.create()
                .withMethod(POST)
                .withHeader("X-HTTP-Method-Override", "UNKNOWN")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"DELETE", "PUT"})
    void shouldDetectMethodOverriddenIdempotentMethod(final String override) {
        assertTrue(unit.test(RequestArguments.create()
                .withMethod(POST)
                .withHeader("X-HTTP-Method-Override", override)));
    }

    @ParameterizedTest
    @ValueSource(strings = {"PATCH", "POST"})
    void shouldDetectMethodOverriddenNonIdempotentMethod(final String override) {
        assertFalse(unit.test(RequestArguments.create()
                .withMethod(POST)
                .withHeader("X-HTTP-Method-Override", override)));
    }

    private static Stream<HttpMethod> shouldOnlyDetectMethodOverrideForPostMethod() {
        return Stream.of(PATCH);
    }

    @ParameterizedTest
    @MethodSource
    void shouldOnlyDetectMethodOverrideForPostMethod(final HttpMethod method) {
        assertFalse(unit.test(RequestArguments.create()
                .withMethod(method)
                .withHeader("X-HTTP-Method-Override", "PUT")));
    }

    @Test
    void shouldDetectCaseInsensitiveMethodOverride() {
        assertTrue(unit.test(RequestArguments.create()
                .withMethod(POST)
                .withHeader("x-http-method-override", "GET")));
    }

    @Test
    void shouldDetectCallSiteOverride() {
        assertTrue(unit.test(RequestArguments.create()
                .withMethod(POST)
                .withAttribute(IDEMPOTENT, true)));
    }

    @Test
    void shouldNotDetectNegativeCallSiteOverride() {
        assertFalse(unit.test(RequestArguments.create()
                .withMethod(GET)
                .withAttribute(IDEMPOTENT, false)));
    }

}
