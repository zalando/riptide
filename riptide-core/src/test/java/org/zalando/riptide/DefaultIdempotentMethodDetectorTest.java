package org.zalando.riptide;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.http.HttpMethod.DELETE;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.HEAD;
import static org.springframework.http.HttpMethod.OPTIONS;
import static org.springframework.http.HttpMethod.PATCH;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpMethod.PUT;
import static org.springframework.http.HttpMethod.TRACE;

// TODO rewrite using @ValueSource when migrating to JUnit 5
final class DefaultIdempotentMethodDetectorTest {

    private final MethodDetector unit = new DefaultIdempotentMethodDetector();

    @Test
    void options() {
        assertTrue(unit.test(arguments(OPTIONS)));
    }

    @Test
    void get() {
        assertTrue(unit.test(arguments(GET)));
    }

    @Test
    void head() {
        assertTrue(unit.test(arguments(HEAD)));
    }

    @Test
    void trace() {
        assertTrue(unit.test(arguments(TRACE)));
    }

    @Test
    void post() {
        assertFalse(unit.test(arguments(POST)));
    }

    @Test
    void put() {
        assertTrue(unit.test(arguments(PUT)));
    }

    @Test
    void patch() {
        assertFalse(unit.test(arguments(PATCH)));
    }

    @Test
    void delete() {
        assertTrue(unit.test(arguments(DELETE)));
    }

    private RequestArguments arguments(final HttpMethod method) {
        return RequestArguments.create().withMethod(method);
    }

}
