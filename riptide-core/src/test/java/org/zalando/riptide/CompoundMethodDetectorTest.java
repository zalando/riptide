package org.zalando.riptide;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class CompoundMethodDetectorTest {

    private final MethodDetector unit = MethodDetector.compound(
            arguments -> arguments.getHeaders().containsKey("A"),
            arguments -> arguments.getHeaders().containsKey("B")
    );

    @Test
    void shouldDetectA() {
        assertTrue(unit.test(RequestArguments.create()
                .withHeader("A", "any")));
    }

    @Test
    void shouldDetectB() {
        assertTrue(unit.test(RequestArguments.create()
                .withHeader("B", "any")));
    }

    @Test
    void shouldDetectNone() {
        assertFalse(unit.test(RequestArguments.create()));
    }

}
