package org.zalando.riptide.autoconfigure;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.zalando.riptide.autoconfigure.junit.EnvironmentVariables;

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.NONE;

@SpringBootTest(classes = DefaultTestConfiguration.class, webEnvironment = NONE)
final class AccessTokensEnvironmentVariablesTest {

    private static final EnvironmentVariables ENVIRONMENT = new EnvironmentVariables();

    @BeforeAll
    static void setAccessTokenUrl() {
        ENVIRONMENT.set("ACCESS_TOKEN_URL", "http://example.com");
    }

    @AfterAll
    static void removeAccessTokenUrl() {
        ENVIRONMENT.set("ACCESS_TOKEN_URL", null);
    }

    @Test
    void shouldRun() {
        // if the application context is booting up, I'm happy
    }

}
