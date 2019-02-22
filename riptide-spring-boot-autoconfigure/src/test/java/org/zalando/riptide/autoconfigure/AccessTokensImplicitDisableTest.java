package org.zalando.riptide.autoconfigure;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.stereotype.Component;
import org.springframework.test.context.ActiveProfiles;
import org.zalando.riptide.auth.AuthorizationPlugin;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

@SpringBootTest
@ActiveProfiles(profiles = "no-oauth", inheritProfiles = false)
@Component
final class AccessTokensImplicitDisableTest {

    @Configuration
    @ImportAutoConfiguration({
            RiptideAutoConfiguration.class,
            JacksonAutoConfiguration.class,
    })
    @EnableScheduling
    public static class TestConfiguration {

    }

    @Autowired(required = false)
    private AuthorizationPlugin exampleAuthorizationPlugin;

    @Test
    void shouldImplicitlyDisable() {
        assertThat(exampleAuthorizationPlugin, is(nullValue()));
    }

}
