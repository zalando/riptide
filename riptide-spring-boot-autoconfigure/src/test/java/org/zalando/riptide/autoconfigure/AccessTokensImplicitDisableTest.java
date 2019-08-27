package org.zalando.riptide.autoconfigure;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.boot.autoconfigure.*;
import org.springframework.boot.autoconfigure.jackson.*;
import org.springframework.boot.test.context.*;
import org.springframework.context.annotation.*;
import org.springframework.scheduling.annotation.*;
import org.springframework.stereotype.*;
import org.springframework.test.context.*;
import org.zalando.riptide.auth.*;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;

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
