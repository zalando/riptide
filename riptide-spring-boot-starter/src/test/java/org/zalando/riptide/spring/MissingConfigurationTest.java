package org.zalando.riptide.spring;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.zalando.riptide.Http;

import static java.util.Collections.emptyMap;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest
@ActiveProfiles(profiles = "none")
@Component
public final class MissingConfigurationTest {

    @Configuration
    @ImportAutoConfiguration({
            RiptideAutoConfiguration.class,
    })
    public static class TestConfiguration {

    }

    @Autowired
    private ApplicationContext context;

    @Test
    public void shouldStartWithoutClients() {
        assertThat(context.getBeansOfType(Http.class), is(emptyMap()));
    }

}
