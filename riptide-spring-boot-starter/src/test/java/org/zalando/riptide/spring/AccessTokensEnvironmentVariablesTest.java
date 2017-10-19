package org.zalando.riptide.spring;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.junit.runner.RunWith;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.zalando.logbook.spring.LogbookAutoConfiguration;
import org.zalando.tracer.spring.TracerAutoConfiguration;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest
public final class AccessTokensEnvironmentVariablesTest {

    @ClassRule
    public static final EnvironmentVariables ENVIRONMENT = new EnvironmentVariables();

    @Configuration
    @ImportAutoConfiguration({
            RiptideAutoConfiguration.class,
            JacksonAutoConfiguration.class,
    })
    public static class TestConfiguration {

    }

    @BeforeClass
    public static void setAccessTokenUrl() {
        ENVIRONMENT.set("ACCESS_TOKEN_URL", "http://example.com");
    }

    @AfterClass
    public static void removeAccessTokenUrl() {
        ENVIRONMENT.set("ACCESS_TOKEN_URL", null);
    }

    @Test
    public void shouldRun() {
        // if the application context is booting up, I'm happy
    }

}
