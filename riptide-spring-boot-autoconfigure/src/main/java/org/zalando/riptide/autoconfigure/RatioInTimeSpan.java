package org.zalando.riptide.autoconfigure;

import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.Getter;

@Getter
final class RatioInTimeSpan {

    private static final Pattern PATTERN = Pattern.compile("(?<ratio>.*)? in (?<timespan>.*)?");

    private final Ratio ratio;
    private final TimeSpan timeSpan;

    // used by SnakeYAML
    @SuppressWarnings("unused")
    public RatioInTimeSpan(String value) {
        this(RatioInTimeSpan.valueOf(value));
    }

    private RatioInTimeSpan(Ratio ratio, TimeSpan timeSpan) {
        this.ratio = ratio;
        this.timeSpan = timeSpan;
    }

    private RatioInTimeSpan(RatioInTimeSpan ratioInTimeSpan) {
        this(ratioInTimeSpan.ratio, ratioInTimeSpan.timeSpan);
    }

    void applyTo(RatioInTimeSpanConsumer consumer) {
        consumer.accept(ratio.getAmount(), ratio.getTotal(), timeSpan.toDuration());
    }

    public static RatioInTimeSpan valueOf(String value) {
        final Matcher matcher = PATTERN.matcher(value);

        if (!matcher.matches()) {
            throw new IllegalArgumentException("'" + value + "' is not a valid ratio in timespan");
        }

        final Ratio ratio = Ratio.valueOf(matcher.group("ratio"));
        final TimeSpan timeSpan = TimeSpan.valueOf(matcher.group("timespan"));
        return new RatioInTimeSpan(ratio, timeSpan);
    }

    interface RatioInTimeSpanConsumer {
        void accept(int amount, int total, Duration duration);
    }
}
