package org.zalando.riptide.spring;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public final class RatioTest {

    @Test
    public void shouldParseAmount() {
        final Ratio ratio = Ratio.valueOf("17");

        assertThat(ratio.getAmount(), is(17));
        assertThat(ratio.getTotal(), is(17));
    }

    @Test
    public void shouldParseAmountUsingConstructor() {
        final Ratio ratio = new Ratio(17);

        assertThat(ratio.getAmount(), is(17));
        assertThat(ratio.getTotal(), is(17));
    }

    @Test
    public void shouldParseAmountOutOfTotal() {
        final Ratio ratio = Ratio.valueOf("3 out of 5");

        assertThat(ratio.getAmount(), is(3));
        assertThat(ratio.getTotal(), is(5));
    }

    @Test
    public void shouldParseAmountOfTotal() {
        final Ratio ratio = Ratio.valueOf("3  of  5");

        assertThat(ratio.getAmount(), is(3));
        assertThat(ratio.getTotal(), is(5));
    }

    @Test
    public void shouldParseAmountDividedTotal() {
        final Ratio ratio = Ratio.valueOf("3 / 5");

        assertThat(ratio.getAmount(), is(3));
        assertThat(ratio.getTotal(), is(5));
    }

    @Test
    public void shouldParseAmountDividedTotalWithoutSpaces() {
        final Ratio ratio = Ratio.valueOf("3/5");

        assertThat(ratio.getAmount(), is(3));
        assertThat(ratio.getTotal(), is(5));
    }

    @Test
    public void shouldApplyTo() {
        final Map<Integer, Integer> consumer = new HashMap<>();
        Ratio.valueOf("1 of 2").applyTo(consumer::put);

        assertThat(consumer, hasEntry(1, 2));
    }

}
