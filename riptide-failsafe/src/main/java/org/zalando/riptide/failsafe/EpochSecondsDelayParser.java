package org.zalando.riptide.failsafe;

import com.google.common.base.CharMatcher;
import lombok.AllArgsConstructor;

import javax.annotation.Nullable;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

import static java.lang.Long.parseLong;
import static java.time.Duration.between;
import static java.time.Instant.now;

@AllArgsConstructor
final class EpochSecondsDelayParser implements DelayParser {

    private final CharMatcher digit = CharMatcher.inRange('0', '9').precomputed();

    private final Clock clock;
    private final Duration threshold;

    EpochSecondsDelayParser(final Clock clock) {
        // maximum difference between timezones is 26 hours
        this(clock, Duration.ofHours(-26));
    }

    @Override
    public Duration parse(final String value) {
        final Instant instant = parseDate(value);

        if (instant == null) {
            return null;
        }

        final Duration duration = between(now(clock), instant);

        if (duration.compareTo(threshold) > 0) {
            return duration;
        }

        return null;
    }

    @Nullable
    private Instant parseDate(final String value) {
        if (isInteger(value)) {
            final long epochSecond = parseLong(value);
            return Instant.ofEpochSecond(epochSecond);
        }

        return null;
    }

    private boolean isInteger(final String value) {
        return digit.matchesAllOf(value) && !value.isEmpty();
    }

}
