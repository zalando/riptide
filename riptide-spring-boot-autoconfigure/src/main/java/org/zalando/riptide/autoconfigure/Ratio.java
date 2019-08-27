package org.zalando.riptide.autoconfigure;

import lombok.*;

import java.util.*;
import java.util.function.*;
import java.util.regex.*;

@AllArgsConstructor
@Getter
final class Ratio {

    private static final Pattern PATTERN = Pattern.compile("(\\d+)(?:(?:(?: +(?:out )?+of +)|(?: */ *))?(\\d+))?");

    private final int amount;
    private final int total;

    // used by SnakeYAML
    @SuppressWarnings("unused")
    public Ratio(final Integer value) {
        this(value.toString());
    }

    // used by SnakeYAML
    @SuppressWarnings("unused")
    public Ratio(final String value) {
        this(Ratio.valueOf(value));
    }

    private Ratio(final Ratio ratio) {
        this(ratio.amount, ratio.total);
    }

    void applyTo(final BiConsumer<Integer, Integer> consumer) {
        consumer.accept(amount, total);
    }

    static Ratio valueOf(final String value) {
        final Matcher matcher = PATTERN.matcher(value);

        if (!matcher.matches()) {
            throw new IllegalArgumentException("'" + value + "' is not a valid ratio");
        }

        final int amount = Integer.parseInt(matcher.group(1));
        final int total = Optional.ofNullable(matcher.group(2)).map(Integer::parseInt).orElse(amount);

        return new Ratio(amount, total);
    }

}
