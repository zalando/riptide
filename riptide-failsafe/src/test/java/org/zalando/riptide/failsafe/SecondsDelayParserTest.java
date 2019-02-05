package org.zalando.riptide.failsafe;

import org.junit.Test;

import java.time.Duration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public final class SecondsDelayParserTest {

    private final DelayParser unit = new SecondsDelayParser();

    @Test
    public void shouldParseSingleDigit() {
        assertEquals(Duration.ofSeconds(1), unit.parse("1"));
    }

    @Test
    public void shouldParseDoubleDigitDelay() {
        assertEquals(Duration.ofSeconds(17), unit.parse("17"));
    }

    @Test
    public void shouldIgnoreHttpDateDelay() {
        assertNull(unit.parse("Wed, 11 Apr 2018 22:34:28 GMT"));
    }

    @Test
    public void shouldIgnoreEmptyString() {
        assertNull(unit.parse(""));
    }

}
