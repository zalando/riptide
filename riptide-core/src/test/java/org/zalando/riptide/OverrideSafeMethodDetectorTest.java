package org.zalando.riptide;

import com.google.common.collect.ImmutableMultimap;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.http.HttpMethod.DELETE;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.PATCH;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpMethod.PUT;

final class OverrideSafeMethodDetectorTest {

    private final MethodDetector unit = new OverrideSafeMethodDetector();

    @Test
    // TODO test this with all safe methods, when migrating to JUnit 5
    void shouldDetectOverriddenSafeMethod() {
        assertTrue(unit.test(arguments(POST, GET)));
    }

    @Test
    void shouldNotDetectOverriddenMethod() {
        assertFalse(unit.test(RequestArguments.create().withMethod(POST)));
    }

    @Test
    void shouldNotDetectUnrecognizedOverriddenMethod() {
        assertFalse(unit.test(arguments(POST, "UNKNOWN")));
    }

    @Test
    void shouldNotDetectOverriddenUnsafeMethod() {
        assertFalse(unit.test(arguments(POST, DELETE)));
    }

    @Test
    // TODO test this with all other methods, when migrating to JUnit 5
    void shouldOnlyDetectForPostMethod() {
        assertFalse(unit.test(arguments(PATCH, PUT)));
    }

    RequestArguments arguments(final HttpMethod method, final HttpMethod override) {
        return arguments(method, override.name());
    }

    RequestArguments arguments(final HttpMethod method, final String name) {
        return RequestArguments.create()
                    .withMethod(method)
                    .withHeaders(ImmutableMultimap.of("X-HTTP-Method-Override", name));
    }

}
