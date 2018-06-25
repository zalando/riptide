package org.zalando.riptide.failsafe;

import net.jodah.failsafe.util.Duration;

import javax.annotation.Nullable;
import java.time.Clock;
import java.time.Instant;
import java.time.format.DateTimeParseException;

import static java.time.Duration.between;
import static java.time.Instant.now;
import static java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME;
import static java.util.concurrent.TimeUnit.SECONDS;

final class HttpDateDelayParser implements DelayParser {

    private final Clock clock;

    HttpDateDelayParser(final Clock clock) {
        this.clock = clock;
    }

    @Override
    public Duration parse(final String value) {
        return until(parseDate(value));
    }

    @Nullable
    private Instant parseDate(final String value) {
        try {
            return Instant.from(RFC_1123_DATE_TIME.parse(value));
        } catch (final DateTimeParseException e) {
            return null;
        }
    }

    @Nullable
    private Duration until(@Nullable final Instant end) {
        return end == null ? null : new Duration(between(now(clock), end).getSeconds(), SECONDS);
    }

}
