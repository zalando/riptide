package org.zalando.riptide;

import com.google.common.collect.ImmutableMultimap;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class CompoundMethodDetectorTest {

    private final MethodDetector unit = MethodDetector.compound(
            arguments -> arguments.getHeaders().containsKey("A"),
            arguments -> arguments.getHeaders().containsKey("B")
    );

    @Test
    public void shouldDetectA() {
        assertTrue(unit.test(RequestArguments.create()
                .withHeaders(ImmutableMultimap.of("A", "any"))));
    }

    @Test
    public void shouldDetectB() {
        assertTrue(unit.test(RequestArguments.create()
                .withHeaders(ImmutableMultimap.of("B", "any"))));
    }

    @Test
    public void shouldDetectNone() {
        assertFalse(unit.test(RequestArguments.create()));
    }

}
