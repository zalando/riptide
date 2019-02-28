package org.zalando.riptide;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.http.HttpMethod.POST;

final class OverrideIdempotentMethodDetectorTest {

    private final MethodDetector unit = new OverrideIdempotentMethodDetector();

    @Test
    void shouldOverrideNonIdempotent() {
        assertTrue(unit.test(RequestArguments.create()
                .withMethod(POST)
                .withAttribute(MethodDetector.IDEMPOTENT, true)));
    }

    @Test
    void shouldNotOverrideNonIdempotent() {
        assertFalse(unit.test(RequestArguments.create()
            .withMethod(POST)
            .withAttribute(MethodDetector.IDEMPOTENT, false)));
    }

}
