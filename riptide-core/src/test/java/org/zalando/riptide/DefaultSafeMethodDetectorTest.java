package org.zalando.riptide;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.http.HttpMethod;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.provider.EnumSource.Mode.EXCLUDE;
import static org.junit.jupiter.params.provider.EnumSource.Mode.INCLUDE;

final class DefaultSafeMethodDetectorTest {

    private final MethodDetector unit = new DefaultSafeMethodDetector();

    @ParameterizedTest
    @EnumSource(value = HttpMethod.class, mode = EXCLUDE, names = {"POST", "PUT", "PATCH", "DELETE"})
    void shouldDetectSafeMethods(final HttpMethod method) {
        assertTrue(unit.test(arguments(method)));
    }

    @ParameterizedTest
    @EnumSource(value = HttpMethod.class, mode = INCLUDE, names = {"POST", "PUT", "PATCH", "DELETE"})
    void shouldDetectUnsafeMethods(final HttpMethod method) {
        assertFalse(unit.test(arguments(method)));
    }

    private RequestArguments arguments(final HttpMethod method) {
        return RequestArguments.create().withMethod(method);
    }

}
