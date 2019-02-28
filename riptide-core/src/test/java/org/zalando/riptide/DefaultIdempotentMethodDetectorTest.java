package org.zalando.riptide;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.http.HttpMethod;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.provider.EnumSource.Mode.EXCLUDE;
import static org.junit.jupiter.params.provider.EnumSource.Mode.INCLUDE;

final class DefaultIdempotentMethodDetectorTest {

    private final MethodDetector unit = new DefaultIdempotentMethodDetector();

    @ParameterizedTest
    @EnumSource(value = HttpMethod.class, mode = EXCLUDE, names = {"POST", "PATCH"})
    void shouldDetectIdempotentMethods(final HttpMethod method) {
        assertTrue(unit.test(arguments(method)));
    }

    @ParameterizedTest
    @EnumSource(value = HttpMethod.class, mode = INCLUDE, names = {"POST", "PATCH"})
    void shouldDetectNonIdempotentMethods(final HttpMethod method) {
        assertFalse(unit.test(arguments(method)));
    }

    private RequestArguments arguments(final HttpMethod method) {
        return RequestArguments.create().withMethod(method);
    }

}
