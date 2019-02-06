package org.zalando.riptide.failsafe;

import org.junit.Test;

import java.time.Duration;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public final class CompoundDelayParserTest {

    private final DelayParser first = mock(DelayParser.class);
    private final DelayParser second = mock(DelayParser.class);

    private final DelayParser unit = new CompoundDelayParser(Arrays.asList(first, second));

    @Test
    public void shouldUseFirstNonNullDelay() {
        when(first.parse("1")).thenReturn(Duration.ofSeconds(1));
        when(second.parse("2")).thenReturn(Duration.ofSeconds(2));

        assertEquals(Duration.ofSeconds(1), unit.parse("1"));
    }

    @Test
    public void shouldIgnoreNullDelay() {
        when(second.parse("2")).thenReturn(Duration.ofSeconds(2));

        assertEquals(Duration.ofSeconds(2), unit.parse("2"));
    }

    @Test
    public void shouldFallbackToNullDelay() {
        when(first.parse("1")).thenReturn(Duration.ofSeconds(1));
        when(second.parse("2")).thenReturn(Duration.ofSeconds(2));

        assertNull(unit.parse("3"));
    }

}
