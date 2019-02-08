package org.zalando.riptide.failsafe;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

final class SecondsDelayParserTest {

    private final DelayParser unit = new SecondsDelayParser();

    @Test
    void shouldParseSingleDigit() {
        assertEquals(Duration.ofSeconds(1), unit.parse("1"));
    }

    @Test
    void shouldParseDoubleDigitDelay() {
        assertEquals(Duration.ofSeconds(17), unit.parse("17"));
    }

    @Test
    void shouldIgnoreHttpDateDelay() {
        assertNull(unit.parse("Wed, 11 Apr 2018 22:34:28 GMT"));
    }

    @Test
    void shouldIgnoreEmptyString() {
        assertNull(unit.parse(""));
    }

}
