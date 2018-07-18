package org.zalando.riptide.autoconfigure;

import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public final class RatioTest {

    @Test
    public void getAmount() {
        final Ratio unit = new Ratio(1, 10);
        assertThat(unit.getAmount(), is(1));
    }

    @Test
    public void getTotal() {
        final Ratio unit = new Ratio(1, 10);
        assertThat(unit.getTotal(), is(10));
    }

}
