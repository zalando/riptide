package org.zalando.riptide.spring;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = DefaultTestConfiguration.class)
public final class AccessTokensEnvironmentVariablesTest {

    @ClassRule
    public static final EnvironmentVariables ENVIRONMENT = new EnvironmentVariables();

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
