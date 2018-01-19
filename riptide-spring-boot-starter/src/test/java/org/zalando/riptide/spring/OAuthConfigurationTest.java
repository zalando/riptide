package org.zalando.riptide.spring;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.NONE;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = DefaultTestConfiguration.class, webEnvironment = NONE)
@TestPropertySource(properties = {
        "riptide.oauth.scheduling-period: 15 seconds",
        "riptide.oauth.connect-timeout: 2 seconds",
        "riptide.oauth.socket-timeout: 3 seconds",
})
public final class OAuthConfigurationTest {

    @Test
    public void shouldUseSchedulingPeriod() {
        // TODO implement
    }

    @Test
    public void shouldUseTimeouts() {
        // TODO implement
    }

}
