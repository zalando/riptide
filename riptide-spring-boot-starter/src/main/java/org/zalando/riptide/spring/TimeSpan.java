package org.zalando.riptide.spring;

import com.google.common.base.Splitter;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkArgument;
import static java.lang.Integer.signum;
import static java.lang.String.CASE_INSENSITIVE_ORDER;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

@AllArgsConstructor(staticName = "of")
@Getter
final class TimeSpan {

    static final TimeSpan ZERO = new TimeSpan(0, TimeUnit.NANOSECONDS);

    private static final Map<String, TimeUnit> UNITS = Arrays.stream(TimeUnit.values())
            .collect(toMap(TimeSpan::toName, identity(), (u, v) -> u, () -> new TreeMap<>(CASE_INSENSITIVE_ORDER)));

    private static final Splitter PARTS = Splitter.on(',').trimResults().omitEmptyStrings();
    private static final Splitter DURATION = Splitter.on(' ').trimResults().omitEmptyStrings().limit(2);

    private final long amount;
    private final TimeUnit unit;

    // used by SnakeYAML
    @SuppressWarnings("unused")
    public TimeSpan(final String value) {
        this(TimeSpan.valueOf(value));
    }

    private TimeSpan(final TimeSpan span) {
        this(span.getAmount(), span.getUnit());
    }

    TimeSpan plus(final TimeSpan span) {
        switch (signum(unit.compareTo(span.getUnit()))) {
            case -1:
                return span.addTo(this);
            case 1:
                return this.addTo(span);
            default:
                // same time unit
                return new TimeSpan(amount + span.getAmount(), unit);
        }
    }

    private TimeSpan addTo(final TimeSpan span) {
        return new TimeSpan(span.getAmount() + span.getUnit().convert(amount, unit), span.getUnit());
    }

    long to(final TimeUnit targetUnit) {
        return targetUnit.convert(amount, unit);
    }

    @Override
    public String toString() {
        return amount + " " + toName(unit);
    }

    static TimeSpan valueOf(final String value) {
        return PARTS.splitToList(value).stream()
                .map(TimeSpan::parse)
                .reduce(TimeSpan::plus).orElse(ZERO);
    }

    private static TimeSpan parse(final String part) {
        final List<String> parts = DURATION.splitToList(part);
        final long amount = Long.parseLong(parts.get(0));
        final TimeUnit unit = fromName(parts.get(1));

        return new TimeSpan(amount, unit);
    }

    private static TimeUnit fromName(final String name) {
        final TimeUnit unit = UNITS.get(name.endsWith("s") ? name : name + "s");
        checkArgument(unit != null, "Unknown time unit: [%s]", name);
        return unit;
    }

    private static String toName(final TimeUnit unit) {
        return unit.name().toLowerCase(Locale.ROOT);
    }

}
