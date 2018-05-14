package org.zalando.riptide.spring;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.stereotype.Component;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.zalando.stups.tokens.AccessTokens;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest
@ActiveProfiles(profiles = "no-oauth", inheritProfiles = false)
@Component
public final class AccessTokensImplicitDisableTest {

    @Configuration
    @ImportAutoConfiguration({
            RiptideAutoConfiguration.class,
            JacksonAutoConfiguration.class,
    })
    @EnableScheduling
    public static class TestConfiguration {

    }

    @Autowired(required = false)
    private AccessTokens accessTokens;

    @Test
    public void shouldImplicitlyDisable() {
        assertThat(accessTokens, is(nullValue()));
    }

}
