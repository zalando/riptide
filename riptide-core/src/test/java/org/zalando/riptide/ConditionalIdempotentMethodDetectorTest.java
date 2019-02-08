package org.zalando.riptide;

import com.google.common.collect.ImmutableMultimap;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

// TODO rewrite using @ValueSource when migrating to JUnit 5
final class ConditionalIdempotentMethodDetectorTest {

    private final MethodDetector unit = new ConditionalIdempotentMethodDetector();

    @Test
    void shouldDetectIfMatch() {
        assertTrue(unit.test(arguments("If-Match", "xyzzy")));
    }

    @Test
    void shouldDetectIfMatchCaseInsensitive() {
        assertTrue(unit.test(arguments("if-match", "xyzzy")));
    }

    @Test
    void shouldDetectIfNoneMatch() {
        assertTrue(unit.test(arguments("If-None-Match", "*")));
    }

    @Test
    void shouldDetectIfNoneMatchCaseInsensitive() {
        assertTrue(unit.test(arguments("if-none-match", "*")));
    }

    @Test
    void shouldDetectIfUnmodifiedSince() {
        assertTrue(unit.test(arguments("If-Unmodified-Since", "Sat, 29 Oct 1994 19:43:31 GMT")));
    }

    @Test
    void shouldDetectIfUnmodifiedSinceCaseInsensitive() {
        assertTrue(unit.test(arguments("if-unmodified-since", "Sat, 29 Oct 1994 19:43:31 GMT")));
    }

    @Test
    void shouldNotDetectUnconditional() {
        assertFalse(unit.test(RequestArguments.create()));
    }

    @Test
    void shouldNotDetectIfModifiedSince() {
        assertFalse(unit.test(arguments("If-Modified-Since", "Sat, 29 Oct 1994 19:43:31 GMT")));
    }

    RequestArguments arguments(final String name, final String value) {
        return RequestArguments.create()
                .withHeaders(ImmutableMultimap.of(name, value));
    }

}
