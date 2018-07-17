package org.zalando.riptide.spring;

import org.junit.Test;

import java.util.concurrent.TimeUnit;

import static org.hamcrest.Matchers.hasToString;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;

public final class TimeSpanTest {

    @Test
    public void shouldSupportEmptyString() {
        final TimeSpan unit = TimeSpan.valueOf("");
        assertThat(unit.getAmount(), is(0L));
    }

    @Test
    public void shouldRenderString() {
        final TimeSpan unit = TimeSpan.of(1, TimeUnit.MINUTES);
        assertThat(unit, hasToString("1 minutes"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldFailToParse() {
        TimeSpan.valueOf("1 month");
    }

}
