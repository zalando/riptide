package org.zalando.riptide.autoconfigure;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasToString;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class TimeSpanTest {

    @Test
    void shouldParseEmpty() {
        final TimeSpan span = TimeSpan.valueOf("");
        assertThat(span.getAmount(), is(0L));
    }

    @Test
    void shouldParseSingular() {
        final TimeSpan span = TimeSpan.valueOf("1 second");
        assertThat(span.to(SECONDS), is(1L));
    }

    @Test
    void shouldParseUsingConstructor() {
        final TimeSpan span = new TimeSpan("1 second");
        assertThat(span.getAmount(), is(1L));
        assertThat(span.getUnit(), is(SECONDS));
    }

    @Test
    void shouldParsePlural() {
        final TimeSpan span = TimeSpan.valueOf("17 milliseconds");
        assertThat(span.to(MILLISECONDS), is(17L));
    }

    @Test
    void shouldParseNonLowerCase() {
        final TimeSpan span = TimeSpan.valueOf("17 Seconds");
        assertThat(span.to(SECONDS), is(17L));
    }

    @Test
    void shouldFailOnUnsupportedTimeSpanFormat() {
        assertThrows(IllegalArgumentException.class, () -> TimeSpan.valueOf("forever"));
    }

    @Test
    void shouldFailOnUnknownTimeUnit() {
        assertThrows(IllegalArgumentException.class, () -> TimeSpan.valueOf("1 decade"));
    }

    @Test
    void shouldApplyTo() {
        final Map<Long, TimeUnit> consumer = new HashMap<>();
        TimeSpan.valueOf("1 second").applyTo(consumer::put);

        assertThat(consumer, hasEntry(1L, SECONDS));
    }

    @Test
    void shouldRenderToString() {
        assertThat(TimeSpan.valueOf("17 seconds"), hasToString("17 seconds"));
    }

}
