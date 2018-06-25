package org.zalando.riptide.failsafe;

import lombok.extern.slf4j.Slf4j;
import net.jodah.failsafe.util.Duration;

import java.util.Collection;
import java.util.Objects;

@Slf4j
final class CompoundDelayParser implements DelayParser {

    private final Collection<DelayParser> parsers;

    CompoundDelayParser(final Collection<DelayParser> parsers) {
        this.parsers = parsers;
    }

    @Override
    public Duration parse(final String value) {
        final Duration delay = parsers.stream()
                .map(parser -> parser.parse(value))
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);

        if (delay == null) {
            log.warn("Received unsupported 'Retry-After' header [{}]; will ignore it", value);
        }

        return delay;
    }

}
