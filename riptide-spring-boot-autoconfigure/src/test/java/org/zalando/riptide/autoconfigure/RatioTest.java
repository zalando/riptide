package org.zalando.riptide.autoconfigure;

import org.junit.jupiter.api.*;

import java.util.*;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

final class RatioTest {

    @Test
    void shouldParseAmount() {
        final Ratio ratio = Ratio.valueOf("17");

        assertThat(ratio.getAmount(), is(17));
        assertThat(ratio.getTotal(), is(17));
    }

    @Test
    void shouldParseAmountUsingConstructor() {
        final Ratio ratio = new Ratio(17);

        assertThat(ratio.getAmount(), is(17));
        assertThat(ratio.getTotal(), is(17));
    }

    @Test
    void shouldParseAmountOutOfTotal() {
        final Ratio ratio = Ratio.valueOf("3 out of 5");

        assertThat(ratio.getAmount(), is(3));
        assertThat(ratio.getTotal(), is(5));
    }

    @Test
    void shouldParseAmountOfTotal() {
        final Ratio ratio = Ratio.valueOf("3  of  5");

        assertThat(ratio.getAmount(), is(3));
        assertThat(ratio.getTotal(), is(5));
    }

    @Test
    void shouldParseAmountDividedTotal() {
        final Ratio ratio = Ratio.valueOf("3 / 5");

        assertThat(ratio.getAmount(), is(3));
        assertThat(ratio.getTotal(), is(5));
    }

    @Test
    void shouldParseAmountDividedTotalWithoutSpaces() {
        final Ratio ratio = Ratio.valueOf("3/5");

        assertThat(ratio.getAmount(), is(3));
        assertThat(ratio.getTotal(), is(5));
    }

    @Test
    void shouldApplyTo() {
        final Map<Integer, Integer> consumer = new HashMap<>();
        Ratio.valueOf("1 of 2").applyTo(consumer::put);

        assertThat(consumer, hasEntry(1, 2));
    }

    @Test
    void shouldFailOnUnsupportedRatioFormat() {
        assertThrows(IllegalArgumentException.class, () -> Ratio.valueOf("a lot out of many"));
    }

}
