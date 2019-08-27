package org.zalando.riptide.autoconfigure;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.boot.autoconfigure.*;
import org.springframework.boot.test.context.*;
import org.springframework.context.*;
import org.springframework.context.annotation.*;
import org.springframework.stereotype.*;
import org.springframework.test.context.*;
import org.zalando.riptide.*;

import static java.util.Collections.*;
import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;

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
