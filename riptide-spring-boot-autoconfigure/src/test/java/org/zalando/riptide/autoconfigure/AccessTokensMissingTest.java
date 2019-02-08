package org.zalando.riptide.autoconfigure;

import com.google.common.base.Throwables;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Configuration;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class AccessTokensMissingTest {

    @Configuration
    @EnableAutoConfiguration
    public static class TestConfiguration {

    }

    @Test
    void shouldFailDueToMissingAccessTokenUrl() {
        final Exception exception = assertThrows(Exception.class,
                new SpringApplicationBuilder(TestConfiguration.class)
                        .profiles("oauth")
                        .build()::run);

        final Throwable rootCause = Throwables.getRootCause(exception);

        assertThat(rootCause.getMessage(), containsString("riptide.oauth.access-token-url"));
        assertThat(rootCause.getMessage(), containsString("ACCESS_TOKEN_URL"));
    }

}
