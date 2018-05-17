package org.zalando.riptide.spring;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

@AllArgsConstructor(staticName = "of")
@Getter
final class TimeSpan {

    private static final Pattern PATTERN = Pattern.compile("(\\d+) (\\w+)");

    private static final Map<String, TimeUnit> UNITS = Arrays.stream(TimeUnit.values())
            .collect(toMap(TimeSpan::toName, identity()));

    private final long amount;
    private final TimeUnit unit;

    // used by SnakeYAML
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

    void applyTo(final BiConsumer<Long, TimeUnit> consumer) {
        consumer.accept(amount, unit);
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
        checkArgument(matcher.matches(), "'%s' is not a valid time span", value);

        final long amount = Long.parseLong(matcher.group(1));
        final TimeUnit unit = fromName(matcher.group(2));

        return new TimeSpan(amount, unit);
    }

    private static TimeUnit fromName(final String name) {
        return parse(name.toLowerCase(Locale.ROOT));
    }

    private static TimeUnit parse(final String name) {
        final TimeUnit unit = UNITS.get(name.endsWith("s") ? name : name + "s");
        checkArgument(unit != null, "Unknown time unit: [%s]", name);
        return unit;
    }

    private static String toName(final TimeUnit unit) {
        return unit.name().toLowerCase(Locale.ROOT);
    }

}
