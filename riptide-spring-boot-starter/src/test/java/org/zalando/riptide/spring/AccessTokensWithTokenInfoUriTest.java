package org.zalando.riptide.spring;

import com.google.gag.annotation.remark.Hack;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.zalando.stups.tokens.AccessTokens;
import org.zalando.stups.tokens.ClientCredentials;
import org.zalando.stups.tokens.ClientCredentialsProvider;
import org.zalando.stups.tokens.TokenRefresherConfiguration;
import org.zalando.stups.tokens.UserCredentials;
import org.zalando.stups.tokens.UserCredentialsProvider;

import java.lang.reflect.Field;
import java.net.URI;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest
@ActiveProfiles(profiles = "token-info-url", inheritProfiles = false)
@Component
public final class AccessTokensWithTokenInfoUriTest {

    @Configuration
    @ImportAutoConfiguration({
            RiptideAutoConfiguration.class,
            JacksonAutoConfiguration.class,
    })
    public static class TestConfiguration {

    }

    @Autowired
    private AccessTokens accessTokens;

    @Test
    public void shouldSetTokenIfoUrl() throws NoSuchFieldException, IllegalAccessException {
        final TokenRefresherConfiguration configuration = getConfiguration();

        assertThat(configuration.getTokenInfoUri(), is(URI.create("http://another-example.com")));
    }

    @Hack
    private TokenRefresherConfiguration getConfiguration() throws NoSuchFieldException, IllegalAccessException {
        final Class<? extends AccessTokens> type = accessTokens.getClass();
        final Field field = type.getSuperclass().getDeclaredField("configuration");
        field.setAccessible(true);
        return (TokenRefresherConfiguration) field.get(accessTokens);
    }

}
