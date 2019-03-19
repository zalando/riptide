package org.zalando.riptide.failsafe;

import net.jodah.failsafe.RetryPolicy.DelayFunction;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

final class CompositeDelayFunctionTest {

    @SuppressWarnings("unchecked")
    private final DelayFunction<String, Exception> first = (DelayFunction) mock(DelayFunction.class);

    @SuppressWarnings("unchecked")
    private final DelayFunction<String, Exception> second = (DelayFunction) mock(DelayFunction.class);

    private final DelayFunction<String, Exception> unit = new CompositeDelayFunction<>(Arrays.asList(first, second));

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
