package org.zalando.riptide.failsafe;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;

import static java.time.Instant.parse;
import static java.time.ZoneOffset.UTC;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

final class EpochSecondsDelayParserTest {

    private final DelayParser unit = new EpochSecondsDelayParser(Clock.fixed(parse("2018-06-24T01:19:37Z"), UTC));

    @Test
    void shouldParseEpochSeconds() {
        assertEquals(Duration.ofSeconds(17), unit.parse("1529803194"));
    }

    @Test
    void shouldIgnoreSeconds() {
        assertNull(unit.parse("17"));
    }

    @Test
    void shouldIgnoreSecondsUpTo24HoursInThePast() {
        assertNull(unit.parse("1529709577"));
    }

    @Test
    void shouldParseEpochSecondsOfAtLeast24Hours() {
        assertEquals(Duration.ofDays(1).plusHours(2).minusSeconds(1).negated(), unit.parse("1529709578"));
    }

    @Test
    void shouldParseEpochSecondsFarInTheFuture() {
        assertEquals(Duration.ofDays(1), unit.parse("1529889577"));
    }

    @Test
    void shouldIgnoreIso8601() {
        assertNull(unit.parse("2018-04-11T22:34:28Z"));
    }

    @Test
    void shouldIgnoreEmptyString() {
        assertNull(unit.parse(""));
    }

}
