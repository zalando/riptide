package org.zalando.riptide.autoconfigure;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class RatioInTimeSpanTest {

    @Test
    void shouldParseRatioInTimeSpan() {
        final RatioInTimeSpan ratioInTimeSpan = RatioInTimeSpan.valueOf("3 out of 5 in 5 seconds");

        assertThat(ratioInTimeSpan.getRatio().getAmount(), is(3));
        assertThat(ratioInTimeSpan.getRatio().getTotal(), is(5));
        assertThat(ratioInTimeSpan.getTimeSpan().toDuration(), is(Duration.ofSeconds(5)));
    }

    @Test
    void shouldParseUsingConstructor() {
        final RatioInTimeSpan ratioInTimeSpan = new RatioInTimeSpan("4 / 10 in 15 minutes");

        assertThat(ratioInTimeSpan.getRatio().getAmount(), is(4));
        assertThat(ratioInTimeSpan.getRatio().getTotal(), is(10));
        assertThat(ratioInTimeSpan.getTimeSpan().toDuration(), is(Duration.ofMinutes(15)));
    }

    @Test
    void shouldApplyTo() {
        final RatioInTimeSpan ratioInTimeSpan = RatioInTimeSpan.valueOf("4 in 15 minutes");
        final Map<String, Object> consumerParameters = new HashMap<>();

        ratioInTimeSpan.applyTo((amount, total, duration) -> {
                            consumerParameters.put("amount", amount);
                            consumerParameters.put("total", total);
                            consumerParameters.put("duration", duration);
                        });

        assertThat(consumerParameters, hasEntry("amount", 4));
        assertThat(consumerParameters, hasEntry("total", 4));
        assertThat(consumerParameters, hasEntry("duration", Duration.ofMinutes(15)));
    }

    @Test
    void shouldFailOnUnsupportedFormat() {
        assertThrows(IllegalArgumentException.class, () -> RatioInTimeSpan.valueOf("a lot out of many"));
    }

}
