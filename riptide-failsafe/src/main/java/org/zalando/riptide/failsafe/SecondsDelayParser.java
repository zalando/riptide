package org.zalando.riptide.failsafe;

import com.google.common.base.CharMatcher;

import java.time.Duration;

import static java.lang.Long.parseLong;

final class SecondsDelayParser implements DelayParser {

    private final CharMatcher digit = CharMatcher.inRange('0', '9').precomputed();

    @Override
    public Duration parse(final String value) {
        if (isInteger(value)) {
            return Duration.ofSeconds(parseLong(value));
        }

        return null;
    }

    private boolean isInteger(final String value) {
        return digit.matchesAllOf(value) && !value.isEmpty();
    }

}
