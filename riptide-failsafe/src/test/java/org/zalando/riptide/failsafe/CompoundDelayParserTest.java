package org.zalando.riptide.failsafe;

import net.jodah.failsafe.util.Duration;
import org.junit.Test;

import java.util.Arrays;

import static java.util.concurrent.TimeUnit.SECONDS;
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
        when(first.parse("1")).thenReturn(new Duration(1, SECONDS));
        when(second.parse("2")).thenReturn(new Duration(2, SECONDS));

        assertEquals(new Duration(1, SECONDS), unit.parse("1"));
    }

    @Test
    public void shouldIgnoreNullDelay() {
        when(second.parse("2")).thenReturn(new Duration(2, SECONDS));

        assertEquals(new Duration(2, SECONDS), unit.parse("2"));
    }

    @Test
    public void shouldFallbackToNullDelay() {
        when(first.parse("1")).thenReturn(new Duration(1, SECONDS));
        when(second.parse("2")).thenReturn(new Duration(2, SECONDS));

        assertNull(unit.parse("3"));
    }

}
