package org.zalando.riptide.failsafe;

import com.google.common.base.CharMatcher;
import net.jodah.failsafe.util.Duration;

import static java.lang.Long.parseLong;
import static java.util.concurrent.TimeUnit.SECONDS;

final class SecondsDelayParser implements DelayParser {

    private final CharMatcher digit = CharMatcher.inRange('0', '9').precomputed();

    @Override
    public Duration parse(final String value) {
        if (isInteger(value)) {
            return new Duration(parseLong(value), SECONDS);
        }

        return null;
    }

    private boolean isInteger(final String value) {
        return digit.matchesAllOf(value) && !value.isEmpty();
    }

}
