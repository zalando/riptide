package org.zalando.riptide.spring;

import org.junit.Test;
import org.springframework.mock.env.MockEnvironment;

import java.util.NoSuchElementException;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class RiptidePostProcessorTest {

    private final RiptidePostProcessor unit = new RiptidePostProcessor((a, b) -> null);

    @Test
    public void shouldParseUsingOnlyParser() {
        final SettingsParser parser = mock(SettingsParser.class);
        final RiptideProperties expected = new RiptideProperties();

        when(parser.isApplicable()).thenReturn(true);
        when(parser.parse(any())).thenReturn(expected);

        final RiptideProperties actual = unit.parse(new MockEnvironment(), singletonList(parser));

        assertThat(actual, is(sameInstance(expected)));
    }

    @Test
    public void shouldParseUsingSecondParser() {
        final SettingsParser first = mock(SettingsParser.class);
        final SettingsParser second = mock(SettingsParser.class);
        final RiptideProperties expected = new RiptideProperties();

        when(first.isApplicable()).thenReturn(false);
        when(second.isApplicable()).thenReturn(true);
        when(second.parse(any())).thenReturn(expected);

        final RiptideProperties actual = unit.parse(new MockEnvironment(), asList(first, second));

        assertThat(actual, is(sameInstance(expected)));
    }

    @Test(expected = NoSuchElementException.class)
    public void shouldFailOnMissingParser() {
        unit.parse(new MockEnvironment(), emptyList());
    }

}
