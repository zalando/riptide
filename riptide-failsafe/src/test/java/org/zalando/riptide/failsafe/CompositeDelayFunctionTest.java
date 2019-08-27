package org.zalando.riptide.failsafe;

import net.jodah.failsafe.function.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.*;
import org.mockito.junit.jupiter.*;

import java.time.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
final class CompositeDelayFunctionTest {

    @SuppressWarnings("unchecked")
    private final DelayFunction<String, Exception> first = (DelayFunction) mock(DelayFunction.class);

    @SuppressWarnings("unchecked")
    private final DelayFunction<String, Exception> second = (DelayFunction) mock(DelayFunction.class);

    private final DelayFunction<String, Exception> unit = new CompositeDelayFunction<>(
            Arrays.asList(first, second));

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
