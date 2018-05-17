package org.zalando.riptide.spring;

import com.google.common.collect.Lists;
import org.junit.Test;

import java.util.List;
import java.util.ServiceLoader;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertThat;

public class SpringBoot2xSettingsParserSPITest {

    @Test
    public void shouldLoad() {
        final List<SettingsParser> parsers = Lists.newArrayList(ServiceLoader.load(SettingsParser.class));

        assertThat(parsers, contains(instanceOf(SpringBoot2xSettingsParser.class)));
    }

}
