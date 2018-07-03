package org.zalando.riptide.spring;

import org.junit.Test;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.validation.BindException;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public final class SpringBoot1xSettingsParserTest {

    @Test
    public void shouldBeApplicable() {
        assertThat(new SpringBoot1xSettingsParser().isApplicable(), is(true));
    }

    @Test
    public void shouldParse() {
        final ConfigurableEnvironment environment = new MockEnvironment()
                .withProperty("riptide.clients.example.base-url", "https://example.com");
        final SettingsParser unit = new SpringBoot1xSettingsParser();

        final RiptideProperties settings = unit.parse(environment);

        assertThat(settings.getClients().values(), hasSize(1));
        assertThat(settings.getClients().get("example").getBaseUrl(), is("https://example.com"));
    }

    @Test(expected = BindException.class)
    public void shouldFailToBind() {
        final ConfigurableEnvironment environment = new MockEnvironment()
                .withProperty("riptide.clients.example.connect-timeout", "foo");
        final SettingsParser unit = new SpringBoot1xSettingsParser();

        unit.parse(environment);
    }

}
