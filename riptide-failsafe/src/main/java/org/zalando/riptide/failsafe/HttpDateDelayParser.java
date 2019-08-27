package org.zalando.riptide.failsafe;

import javax.annotation.*;
import java.time.*;
import java.time.format.*;

import static java.time.Duration.*;
import static java.time.Instant.*;
import static java.time.format.DateTimeFormatter.*;

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
        return end == null ? null : between(now(clock), end);
    }

}
