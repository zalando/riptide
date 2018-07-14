package org.zalando.riptide;

import org.junit.Test;
import org.springframework.http.HttpMethod;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.springframework.http.HttpMethod.DELETE;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.HEAD;
import static org.springframework.http.HttpMethod.OPTIONS;
import static org.springframework.http.HttpMethod.PATCH;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpMethod.PUT;
import static org.springframework.http.HttpMethod.TRACE;

// TODO rewrite using @ValueSource when migrating t JUnit 5
public final class DefaultSafeMethodDetectorTest {

    private final MethodDetector unit = new DefaultSafeMethodDetector();

    @Test
    public void options() {
        assertTrue(unit.test(arguments(OPTIONS)));
    }

    @Test
    public void get() {
        assertTrue(unit.test(arguments(GET)));
    }

    @Test
    public void head() {
        assertTrue(unit.test(arguments(HEAD)));
    }

    @Test
    public void trace() {
        assertTrue(unit.test(arguments(TRACE)));
    }

    @Test
    public void post() {
        assertFalse(unit.test(arguments(POST)));
    }

    @Test
    public void put() {
        assertFalse(unit.test(arguments(PUT)));
    }

    @Test
    public void patch() {
        assertFalse(unit.test(arguments(PATCH)));
    }

    @Test
    public void delete() {
        assertFalse(unit.test(arguments(DELETE)));
    }

    private RequestArguments arguments(final HttpMethod method) {
        return RequestArguments.create().withMethod(method);
    }

}
