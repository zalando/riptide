package org.zalando.riptide.failsafe;

import org.junit.jupiter.api.*;

import java.time.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

final class CompositeDelayParserTest {

    private final DelayParser first = mock(DelayParser.class);
    private final DelayParser second = mock(DelayParser.class);

    private final DelayParser unit = new CompositeDelayParser(Arrays.asList(first, second));

    @Test
    void shouldUseFirstNonNullDelay() {
        when(first.parse("1")).thenReturn(Duration.ofSeconds(1));
        when(second.parse("2")).thenReturn(Duration.ofSeconds(2));

        assertEquals(Duration.ofSeconds(1), unit.parse("1"));
    }

    @Test
    void shouldIgnoreNullDelay() {
        when(second.parse("2")).thenReturn(Duration.ofSeconds(2));

        assertEquals(Duration.ofSeconds(2), unit.parse("2"));
    }

    @Test
    void shouldFallbackToNullDelay() {
        when(first.parse("1")).thenReturn(Duration.ofSeconds(1));
        when(second.parse("2")).thenReturn(Duration.ofSeconds(2));

        assertNull(unit.parse("3"));
    }

}
