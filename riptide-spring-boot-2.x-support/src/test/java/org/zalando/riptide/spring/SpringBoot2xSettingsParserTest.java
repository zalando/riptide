package org.zalando.riptide.spring;

import org.junit.Test;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.mock.env.MockEnvironment;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public final class SpringBoot2xSettingsParserTest {

    @Test
    public void shouldBeApplicable() {
        assertThat(new SpringBoot2xSettingsParser().isApplicable(), is(true));
    }

    @Test
    public void shouldParse() {
        final ConfigurableEnvironment environment = new MockEnvironment()
                .withProperty("riptide.clients.example.base-url", "https://example.com");
        final SettingsParser unit = new SpringBoot2xSettingsParser();

        final RiptideProperties settings = unit.parse(environment);

        assertThat(settings.getClients().values(), hasSize(1));
        assertThat(settings.getClients().get("example").getBaseUrl(), is("https://example.com"));
    }

}
