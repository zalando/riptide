package org.zalando.riptide.autoconfigure;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import org.springframework.test.context.ActiveProfiles;
import org.zalando.riptide.Http;

import static java.util.Collections.emptyMap;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

@SpringBootTest
@ActiveProfiles(profiles = "none")
@Component
final class MissingConfigurationTest {

    @Configuration
    @ImportAutoConfiguration({
            RiptideAutoConfiguration.class,
    })
    public static class TestConfiguration {

    }

    @Autowired
    private ApplicationContext context;

    @Test
    void shouldStartWithoutClients() {
        assertThat(context.getBeansOfType(Http.class), is(emptyMap()));
    }

}
