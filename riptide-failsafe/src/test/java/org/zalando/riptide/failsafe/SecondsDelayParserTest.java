package org.zalando.riptide.failsafe;

import net.jodah.failsafe.util.Duration;
import org.junit.Test;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public final class SecondsDelayParserTest {

    private final DelayParser unit = new SecondsDelayParser();

    @Test
    public void shouldParseSingleDigit() {
        assertEquals(new Duration(1, SECONDS), unit.parse("1"));
    }

    @Test
    public void shouldParseDoubleDigitDelay() {
        assertEquals(new Duration(17, SECONDS), unit.parse("17"));
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
