package org.zalando.riptide;

import com.google.common.collect.ImmutableMultimap;
import org.junit.Test;

import static org.junit.Assert.*;

// TODO rewrite using @ValueSource when migrating to JUnit 5
public final class ConditionalIdempotentMethodDetectorTest {

    private final MethodDetector unit = new ConditionalIdempotentMethodDetector();

    @Test
    public void shouldDetectIfMatch() {
        assertTrue(unit.test(arguments("If-Match", "xyzzy")));
    }

    @Test
    public void shouldDetectIfMatchCaseInsensitive() {
        assertTrue(unit.test(arguments("if-match", "xyzzy")));
    }

    @Test
    public void shouldDetectIfNoneMatch() {
        assertTrue(unit.test(arguments("If-None-Match", "*")));
    }

    @Test
    public void shouldDetectIfNoneMatchCaseInsensitive() {
        assertTrue(unit.test(arguments("if-none-match", "*")));
    }

    @Test
    public void shouldDetectIfUnmodifiedSince() {
        assertTrue(unit.test(arguments("If-Unmodified-Since", "Sat, 29 Oct 1994 19:43:31 GMT")));
    }

    @Test
    public void shouldDetectIfUnmodifiedSinceCaseInsensitive() {
        assertTrue(unit.test(arguments("if-unmodified-since", "Sat, 29 Oct 1994 19:43:31 GMT")));
    }

    @Test
    public void shouldNotDetectUnconditional() {
        assertFalse(unit.test(RequestArguments.create()));
    }

    @Test
    public void shouldNotDetectIfModifiedSince() {
        assertFalse(unit.test(arguments("If-Modified-Since", "Sat, 29 Oct 1994 19:43:31 GMT")));
    }

    RequestArguments arguments(final String name, final String value) {
        return RequestArguments.create()
                .withHeaders(ImmutableMultimap.of(name, value));
    }

}
