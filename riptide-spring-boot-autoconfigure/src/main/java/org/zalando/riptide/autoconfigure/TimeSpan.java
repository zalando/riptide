package org.zalando.riptide.autoconfigure;

import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

@AllArgsConstructor(staticName = "of")
@Getter
final class TimeSpan {

    private static final Pattern PATTERN = Pattern.compile("(\\d+) (\\w+)");

    private static final Map<String, TimeUnit> UNIT_NAMES = Arrays.stream(TimeUnit.values())
            .collect(toMap(TimeSpan::toName, identity()));

    private static final BiMap<TimeUnit, ChronoUnit> UNIT_MAPPING = ImmutableBiMap.<TimeUnit, ChronoUnit>builder()
            .put(TimeUnit.NANOSECONDS, ChronoUnit.NANOS)
            .put(TimeUnit.MICROSECONDS, ChronoUnit.MICROS)
            .put(TimeUnit.MILLISECONDS, ChronoUnit.MILLIS)
            .put(TimeUnit.SECONDS, ChronoUnit.SECONDS)
            .put(TimeUnit.MINUTES, ChronoUnit.MINUTES)
            .put(TimeUnit.HOURS, ChronoUnit.HOURS)
            .put(TimeUnit.DAYS, ChronoUnit.DAYS)
            .build();

    private final long amount;
    private final TimeUnit unit;

    @SuppressWarnings("unused")
    public TimeSpan(final String value) {
        this(TimeSpan.valueOf(value));
    }

    private TimeSpan(final TimeSpan span) {
        this(span.amount, span.unit);
    }

    long to(final TimeUnit targetUnit) {
        return targetUnit.convert(amount, unit);
    }

    Duration toDuration() {
        return Duration.of(amount, UNIT_MAPPING.get(unit));
    }

    void applyTo(final BiConsumer<Long, TimeUnit> consumer) {
        consumer.accept(amount, unit);
    }

    void applyTo(final Consumer<Duration> consumer) {
        consumer.accept(toDuration());
    }

    @Override
    public String toString() {
        return amount + " " + toName(unit);
    }

    static TimeSpan valueOf(final String value) {
        if (value.isEmpty()) {
            return new TimeSpan(0, TimeUnit.NANOSECONDS);
        }

        final Matcher matcher = PATTERN.matcher(value);

        if (!matcher.matches()) {
            throw new IllegalArgumentException("'" + value + "' is not a valid time span");
        }

        final long amount = Long.parseLong(matcher.group(1));
        final TimeUnit unit = fromName(matcher.group(2));

        return new TimeSpan(amount, unit);
    }

    private static TimeUnit fromName(final String name) {
        return parse(name.toLowerCase(Locale.ROOT));
    }

    private static TimeUnit parse(final String name) {
        final TimeUnit unit = UNIT_NAMES.get(name.endsWith("s") ? name : name + "s");

        if (unit == null) {
            throw new IllegalArgumentException("Unknown time unit: [" + name + "]");
        }

        return unit;
    }

    private static String toName(final TimeUnit unit) {
        return unit.name().toLowerCase(Locale.ROOT);
    }

}
