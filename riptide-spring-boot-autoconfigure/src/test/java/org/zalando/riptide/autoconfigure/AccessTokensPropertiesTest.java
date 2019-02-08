package org.zalando.riptide.autoconfigure;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.NONE;

@SpringBootTest(classes = DefaultTestConfiguration.class, webEnvironment = NONE)
@TestPropertySource(properties = "riptide.oauth.access-token-url: http://example.com")
final class AccessTokensPropertiesTest {

    @Test
    void shouldRun() {
        // if the application context is booting up, I'm happy
    }

}
