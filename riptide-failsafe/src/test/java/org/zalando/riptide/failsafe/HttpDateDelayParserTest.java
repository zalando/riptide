package org.zalando.riptide.failsafe;

import net.jodah.failsafe.util.Duration;
import org.junit.Test;

import java.time.Clock;

import static java.time.Instant.parse;
import static java.time.ZoneOffset.UTC;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public final class HttpDateDelayParserTest {

    private final DelayParser unit = new HttpDateDelayParser(Clock.fixed(parse("2018-06-24T01:19:37Z"), UTC));

    @Test
    public void shouldParseHttpDate() {
        assertEquals(new Duration(17, SECONDS), unit.parse("Sun, 24 Jun 2018 01:19:54 GMT"));
    }

    @Test
    public void shouldIgnoreSeconds() {
        assertNull(unit.parse("17"));
    }

    @Test
    public void shouldIgnoreIso8601() {
        assertNull(unit.parse("2018-04-11T22:34:28Z"));
    }

}
