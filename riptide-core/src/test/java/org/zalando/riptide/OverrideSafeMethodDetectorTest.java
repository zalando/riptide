package org.zalando.riptide;

import com.google.common.collect.ImmutableMultimap;
import org.junit.Test;
import org.springframework.http.HttpMethod;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.springframework.http.HttpMethod.DELETE;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.PATCH;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpMethod.PUT;

public final class OverrideSafeMethodDetectorTest {

    private final MethodDetector unit = new OverrideSafeMethodDetector();

    @Test
    // TODO test this with all safe methods, when migrating to JUnit 5
    public void shouldDetectOverriddenSafeMethod() {
        assertTrue(unit.test(arguments(POST, GET)));
    }

    @Test
    public void shouldNotDetectOverriddenMethod() {
        assertFalse(unit.test(RequestArguments.create().withMethod(POST)));
    }

    @Test
    public void shouldNotDetectUnrecognizedOverriddenMethod() {
        assertFalse(unit.test(arguments(POST, "UNKNOWN")));
    }

    @Test
    public void shouldNotDetectOverriddenUnsafeMethod() {
        assertFalse(unit.test(arguments(POST, DELETE)));
    }

    @Test
    // TODO test this with all other methods, when migrating to JUnit 5
    public void shouldOnlyDetectForPostMethod() {
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
