package org.zalando.riptide.failsafe;

import net.jodah.failsafe.function.DelayFunction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.zalando.riptide.failsafe.CompositeDelayFunction.composite;

@ExtendWith(MockitoExtension.class)
final class CompositeDelayFunctionTest {

    private final DelayFunction<String, Exception> first;
    private final DelayFunction<String, Exception> second;
    private final DelayFunction<String, Exception> unit;

    CompositeDelayFunctionTest(
            @Mock final DelayFunction<String, Exception> first,
            @Mock final DelayFunction<String, Exception> second) {
        this.first = first;
        this.second = second;
        this.unit = composite(first, second);
    }

    @BeforeEach
    void defaultBehavior() {
        // starting with Mockito 3.4.4, mocks will return Duration.ZERO instead of null, by default
        when(first.computeDelay(any(), any(), any())).thenReturn(null);
        when(second.computeDelay(any(), any(), any())).thenReturn(null);
    }

    @Test
    void shouldUseFirstNonNullDelay() {
        when(first.computeDelay(eq("1"), any(), any())).thenReturn(Duration.ofSeconds(1));
        when(second.computeDelay(eq("2"), any(), any())).thenReturn(Duration.ofSeconds(2));

        assertEquals(Duration.ofSeconds(1), unit.computeDelay("1", null, null));
    }

    @Test
    void shouldIgnoreNullDelay() {
        when(second.computeDelay(eq("2"), any(), any())).thenReturn(Duration.ofSeconds(2));

        assertEquals(Duration.ofSeconds(2), unit.computeDelay("2", null, null));
    }

    @Test
    void shouldFallbackToNullDelay() {
        when(first.computeDelay(eq("1"), any(), any())).thenReturn(Duration.ofSeconds(1));
        when(second.computeDelay(eq("2"), any(), any())).thenReturn(Duration.ofSeconds(2));

        assertNull(unit.computeDelay("3", null, null));
    }

}
