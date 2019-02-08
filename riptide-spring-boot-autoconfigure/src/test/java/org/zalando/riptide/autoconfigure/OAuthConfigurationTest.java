package org.zalando.riptide.autoconfigure;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.NONE;

@SpringBootTest(classes = DefaultTestConfiguration.class, webEnvironment = NONE)
@TestPropertySource(properties = {
        "riptide.oauth.scheduling-period: 15 seconds",
        "riptide.oauth.connect-timeout: 2 seconds",
        "riptide.oauth.socket-timeout: 3 seconds",
})
final class OAuthConfigurationTest {

    @Test
    void shouldUseSchedulingPeriod() {
        // TODO implement
    }

    @Test
    void shouldUseTimeouts() {
        // TODO implement
    }

}
