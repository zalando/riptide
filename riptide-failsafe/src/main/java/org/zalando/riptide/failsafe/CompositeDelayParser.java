package org.zalando.riptide.failsafe;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.Collection;
import java.util.Objects;

@Slf4j
@AllArgsConstructor
final class CompositeDelayParser implements DelayParser {

    private final Collection<DelayParser> parsers;

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
